'use strict';
const $=id=>document.getElementById(id), page=$('routeContent'), explorerPane=$('explorerPane'), overlay=$('overlay'), sheet=$('sheet');
let tabResults=new Map(), editorViews=new Map(), cmOwner=null, schemaJob=null, tableRefs=new Map();
let explorerKey='', scrollAt={};
function clearViews(){
 window.currentExplorer?.destroy();window.currentExplorer=null;explorerPane.replaceChildren();explorerKey='';
 cm?.destroy();cm=null;cmOwner=null;editorViews.clear();tabResults.clear();schemaJob=null;scrollAt={};tableRefs.clear();
}
function hideToast(){clearTimeout(toastTimer);$('toast').hidden=true;}
let cm=null, schemaCatalog=Object.create(null), selectedSource='demo', sourceFilter='', schemaFilter='', openSchema=new Set();
function icon(name){let m=window.carbonGlyphs[name];let svg=new DOMParser().parseFromString(m,'image/svg+xml').documentElement;let i=document.importNode(svg,true);i.setAttribute('class','ico');i.setAttribute('aria-hidden','true');return i;}
function tool(symbol,label,fn,cls=''){let b=button('',fn,cls);b.append(icon(symbol),el('span','',label));return b;}
function iconOnly(symbol,label,fn,cls=''){let b=button('',fn,'icon-only '+cls);b.append(icon(symbol));b.setAttribute('aria-label',label);b.title=label;return b;}
let cur='conn',w={},profiles=[],locals=[],conn=null,result=null,lastError='',lastMs=0,working=false,lock=false,shelf='saved',dialogCancel=null,saveTimer,toastTimer,schemaText='',seq=0,requests=new Map();
const sample="SELECT region,\n       SUM(revenue) AS total_revenue\nFROM orders\nGROUP BY region\nORDER BY total_revenue DESC;";
const t=k=>(lex[k]||[k])[Math.max(0,languages.indexOf(w.lang))]||k;
function rpc(op,data={}){return new Promise((resolve,reject)=>{const id=++seq;requests.set(id,[resolve,reject]);try{Native.send(id,op,JSON.stringify(data));}catch(e){requests.delete(id);reject(e);}});}
window.receive=(id,ok,data)=>{const r=requests.get(id);if(r){requests.delete(id);ok?r[0](data):r[1](Error(String(data)));}};
function el(tag,cls,txt){let e=document.createElement(tag);if(cls)e.className=cls;if(txt!==undefined)e.textContent=txt;return e;}
function node(tag,cls,...items){let e=el(tag,cls);e.append(...items);return e;}
function button(txt,fn,cls=''){const e=el('button',cls,txt);e.type='button';e.onclick=()=>Promise.resolve().then(fn).catch(showError);return e;}
function notify(txt){let x=$('toast');x.textContent=String(txt);x.hidden=false;clearTimeout(toastTimer);toastTimer=setTimeout(()=>x.hidden=true,2600);}
function showError(e){let s=String(e?.message||e);if(s==='已取消')return;void dialog(t('sql'),el('p','err',s),[[t('close'),true,'primary']]);}
function dialog(title,body,actions){if(dialogCancel)dialogCancel(null);sheet.replaceChildren(el('h2','',title),body);overlay.hidden=false;
 return new Promise(resolve=>{function end(v){overlay.hidden=true;dialogCancel=null;resolve(v);}dialogCancel=end;const bar=el('div','buttons');for(const [label,value,cls] of actions)bar.append(button(label,()=>end(value),cls));sheet.append(bar);});}
