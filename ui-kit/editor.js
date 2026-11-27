import {basicSetup} from 'codemirror';
import {EditorView,keymap,placeholder} from '@codemirror/view';
import {Compartment,EditorState,StateEffect} from '@codemirror/state';
import {sql,SQLite,PostgreSQL,MySQL,MariaSQL} from '@codemirror/lang-sql';
import {HighlightStyle,syntaxHighlighting} from '@codemirror/language';
import {tags} from '@lezer/highlight';
const syntax=HighlightStyle.define([
 {tag:tags.keyword,color:'var(--syn-keyword)',fontWeight:'600'},
 {tag:[tags.string,tags.special(tags.string)],color:'var(--syn-string)'},
 {tag:[tags.number,tags.bool,tags.null],color:'var(--syn-number)'},
 {tag:[tags.comment],color:'var(--cds-text-secondary)',fontStyle:'italic'},
 {tag:[tags.function(tags.variableName),tags.standard(tags.name)],color:'var(--syn-func)'}]);
// Retain CodeMirror's document, undo/redo history and selections, not a tab's callbacks.
const shared=[basicSetup,syntaxHighlighting(syntax),EditorState.tabSize.of(2),EditorView.lineWrapping,
  EditorView.contentAttributes.of({'aria-label':'SQL','spellcheck':'false','autocapitalize':'off','autocorrect':'off'}),placeholder('SELECT …')];
window.FieldEditor=function(host,{value,engine,schema,onChange,run,snapshot}) {
  const dialect=({sqlite:SQLite,postgres:PostgreSQL,mysql:MySQL,mariadb:MariaSQL})[engine]||SQLite;
  const lang=new Compartment(), extensions=[keymap.of([{key:'Mod-Enter',run(){run();return true;}}]),shared,lang.of(sql({dialect,schema})),
    EditorView.updateListener.of(u=>{if(u.docChanged)onChange(u.state.doc.toString());})];
  const restore=snapshot?.state instanceof EditorState && snapshot.state.doc.toString()===value;
  const state=restore?snapshot.state.update({effects:StateEffect.reconfigure.of(extensions)}).state:EditorState.create({doc:value,extensions});
  const view=new EditorView({parent:host,state,scrollTo:restore?snapshot.scroll:undefined});
  return {snapshot(){return {state:view.state.update({effects:StateEffect.reconfigure.of(shared)}).state,scroll:view.scrollSnapshot()};},
    destroy(){view.destroy();},updateSchema(value){view.dispatch({effects:lang.reconfigure(sql({dialect,schema:value}))});},
    insert(text){view.dispatch(view.state.replaceSelection(text));view.focus();},focus(){view.focus();}};
};
