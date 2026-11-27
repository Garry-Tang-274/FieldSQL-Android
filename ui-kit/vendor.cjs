// Vendored Carbon glyphs and runtime theme tokens; no runtime network requests.
const fs=require('fs'),path=require('path'),{white,g100}=require('@carbon/themes');
const assets=path.resolve(__dirname,'../app/src/main/assets'),lic=path.join(assets,'licenses');
fs.mkdirSync(lic,{recursive:true});const pk=n=>path.join(__dirname,'node_modules',n);
const names='data--base data--structured data-table tree-view terminal notebook settings play--filled machine-learning-model query table chevron--right chevron--down folder document code add close arrow--right upload screen save catalog search copy checkmark--filled cloud--offline database--postgreSQL column layers application connection branch bookmark'.split(' ');
const icons=Object.fromEntries(names.map(n=>[n,fs.readFileSync(pk('@carbon/icons/svg/32/'+n+'.svg'),'utf8').trim()]));
fs.writeFileSync(path.join(assets,'carbon-icons.js'),'/* IBM Carbon Icons / Apache-2.0 */\nwindow.carbonGlyphs='+JSON.stringify(icons)+';\n');
const roles='background layer01 layer02 layerHover01 textPrimary textSecondary borderSubtle00 borderSubtle01 borderStrong01 linkPrimary focus supportSuccess interactive iconPrimary highlight'.split(' ');
const css=(theme)=>roles.map(k=>'--cds-'+k.replace(/[A-Z]/g,c=>'-'+c.toLowerCase())+':'+theme[k]+';').join('');
fs.writeFileSync(path.join(assets,'carbon-tokens.css'),'/* @carbon/themes white + g100 / Apache-2.0 */\n:root{'+css(white)+'}\n:root[data-theme=dark]{'+css(g100)+'}\n');
const radix=require('@radix-ui/colors');const scale=(...sets)=>Object.entries(Object.assign({},...sets)).map(([k,v])=>'--'+k+':'+v+';').join('');
fs.writeFileSync(path.join(assets,'radix.css'),'/* @radix-ui/colors / MIT */\n:root{'+scale(radix.slate,radix.iris)+'}\n:root[data-theme=dark]{'+scale(radix.slateDark,radix.irisDark)+'}\n');
fs.copyFileSync(pk('@radix-ui/colors/LICENSE'),path.join(lic,'Radix-Colors-MIT.txt'));
fs.copyFileSync(pk('@carbon/icons/LICENSE'),path.join(lic,'IBM-Carbon-Apache-2.0.txt'));
let out='';
for(let n of ['codemirror','style-mod','crelt','w3c-keyname','@marijn/find-cluster-break',...['view','state','language','commands','autocomplete','search','lint','lang-sql'].map(x=>'@codemirror/'+x),...['common','lr','highlight'].map(x=>'@lezer/'+x)]){
 let dir=pk(n);if(!fs.existsSync(dir))continue;let file=fs.readdirSync(dir).find(x=>/^licen[sc]e(\.|$)/i.test(x));if(file)out+=n+'\n'+fs.readFileSync(path.join(dir,file),'utf8')+'\n\n';
}
fs.writeFileSync(path.join(lic,'CodeMirror-and-dependencies.txt'),out);