const yn=(title,body)=>dialog(title,body,[[t('cancel'),false],[t('ok'),true,'primary']]);
function field(label,input){return node('label','field',document.createTextNode(label),input);}
function input(value='',type='text',placeholder=''){const i=el('input');i.type=type;i.value=value;i.placeholder=placeholder;return i;}
function select(arr,value){let s=el('select');for(const [v,label] of arr){let o=el('option','',label);o.value=v;s.append(o);}s.value=value;return s;}
function check(label,value,fn){let i=input('','checkbox');i.checked=value;if(fn)i.onchange=()=>Promise.resolve(fn(i.checked)).catch(e=>{i.checked=!i.checked;showError(e);});return [node('label','switch',i,document.createTextNode(label)),i];}
async function askName(label,value=''){let i=input(value);if(await yn(label,field(t('name'),i)))return i.value.trim();return '';}
function persist(){clearTimeout(saveTimer);return rpc('workspace',w).catch(showError);}
function queueSave(){clearTimeout(saveTimer);saveTimer=setTimeout(persist,350);}
document.addEventListener('visibilitychange',()=>{if(document.hidden&&w.tabs)persist();});
let appliedChrome;function theme(){let dark=w.theme==='dark'||w.theme==='auto'&&matchMedia('(prefers-color-scheme: dark)').matches;document.documentElement.dataset.theme=dark?'dark':'light';document.documentElement.lang=w.lang;if(appliedChrome!==dark){appliedChrome=dark;rpc('systemTheme',{dark}).catch(()=>{});}}
matchMedia('(prefers-color-scheme: dark)').addListener(theme);
function header(k,small,action){let h=node('div','heading',el('h1','',t(k)));if(action)h.append(action);return h;}
function section(title,right){return node('div','sectionline',el('h2','',title),right||el('span'));}
function render(){
 if(cm){if(w.tabs.some(item=>item.n===cmOwner)){const grid=page.querySelector('.gridscroll');editorViews.set(cmOwner,{snapshot:cm.snapshot(),left:grid?.scrollLeft,top:grid?.scrollTop,result:grid?.boundResult,tabX:page.querySelector('.tabs')?.scrollLeft||0});}cm.destroy();cm=null;cmOwner=null;}
 theme();let st=$('status');st.replaceChildren(el('i','led'+(conn?' on':'')),document.createTextNode(conn?conn.name:t('offline')));st.onclick=()=>conn?connectionMenu().catch(showError):go('conn');
 if(!$('brandIcon').firstChild)$('brandIcon').append(icon('data--structured'));st.append(icon('chevron--down'));
 let nav=$('nav');nav.replaceChildren(el('span','nav-caption',t('workspaceNav')));[['conn','data--base'],['sql','terminal'],['objects','data-table'],['library','notebook']].forEach(([k,ico])=>{let b=tool(ico,t(k),()=>go(k));if(cur===k)b.setAttribute('aria-current','page');nav.append(b);});nav.append(tool('settings',t('settings'),preferences,'nav-settings'));
 $('page').dataset.route=cur;
 const bound=conn?JSON.stringify([conn.session,conn.database,w.lang]):'';
 if(window.currentExplorer&&explorerKey!==bound){window.currentExplorer.destroy();window.currentExplorer=null;explorerPane.replaceChildren();}
 const browsing=cur==='objects'&&!!conn;
 page.replaceChildren();page.hidden=browsing;explorerPane.hidden=!browsing;
 if(browsing){if(!window.currentExplorer){explorerKey=bound;objects();}else window.currentExplorer.resume().catch(showError);}
 else if(cur==='objects')home(true);else if(cur==='conn')home();else if(cur==='sql')worksheet();else library();
}
function go(k){
 if(cur!==k)scrollAt[cur]=window.scrollY;
 const y=cur===k?window.scrollY:scrollAt[k]||0;cur=k;render();
 window.scrollTo(0,y);
}
window.goShelf=()=>{shelf='saved';go('library');};
window.back=()=>{if(dialogCancel){dialogCancel(null);return true;}if(cur==='objects'&&window.currentExplorer?.back())return true;if(cur!=='conn'){go('conn');return true;}return false;};
function row(stamp,title,detail,fn){let b=button('',fn,'record');b.append(el('span','stamp',stamp),node('span','grow',el('strong','',title),el('small','',detail)),el('span','arrow','›'));return b;}
function openSource(p,destination){
 if(conn?.id===p.id&&!conn.profileDirty){go(destination);return;}
 return connect(p,!!p.sample,undefined,destination);
}
function home(chooseForBrowse=false){
 const demo={id:'demo',engine:'sqlite',name:'Field Notes',sample:true},sources=[...profiles,...locals.filter(x=>x!=='Field Notes').map(name=>({name,id:'local:'+name,engine:'sqlite'})),demo];
 let chosen=sources.find(p=>p.id===selectedSource)||sources[0];const engineName=p=>({sqlite:'SQLite',postgres:'PostgreSQL',mysql:'MySQL',mariadb:'MariaDB'})[p.engine];
 page.append(header(chooseForBrowse?'objects':'sources','',tool('add',t('new'),()=>profileForm(),'primary')));
 if(chooseForBrowse)page.append(el('p','choose-source',t('browseSource')));
 const layout=el('div','sources-layout'),master=el('section','source-master'),ins=el('aside','source-inspector');page.append(layout);layout.append(master,ins);
 let search=input(sourceFilter,'search',t('filterSources'));search.setAttribute('aria-label',t('filterSources'));let tools=node('div','source-tools',node('label','searchbox',icon('search'),search),iconOnly('upload',t('import'),async()=>{let name=await askName(t('import'));if(name){await rpc('import',{name});locals=await rpc('localList');render();}}),iconOnly('add',t('local')+' · '+t('create'),async()=>{let name=await askName(t('create'));if(name)await connect({id:'local:'+name,engine:'sqlite',name});}));master.append(tools);
 const lists=el('div','source-lists');master.append(lists);function inspect(p){chosen=p;selectedSource=p.id;ins.replaceChildren();ins.append(node('div','ins-caption',icon('application'),el('span','',t('details'))),node('div','inspect-title',el('span','engine-mark',p.engine==='postgres'?'Pg':p.engine==='mysql'?'My':p.engine==='mariadb'?'Ma':'Sq'),el('h2','',p.name||p.host)),el('p','ins-sub',p.sample?t('sampleDesc'):p.host||t('local')));
 let dl=el('dl','kv');for(let [k,x] of [['engine',engineName(p)],['host',p.host||t('device')],['database',p.database||p.name],...(p.host?[['port',p.port],['user',p.user]]:[])])dl.append(el('dt','',t(k)),el('dd','',x));ins.append(dl,node('div','source-actions',tool('data-table',t('objects'),()=>openSource(p,'objects'),'primary'),tool('terminal',t('workspace'),()=>openSource(p,'sql'))));
 if(p.host)ins.append(tool('settings',t('edit'),()=>profileForm(p),'ghost'));if(p.sample){let diagram=node('div','diagram',el('div','diagram-label','SQLITE  /  SAMPLE'),node('div','schema-chip',icon('table'),el('code','','orders')),el('i','diagram-line'),node('div','schema-chip',icon('layers'),el('code','','regional_sales')));ins.append(diagram);}
 }
 function draw(){lists.replaceChildren();for(let group of ['remote','local']){let filtered=sources.filter(p=>(group==='local')===(p.engine==='sqlite')&&[p.name,p.host,engineName(p)].join(' ').toLowerCase().includes(sourceFilter.toLowerCase()));lists.append(node('div','group-band',el('strong','',t(group)),el('span','count',filtered.length)));
 if(!filtered.length){lists.append(el('p','empty',sourceFilter?t('noMatch'):t('noProfiles')));continue;}
 for(let p of filtered){let bb=button('',()=>{if(matchMedia('(min-width: 760px)').matches){inspect(p);draw();}else return openSource(p,chooseForBrowse?'objects':'sql');},'source-record'+(p.id===chosen.id?' selected':''));bb.append(icon(p.engine==='sqlite'?'data--base':'connection'),node('span','source-name',el('strong','',p.name||p.host),el('small','',p.sample?t('sampleTag')+' · '+t('sampleDesc'):p.host?p.user+' @ '+p.host+':'+p.port:t('device'))),el('span','engine-tag',engineName(p)),icon('chevron--right'));let rr=node('div','source-row',bb);let mobile=node('div','mobile-source-actions',tool('data-table',t('objects'),()=>openSource(p,'objects'),'browse-action'),tool('terminal',t('workspace'),()=>openSource(p,'sql'),'quiet'));if(p.host)mobile.append(iconOnly('settings',t('edit'),()=>profileForm(p)));rr.append(mobile);lists.append(rr);}}}
 search.oninput=()=>{sourceFilter=search.value;draw();};draw();inspect(chosen);
}

