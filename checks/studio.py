"""Exercise the shipped UI assets against a disposable SQLite fixture in Chromium."""
import json, sqlite3, os, sys
from pathlib import Path
from playwright.sync_api import sync_playwright, expect
root=Path(__file__).resolve().parents[1]; assets=root/'app/src/main/assets'; output=Path(sys.argv[1]) if len(sys.argv)>1 else None
if output: output.mkdir(parents=True,exist_ok=True)
db=sqlite3.connect(':memory:'); db.executescript("""CREATE TABLE orders(id INTEGER PRIMARY KEY,region TEXT,product TEXT,units INTEGER,revenue NUMERIC);
INSERT INTO orders VALUES (1,'杭州','手冲咖啡',16,512),(2,'上海','冷萃咖啡',12,432),(3,'杭州','冷萃咖啡',8,288),(4,'成都','手冲咖啡',10,320),(5,'上海','意式浓缩',15,360),(6,'成都','意式浓缩',9,216);
CREATE VIEW regional_sales AS SELECT region,SUM(revenue) AS revenue FROM orders GROUP BY region;""")
db.execute("CREATE TABLE shelf(id INTEGER PRIMARY KEY, title TEXT)")
db.executemany("INSERT INTO shelf VALUES (?,?)",[(n,'Shelf %02d'%n) for n in range(1,47)])
connected=[]
def bridge(op,body):
 a=json.loads(body)
 if op=='init':return {'profiles':[],'local':[],'workspace':{},'lock':False}
 if op=='connect':
  connected.append(a['id']);return dict(session=str(len(connected)),engine='sqlite',id=a['id'],name=a['name'])
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
  def nav(name):page.locator('#nav').get_by_role('button',name=name,exact=True).click()
  def source(action):page.locator('.source-inspector' if label=='tablet' else '.mobile-source-actions').get_by_role('button',name=action,exact=True).click()
  source('浏览表')
  grid=page.locator('.fx-grid');page.get_by_role('button',name='shelf',exact=True).click()
  page.get_by_role('button',name='下一页 ›').click();expect(grid).to_contain_text('Shelf 21')
  if label=='tablet':page.locator('.fx-filter').fill('she')
  # Switching destinations, including the same source itself, must reuse the live page and session.
  count=len(connected);nav('查询库');nav('连接');source('浏览表');assert len(connected)==count
  expect(grid).to_contain_text('Shelf 21')
  if label=='tablet':expect(page.locator('.fx-filter')).to_have_value('she')
  page.get_by_role('tab',name='列',exact=True).click();nav('查询');nav('浏览表');expect(page.get_by_role('tab',name='列')).to_have_attribute('aria-selected','true')
  page.get_by_role('tab',name='数据').click();expect(grid).to_contain_text('Shelf 21')
  page.locator('.fx-row-action').first.click();draft=page.locator('.fx-form textarea').last
  page.get_by_role('combobox',name='title').select_option('set');draft.fill('UNSAVED');nav('查询');nav('浏览表');expect(draft).to_have_value('UNSAVED')
  page.locator('#status').click();page.get_by_role('button',name='断开连接').click()
  expect(page.get_by_role('dialog')).to_contain_text('放弃未提交的表格编辑？')
  page.get_by_role('dialog').get_by_role('button',name='取消').click();expect(draft).to_have_value('UNSAVED')
  page.locator('.fx-form').get_by_role('button',name='取消').click()
  if label=='phone':
   assert page.evaluate('window.back()');expect(page.locator('.fx-filter')).to_be_visible()
  nav('查询')
  expect(page.locator('.cm-content')).to_contain_text('GROUP BY')
  page.get_by_role('button',name='执行 SQL').click();expect(page.locator('.gridscroll')).to_contain_text('800')
  if label=='tablet':
   page.locator('.tree-row').first.click();expect(page.locator('.tree-column').first).to_be_visible()
   page.get_by_role('button',name='浏览表 · orders',exact=True).click();expect(grid).to_contain_text('手冲咖啡')
   nav('查询')
  assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'),label
  if output:page.screenshot(path=str(output/(label+'-sql.png')))
  page.get_by_role('button',name='新查询',exact=True).click();page.locator('.cm-content').fill('SELECT 42 AS answer');page.get_by_role('button',name='执行 SQL').click();expect(page.locator('.gridscroll')).to_contain_text('42')
  page.get_by_role('button',name='查询 1',exact=True).click();expect(page.locator('.gridscroll')).to_contain_text('800')
  nav('连接');page.get_by_role('button',name='本机数据库 · 新建').click()
  page.get_by_role('dialog').locator('input').fill('Another DB');page.get_by_role('button',name='确定',exact=True).click()
  nav('浏览表');expect(page.locator('.fx-filter')).to_have_value('');expect(grid).to_have_count(0)
  assert not errors,errors;page.close()
 browser.close()
print('Phone/tablet: direct table browsing, preserved pagination, tab, filter and edit draft; SQL worksheet, results, navigation and database isolation passed.')
