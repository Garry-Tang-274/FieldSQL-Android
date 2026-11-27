"""Exercise the shipped UI assets against a disposable SQLite fixture in Chromium."""
import json, sqlite3, os, sys
from pathlib import Path
from playwright.sync_api import sync_playwright, expect
root=Path(__file__).resolve().parents[1]; assets=root/'app/src/main/assets'; output=Path(sys.argv[1]) if len(sys.argv)>1 else None
if output: output.mkdir(parents=True,exist_ok=True)
db=sqlite3.connect(':memory:'); db.executescript("""CREATE TABLE orders(id INTEGER PRIMARY KEY,region TEXT,product TEXT,units INTEGER,revenue NUMERIC);
INSERT INTO orders VALUES (1,'杭州','手冲咖啡',16,512),(2,'上海','冷萃咖啡',12,432),(3,'杭州','冷萃咖啡',8,288),(4,'成都','手冲咖啡',10,320),(5,'上海','意式浓缩',15,360),(6,'成都','意式浓缩',9,216);
CREATE VIEW regional_sales AS SELECT region,SUM(revenue) AS revenue FROM orders GROUP BY region;""")
def bridge(op,body):
 a=json.loads(body)
 if op=='init':return {'profiles':[],'local':[],'workspace':{},'lock':False}
 if op=='connect':return dict(session='fixture',engine='sqlite',id='demo',name='Field Notes')
 if op=='dictionary':return ['orders','regional_sales']
 if op=='localList':return ['Field Notes']
 if op=='query':
  c=db.execute(a['sql']);return dict(columns=[x[0] for x in c.description] if c.description else [],rows=c.fetchall(),truncated=False,affected=c.rowcount)
 return True
def resource(route):
 name=route.request.url.split('.net/',1)[1];p=assets/name
 if name.startswith('ai-fonts/'):p=root/'ai/src/main/assets'/name
 mime={'.html':'text/html','.js':'application/javascript','.css':'text/css','.ttf':'font/ttf'}
 if p.is_file():route.fulfill(body=p.read_bytes(),content_type=mime.get(p.suffix,'application/octet-stream'))
 else:route.fulfill(status=404)
with sync_playwright() as pw:
 exe=Path(os.environ.get('ProgramFiles','C:/Program Files'))/'Google/Chrome/Application/chrome.exe'
 browser=pw.chromium.launch(executable_path=str(exe) if exe.exists() else None)
 for label,size in [('phone',(390,844)),('tablet',(1280,800))]:
  page=browser.new_page(viewport=dict(zip(('width','height'),size)));errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
  page.expose_function('dispatchFixture',bridge);page.add_init_script("window.Native={send:(id,op,a)=>dispatchFixture(op,a).then(v=>window.receive(id,true,v),e=>window.receive(id,false,e.message))}")
  page.route('https://appassets.androidplatform.net/**',resource);page.goto('https://appassets.androidplatform.net/index.html')
  expect(page.locator('.source-record')).to_be_visible()
  if output: page.screenshot(path=str(output/(label+'-sources.png')))
  if label=='tablet':page.get_by_role('button',name='打开工作区').click()
  else:page.locator('.source-record').click()
  expect(page.locator('.cm-content')).to_contain_text('GROUP BY')
  page.get_by_role('button',name='执行 SQL').click();expect(page.locator('.gridscroll')).to_contain_text('800')
  if label=='tablet':
   page.locator('.tree-row').first.click();expect(page.locator('.tree-column').first).to_be_visible()
  assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'),label
  if output:page.screenshot(path=str(output/(label+'-sql.png')))
  page.get_by_role('button',name='新查询',exact=True).click();page.locator('.cm-content').fill('SELECT 42 AS answer');page.get_by_role('button',name='执行 SQL').click();expect(page.locator('.gridscroll')).to_contain_text('42')
  page.get_by_role('button',name='查询 1',exact=True).click();expect(page.locator('.gridscroll')).to_contain_text('800')
  assert not errors,errors;page.close()
 browser.close()
print('Phone and tablet: source selection, CodeMirror input, SQL execution, tabs, results and viewport checks passed.')