async function profileForm(p={},focusCA=false,reconnect=false){
 let form=el('div'),eng=select([['postgres','PostgreSQL'],['mysql','MySQL'],['mariadb','MariaDB']],p.engine||'postgres'),name=input(p.name||''),host=input(p.host||'','text','db.example.org'),port=input(p.port||5432,'number'),user=input(p.user||''),db=input(p.database||''),pass=input('','password',p.has_password?t('keepSecret'):''),[tl,tls]=check(t('tls'),p.tls!==false),[sl,ssh]=check(t('ssh'),!!p.ssh),sh=input(p.sshHost||''),sp=input(p.sshPort||22,'number'),su=input(p.sshUser||''),fp=input(p.fingerprint||'','text','SHA256:…'),timeout=input(p.timeout||60,'number'),phrase=input('','password',p.has_passphrase?t('keepSecret'):''),keyData='';
 eng.onchange=()=>port.value=eng.value==='postgres'?5432:3306;
 let extra=el('div');let key=button(p.has_privateKey?t('keyOK'):t('key'),async()=>{keyData=await rpc('key');key.textContent=t('keyOK');});extra.append(node('div','cols',field('SSH · '+t('host'),sh),field(t('port'),sp)),field('SSH · '+t('user'),su),field(t('fp'),fp),key,field(t('phrase'),phrase));extra.hidden=!ssh.checked;ssh.onchange=()=>extra.hidden=!ssh.checked;
 let caAction='keep',caToken='',caName=p.has_ca?p.caName:'',caBox=el('div','ca-box'),caErr=el('p','err'),caButton;
 caErr.setAttribute('role','alert');
 function drawCA(){caButton=button(t(caName?'replaceCA':'importCA'),async()=>{
   try{const f=await rpc('ca');caAction='replace';caToken=f.token;caName=f.name;caErr.textContent='';drawCA();}
   catch(e){if(e.message!=='已取消')caErr.textContent=e.message;}
 });
 const line=node('div','ca-line',node('div','grow',el('strong','',t('ca')),el('span','ca-name',caName||t('systemCA'))),caButton);
 if(caName)line.append(button(t('removeCA'),()=>{caAction='remove';caToken='';caName='';caErr.textContent='';drawCA();},'quiet'));
 caBox.replaceChildren(line,el('small','',t('caHelp')),caErr);caBox.hidden=!tls.checked;}
 drawCA();tls.onchange=()=>caBox.hidden=!tls.checked;
 form.append(field(t('name'),name),field(t('engine'),eng),node('div','cols',field(t('host'),host),field(t('port'),port)),field(t('database'),db),field(t('user'),user),field(t('pass'),pass),field(t('timeout'),timeout),tl,caBox,sl,extra);
 let acts=[[t('cancel'),null],[t(reconnect?'saveRetry':'save'),'save','primary']];if(p.id)acts.unshift([t('delete'),'drop','danger']);
 for(;;){let choice=dialog(t(p.id?'editConnection':'new'),form,acts);
 if(focusCA){caBox.scrollIntoView({block:'center'});caButton.focus({preventScroll:true});focusCA=false;}
 let act=await choice;if(!act)return;
 if(act==='drop'){if(await yn(t('remove'),el('p','',p.name||p.host))&&(conn?.id!==p.id||await mayLeaveDatabase())){await rpc('deleteProfile',{id:p.id});profiles=profiles.filter(x=>x.id!==p.id);if(conn?.id===p.id){await rpc('disconnect');clearViews();conn=null;}render();}return;}
 if(!Number.isInteger(+timeout.value)||+timeout.value<1||+timeout.value>3600||!host.value.trim()||!user.value.trim()||!db.value.trim()||!(+port.value>0&&+port.value<=65535)||ssh.checked&&(!sh.value.trim()||!su.value.trim()||!fp.value.trim()||!(p.has_privateKey||keyData)||!(+sp.value>0&&+sp.value<=65535))){notify(t('required'));continue;}
 if(!tls.checked&&!await yn('TLS',el('p','',t('plain'))))continue;
 let data=await rpc('saveProfile',{id:p.id||'',name:name.value.trim()||host.value.trim(),engine:eng.value,host:host.value.trim(),port:+port.value,database:db.value.trim(),user:user.value.trim(),password:pass.value,timeout:+timeout.value,tls:tls.checked,caAction,caToken,ssh:ssh.checked,sshHost:sh.value.trim(),sshPort:+sp.value,sshUser:su.value.trim(),fingerprint:fp.value.trim(),privateKey:keyData,passphrase:phrase.value});
 profiles=profiles.filter(x=>x.id!==data.id);profiles.push(data);selectedSource=data.id;if(conn?.id===data.id)conn.profileDirty=true;render();notify(t('savedOK'));return data;}
}
async function connectionError(p,e){
 const msg=String(e.message||e),code=msg.split(':',1)[0],key=({CA_UNTRUSTED:'trustError',HOST_MISMATCH:'hostnameError',CERT_EXPIRED:'expiredCert',CERT_NOT_YET_VALID:'futureCert'})[code];
 const body=el('div','connect-error');body.append(el('p','',key?t(key):msg));
 if(key){body.append(node('details','diagnostic',el('summary','',t('technical')),el('pre','preview',msg.slice(code.length+1).trim())));}
 const actions=[[t('close'),null]];if(p.host)actions.push([t(code==='CA_UNTRUSTED'?'importCA':'editConnection'),'edit','primary']);
 if(await dialog(t('connectFailed'),body,actions)==='edit')return profileForm(p,code==='CA_UNTRUSTED',true);
}
async function mayLeaveDatabase(){
 if(!window.currentExplorer?.hasDraft())return true;
 return !!await dialog(t('leaveDraft'),el('p','',t('draftLoss')),[[t('cancel'),false],[t('discardContinue'),true,'danger']]);
}
async function connect(p,demo=false,dbname,destination='sql'){
 if(working){notify(t('busy'));return;}if(!await mayLeaveDatabase()||working)return;working=true;notify(t('loading'));try{
 let c;try{c=await rpc('connect',{id:p.id,engine:p.engine,name:p.name,keep:w.keep,...(dbname?{database:dbname}:{})});}
 catch(e){hideToast();let retry=await connectionError(p,e);if(retry){working=false;await connect(retry,false,dbname,destination);}return;}
 clearViews();conn=c;selectedSource=p.id;conn.database=dbname||p.database||p.name;result=null;lastError='';schemaText='';schemaCatalog=Object.create(null);openSchema.clear();schemaFilter='';if(demo&&(!tab().sql.trim()||tab().sql==='SELECT 1;'))tab().sql=sample;
 if(c.engine==='sqlite')locals=await rpc('localList');working=false;hideToast();cur=destination;render();window.scrollTo(0,0);queueSave();
 }finally{working=false;}
}
function apiQuery(sql,limit=1000,sess=conn?.session){return rpc('query',{session:sess,sql,limit});}
function quoteIdent(n){let q=conn?.engine==='mysql'||conn?.engine==='mariadb'?'\u0060':'"';return q+String(n).split(q).join(q+q)+q;}
function loadSchema(c,force=false){
 const key=JSON.stringify([c.session,c.database]);if(!force&&schemaJob?.key===key)return schemaJob.promise;
 const job={key};schemaJob=job;job.promise=readSchema(c,job).catch(e=>{if(schemaJob===job){schemaJob=null;throw e;}});return job.promise;
}
async function readSchema(c,job){
 let sql=c.engine==='sqlite'?"SELECT m.name,p.name FROM sqlite_master AS m JOIN pragma_table_info(m.name) AS p WHERE m.type IN ('table','view') AND m.name NOT LIKE 'sqlite_%' AND m.name!='android_metadata' ORDER BY m.name,p.cid":c.engine==='postgres'?"SELECT table_schema||'.'||table_name,column_name,table_schema,table_name FROM information_schema.columns WHERE table_schema NOT IN ('pg_catalog','information_schema') ORDER BY table_schema,table_name,ordinal_position":"SELECT table_name,column_name FROM information_schema.columns WHERE table_schema=DATABASE() ORDER BY table_name,ordinal_position";
 let r=await apiQuery(sql,3000,c.session);if(schemaJob!==job)return;
 let by=new Map;tableRefs.clear();for(let [a,b,schema,table] of r.rows){if(!by.has(a))by.set(a,[]);by.get(a).push(b);
 let ref=[schema||null,table||a];if(!tableRefs.has(a))tableRefs.set(a,ref);else if(JSON.stringify(tableRefs.get(a))!==JSON.stringify(ref))tableRefs.set(a,null);}schemaCatalog=Object.fromEntries(by);cm?.updateSchema(schemaCatalog);document.querySelector('.schema-pane')?.refresh?.();schemaText=Array.from(by,([a,cols])=>a+'('+cols.join(', ')+')').join('\n').slice(0,10000);
}
async function connectionMenu(){if(!conn)return;let body=el('p','',conn.engine.toUpperCase()+'  /  '+conn.database);
 let act=await dialog(conn.name,body,[[t('close'),null],...(conn.engine!=='sqlite'?[[t('switch'),'switch']]:[]),[t('refresh'),'refresh'],[t('disconnect'),'off','danger']]);
 if(act==='off'){if(!await mayLeaveDatabase())return;await rpc('disconnect');clearViews();conn=null;schemaText='';result=null;go('conn');}
 if(act==='refresh'){await loadSchema(conn,true);await window.currentExplorer?.refresh();notify(t('done'));render();}
 if(act==='switch'){let c=conn,pg=c.engine==='postgres';let r=await apiQuery(pg?'SELECT datname FROM pg_database WHERE datallowconn ORDER BY datname':'SHOW DATABASES');let sel=select(r.rows.map(row=>[row[0],row[0]]),c.database);let a=await dialog(t('switch'),sel,[[t('cancel'),null],[t('createDB'),'create'],[t('switch'),'switch','primary']]);if(a==='create'){let name=await askName(t('createDB'));if(name)await confirmRun('CREATE DATABASE '+quoteIdent(name));}if(a==='switch'&&sel.value&&sel.value!==c.database){if(pg)await connect(profiles.find(x=>x.id===c.id),false,sel.value,cur);else{if(!await mayLeaveDatabase())return;await apiQuery('USE '+quoteIdent(sel.value));await changedDatabase(sel.value);}}}
}
async function changedDatabase(name){clearViews();conn.database=name;schemaText='';schemaCatalog=Object.create(null);openSchema.clear();schemaFilter='';result=null;lastError='';render();}
function tab(){return w.tabs[w.active]||w.tabs[0];}
function schemaPane(){
 let pane=el('aside','schema-pane');pane.append(node('div','pane-title',icon('tree-view'),el('h2','',t('structure'))));let s=input(schemaFilter,'search',t('schemaSearch'));s.setAttribute('aria-label',t('schemaSearch'));pane.append(node('label','searchbox',icon('search'),s));let tree=el('div','schema-body');pane.append(tree);
 function fill(){tree.replaceChildren();let names=Object.keys(schemaCatalog),filter=schemaFilter.toLowerCase();let keys=names.filter(n=>n.toLowerCase().includes(filter)||schemaCatalog[n].some(v=>v.toLowerCase().includes(filter)));tree.append(el('small','tree-label',t('tables')+'  /  '+keys.length));
 for(let name of keys){const expanded=openSchema.has(name)||!!filter;let b=button('',()=>{openSchema.has(name)?openSchema.delete(name):openSchema.add(name);fill();},'tree-row');b.setAttribute('aria-expanded',String(expanded));b.append(icon(expanded?'chevron--down':'chevron--right'),icon('table'),el('code','',name));const ref=tableRefs.get(name),line=node('div','tree-line',b);
 if(ref){line.append(iconOnly('data-table',t('objects')+' · '+name,async()=>{go('objects');await window.currentExplorer.openTable(...ref);},'tree-preview'));}
 tree.append(line);
 if(expanded)for(let col of schemaCatalog[name]){let bc=tool('column',col,()=>cm?.insert(quoteIdent(col)),'tree-column');bc.title=t('insertName')+' · '+col;tree.append(bc);}}
 if(!keys.length)tree.append(el('small','empty',t('noMatch')));}
 s.oninput=()=>{schemaFilter=s.value;fill();};pane.refresh=fill;fill();return pane;
}
function worksheet(){
 page.append(header('workspace','',tool('machine-learning-model',t('ai'),aiMenu,'ai-action')));if(!conn){page.append(emptyConnection());return;}
 loadSchema(conn).catch(e=>notify(e.message));
 let grid=el('div','workbench'),studio=el('section','studio');page.append(grid);grid.append(schemaPane(),studio);
 let tabs=el('div','tabs');w.tabs.forEach((v,i)=>tabs.append(tool('document',t('tab')+' '+v.n,()=>{w.active=i;restoreResult();queueSave();render();},i===w.active?'active':'')));tabs.append(iconOnly('add',t('newDraft'),()=>addTab('')),iconOnly('close',t('close'),async()=>{if(!await yn(t('close')+' · '+t('tab')+' '+tab().n,el('pre','preview',tab().sql)))return;tabResults.delete(tab().n);editorViews.delete(tab().n);w.tabs.splice(w.active,1);if(!w.tabs.length)w.tabs.push({n:++w.serial,sql:''});w.active=Math.min(w.active,w.tabs.length-1);restoreResult();queueSave();render();}));studio.append(tabs);tabs.scrollLeft=editorViews.get(tab().n)?.tabX||0;const activeButton=tabs.querySelector('.active');if(activeButton&&(activeButton.offsetLeft<tabs.offsetLeft+tabs.scrollLeft||activeButton.offsetLeft+activeButton.offsetWidth>tabs.offsetLeft+tabs.scrollLeft+tabs.clientWidth))tabs.scrollLeft+=activeButton.getBoundingClientRect().left-tabs.getBoundingClientRect().left;
 let lim=select([...new Set([20,50,100,500,w.limit])].sort((a,b)=>a-b).map(v=>[String(v),v+' '+t('rows')]).concat([['?',t('custom')]]),String(w.limit));lim.setAttribute('aria-label',t('limit'));lim.onchange=async()=>{let n=+lim.value;if(lim.value==='?'){let i=input(w.limit,'number');i.min=1;i.max=10000;if(!await yn(t('limit')+' · 1–10000',i)){lim.value=String(w.limit);return;}n=+i.value;}if(Number.isInteger(n)&&n>=1&&n<=10000)w.limit=n;else notify('1–10000');queueSave();render();};
 let run=tool('play--filled',t(working?'busy':'run'),()=>runUser(tab().sql),'primary');run.disabled=working;
 let save=iconOnly('bookmark',t('favorite'),async()=>{if(!tab().sql.trim())return;let name=await askName(t('favorite'),tab().sql.trim().split('\n')[0].slice(0,38));if(name){w.saved.unshift({name,sql:tab().sql});await persist();notify(t('savedOK'));}});
 let bar=node('div','actions',run,lim,save,el('code','editor-context',conn.engine.toUpperCase()+' / '+conn.database));studio.append(bar);
 let cmHost=el('div','cm-host');studio.append(cmHost);cmOwner=tab().n;cm=FieldEditor(cmHost,{value:tab().sql,snapshot:editorViews.get(cmOwner)?.snapshot,engine:conn.engine,schema:schemaCatalog,onChange:s=>{tab().sql=s;queueSave();},run:()=>runUser(tab().sql).catch(showError)});
 let out=el('section','result-pane'),exportBar=el('div','result-meta');studio.append(out);if(result)exportBar.append(el('span','counter',Math.round(lastMs)+' ms'));if(result?.columns.length){for(let f of ['csv','xlsx'])exportBar.append(button(f.toUpperCase(),()=>Explorer.exportData(result,f,api(),'FieldSQL-result'),'ghost'));}
 out.append(node('div','result-head',node('div','row',icon('data-table'),el('h2','',t('result')),result?.columns.length?el('span','count',result.rows.length):el('span')),exportBar));
 if(lastError){out.append(node('div','notice',el('div','err',lastError),button(t('fix'),()=>launchAI('fix'))));return;}
 if(!result){out.append(el('p','empty',t('idle')));return;}if(!result.columns.length){out.append(el('p','empty',result.affected<0?t('done'):t('affected')+' · '+result.affected));return;}
 let table=el('table'),head=el('tr');head.append(el('th','ord','#'));for(let c of result.columns)head.append(el('th','',c));table.append(node('thead','',head));let body=el('tbody');let idx=0;for(let row of result.rows){let tr=el('tr');tr.append(el('td','ord',++idx));for(let cell of row){let td=el('td',cell===null?'dim':'',cell===null?'NULL':cell);td.onclick=()=>void dialog(t('result'),el('pre','preview',cell===null?'NULL':cell),[[t('close'),true]]);tr.append(td);}body.append(tr);}table.append(body);const gridRows=node('div','gridscroll',table);gridRows.boundResult=result;out.append(gridRows);const lastView=editorViews.get(cmOwner);if(lastView?.result===result){gridRows.scrollLeft=lastView.left;gridRows.scrollTop=lastView.top;}if(result.truncated)out.append(el('small','statusline',t('limited')));
}

function restoreResult(){let r=tabResults.get(tab().n);result=r?.value||null;lastError=r?.error||'';lastMs=r?.ms||0;}
function addTab(sql){w.tabs.push({n:++w.serial,sql});w.active=w.tabs.length-1;result=null;lastError='';queueSave();go('sql');}
function emptyConnection(){return node('div','card',el('p','empty',t('choose')),row('↗',t('conn'),t('demo'),()=>go('conn')));}
function safeRead(sql){return /^\s*(SELECT|SHOW|DESCRIBE)\b/i.test(sql)&&!/[;][\s\S]*\S/.test(sql.trim())&&!/\b(INTO|UPDATE|DELETE|INSERT|ALTER|TRUNCATE|DROP|CREATE|CALL|COPY|REPLACE)\b/i.test(sql);}
async function confirmRun(sql,fromExplorer=false){let c=conn;if(!c)return false;if(!await yn(t('confirm'),node('div','',el('small','',c.name+' / '+c.database),el('pre','preview',sql))))return false;
 if(!fromExplorer&&(c.engine==='mysql'||c.engine==='mariadb')&&/\bUSE\s+/i.test(sql)&&!await mayLeaveDatabase())return false;return await execute(sql,c.session,fromExplorer);}
async function runUser(sql){if(!sql.trim()){notify(t('emptyQuery'));return;}if(working){notify(t('busy'));return;}if(!safeRead(sql))await confirmRun(sql);else await execute(sql,conn.session);}
async function execute(sql,sess,fromExplorer=false){
 if(working)throw Error(t('busy'));working=true;let start=performance.now(),owner=tab().n,err='',last;try{last=await apiQuery(sql,w.limit,sess);if(conn?.session===sess){
 if(!fromExplorer&&!safeRead(sql)){
   if((conn.engine==='mysql'||conn.engine==='mariadb')&&/\bUSE\s+/i.test(sql)){let dbName=(await apiQuery('SELECT DATABASE()',1,sess)).rows[0][0];if(conn.database!==dbName)await changedDatabase(dbName);}
   window.currentExplorer?.invalidate();
 }
 tabResults.set(owner,{value:last,error:'',ms:performance.now()-start});restoreResult();}return true;}catch(e){err=e.message;if(conn?.session===sess){tabResults.set(owner,{value:null,error:err,ms:performance.now()-start});restoreResult();}throw e;}finally{working=false;w.history.unshift({sql,db:conn?.database||'',time:Date.now(),err:!!err});w.history=w.history.slice(0,500);queueSave();if(cur==='sql')render();if(!err&&/^\s*(ALTER|CREATE|DROP|RENAME)\b/i.test(sql)&&conn)loadSchema(conn,true).catch(()=>{});}
}
function api(){let c=conn;return {engine:c.engine,lang:w.lang,query:(sql)=>apiQuery(sql,10000,c.session),confirmRun:sql=>confirmRun(sql,true),saveFile:async(name,mime,data)=>{let choice=await dialog(t('export'),el('p','',name),[[t('cancel'),null],[t('share'),'share'],[t('save'),'export','primary']]);if(!choice)throw Error('已取消');await rpc(choice,{name,mime,data});},notify,quoteIdent,copy:text=>rpc('copy',{text})};}
function objects(){let holder=el('div');explorerPane.replaceChildren(header('objects'),holder);window.currentExplorer=Explorer.mount(holder,api());}
function library(){page.append(header('library','04  /  LIBRARY'),node('div','seg',...['saved','history'].map(k=>button(t(k),()=>{shelf=k;render();},k===shelf?'active':''))));let list=w[shelf],card=el('div','card');if(shelf==='history'&&list.length)page.append(button(t('clearHist'),async()=>{if(await yn(t('clearHist')+'?',el('p','',list.length+' / 500'))){w.history=[];await persist();render();}},'quiet danger'));page.append(card);if(!list.length)card.append(el('p','empty',t(shelf==='history'?'noHistory':'noSaved')));for(const [i,v] of list.entries()){let item=node('div','listitem',el('strong','',v.name||(v.db+' · '+new Date(v.time).toLocaleString(w.lang))),el('pre','',v.sql));let acts=node('div','btnrow',button(t('load'),()=>addTab(v.sql)));if(conn)acts.append(button(t('rerun'),async()=>{addTab(v.sql);await runUser(v.sql);}));if(shelf==='saved')acts.append(button(t('delete'),()=>{w.saved.splice(i,1);queueSave();render();},'quiet'));item.append(acts);card.append(item);}}
async function aiMenu(){await launchAI('generate');}
async function launchAI(mode){if(conn)await loadSchema(conn).catch(e=>notify(e.message));let sql=await rpc('ai',{dialect:conn?.engine||'sqlite',schema:schemaText,sql:tab().sql,mode,error:lastError,lang:w.lang,dark:document.documentElement.dataset.theme==='dark'});if(sql)addTab(sql);}
async function preferences(){let div=el('div','settings'),lang=select(languages.map((l,i)=>[l,['中文','English','日本語','한국어','Español'][i]]),w.lang),th=select(['auto','light','dark'].map(k=>[k,t(k)]),w.theme);
 div.append(field(t('lang'),lang),field(t('theme'),th),check(t('lock'),lock,async v=>{lock=await rpc('lock',{value:v});})[0],check(t('service'),w.keep,async v=>{await rpc('keep',{value:v});w.keep=v;await persist();})[0]);
 lang.onchange=async()=>{if(!await mayLeaveDatabase()){lang.value=w.lang;return;}w.lang=lang.value;queueSave();render();};th.onchange=()=>{w.theme=th.value;theme();queueSave();};let action=await dialog(t('settings'),div,[[t('ai'),'ai'],[t('close'),true,'primary']]);render();if(action==='ai')await aiMenu();}
rpc('init').then(data=>{Object.assign(w,{lang:'zh',theme:'dark',limit:100,keep:true,tabs:[{n:1,sql:'SELECT 1;'}],active:0,serial:1,saved:[],history:[]},data.workspace);profiles=data.profiles;locals=data.local;lock=data.lock;if(data.openShelf)cur='library';render();}).catch(e=>page.append(el('p','notice',e.message)));
