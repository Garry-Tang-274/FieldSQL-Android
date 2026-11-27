"""Drive the production Explorer in Chrome against a real, disposable SQLite DB."""
import argparse, base64, csv, io, os, sqlite3, tempfile
from pathlib import Path
from openpyxl import load_workbook
from playwright.sync_api import sync_playwright

ASSETS = Path(__file__).resolve().parents[2] / 'app/src/main/assets'

def main():
    parser = argparse.ArgumentParser(); parser.add_argument('--screens', type=Path)
    args = parser.parse_args()
    db = sqlite3.connect(':memory:')
    db.executescript("""
      PRAGMA foreign_keys=ON;
      CREATE TABLE accounts(id INTEGER PRIMARY KEY, name TEXT NOT NULL);
      CREATE TABLE orders(account_id INTEGER NOT NULL REFERENCES accounts(id), seq INTEGER NOT NULL,
        title TEXT, total NUMERIC, memo TEXT, PRIMARY KEY(account_id,seq));
      CREATE UNIQUE INDEX named_orders ON orders(account_id,title);
      CREATE VIEW report AS SELECT * FROM orders;
      CREATE TABLE no_key(raw TEXT);
      INSERT INTO accounts VALUES(7,'Field');
    """)
    db.executemany("INSERT INTO orders VALUES(7,?,?,?,?)", [(i,f'Order {i:02d}',i*2, None) for i in range(1,24)])
    exports={};sqls=[];notices=[]
    def query(s):
        c=db.execute(s)
        return {'columns':[x[0] for x in c.description or []],'rows':[list(x) for x in c.fetchall()],'truncated':False,'affected':max(0,c.rowcount)}
    def confirm(s):
        sqls.append(s)
        if not allow[0]: return False
        db.execute(s); return True
    allow=[True]
    chrome = os.environ.get('FIELDSQL_CHROME') or next((str(p) for p in [Path('C:/Program Files/Google/Chrome/Application/chrome.exe'),Path('/usr/bin/chromium'),Path('/usr/bin/google-chrome')] if p.exists()),None)
    with sync_playwright() as pw:
        browser=pw.chromium.launch(executable_path=chrome)
        p=browser.new_page(viewport={'width':390,'height':844},device_scale_factor=1)
        errors=[];p.on('pageerror',lambda err:errors.append(str(err)))
        p.expose_function('readSQL',query);p.expose_function('runSQL',confirm)
        p.expose_function('save',lambda n,m,b:exports.update({n:base64.b64decode(b)}))
        p.expose_function('notify',lambda a:notices.append(a))
        p.set_content('<!doctype html><html lang="en"><meta name="viewport" content="width=device-width, initial-scale=1"><style>body{font:16px Arial;margin:16px}button{font:inherit}</style><main id="app"></main></html>')
        p.add_style_tag(path=str(ASSETS/'explorer.css'));p.add_script_tag(path=str(ASSETS/'explorer.js'))
        p.evaluate("""async()=>{window.testAPI={engine:'sqlite',query:readSQL,confirmRun:runSQL,saveFile:save,notify:notify,copy:async x=>{window.copied=x},quoteIdent:s=>'"'+s.replaceAll('"','""')+'"'};window.ex=Explorer.mount(document.querySelector('main'),testAPI);await ex.ready}""")
        def opened(name):
            p.locator('.fx-object').filter(has=p.get_by_text(name,exact=True)).click()
        def b(label): return p.get_by_role('button',name=label,exact=True)
        def tab(name):p.get_by_role('tab',name=name,exact=True).click()
        p.locator('.fx-filter').fill('ord');opened('orders');p.wait_for_selector('.fx-grid tbody tr')
        assert p.locator('.fx-grid tbody tr').count()==20
        assert p.evaluate('document.documentElement.scrollWidth<=innerWidth') is True
        assert p.locator('.fx-side').is_visible() is False
        if args.screens:
            args.screens.mkdir(exist_ok=True,parents=True);p.screenshot(path=str(args.screens/'explorer-portrait.png'))
        b('Next ›').click();p.wait_for_function("document.querySelector('.fx-pager small')?.textContent.includes('21–23')")
        assert p.locator('.fx-grid tbody tr').count()==3
        assert p.evaluate('ex.refresh()') and p.locator('.fx-filter').input_value()=='ord'
        assert '21–23' in p.locator('.fx-pager small').inner_text()
        assert p.evaluate('[ex.back(),ex.back()]')==[True,False]
        assert p.locator('.fx-side').is_visible() and not p.locator('.fx-filter').evaluate('e=>e===document.activeElement')
        assert p.evaluate("ex.openTable(null,'orders')") and '21–23' in p.locator('.fx-pager small').inner_text()
        assert not p.evaluate("ex.openTable(null,'orde')")
        tab('Columns');p.wait_for_selector('.fx-record')
        assert p.evaluate("ex.openTable(null,'orders')") and p.get_by_role('tab',name='Data',exact=True).get_attribute('aria-selected')=='true'
        b('‹ Previous').click();p.wait_for_function("document.querySelector('.fx-pager small')?.textContent.includes('1–20')")
        p.locator('.fx-row-action').first.click()
        assert not p.evaluate('ex.hasDraft()')
        title=p.locator('.fx-field').filter(has=p.get_by_text('title',exact=True))
        title.locator('select').select_option('set');marker='海风, "O\'Brien"\r\n=1+1'
        title.locator('textarea').fill(marker)
        assert p.evaluate('ex.hasDraft()')
        p.evaluate("document.querySelector('main').hidden=true;ex.invalidate()")
        assert not p.evaluate("ex.refresh()");p.evaluate("document.querySelector('main').hidden=false")
        assert not p.evaluate("ex.resume()");assert p.evaluate("ex.back()") and p.evaluate("ex.openTable(null,'orders')")
        assert title.locator('textarea').input_value().replace('\r','')==marker.replace('\r','')
        allow[0]=False;b('Review SQL').click();p.wait_for_timeout(60)
        assert db.execute('SELECT title FROM orders WHERE seq=1').fetchone()[0]=='Order 01'
        assert '"account_id" = 7 AND "seq" = 1' in sqls[-1]
        allow[0]=True;b('Review SQL').click();p.wait_for_selector('.fx-grid');assert not p.evaluate('ex.hasDraft()')
        cell=db.execute('SELECT title FROM orders WHERE seq=1').fetchone()[0];assert cell.replace('\r','')==marker.replace('\r','')
        b('Export This page CSV').click();b('Export This page XLSX').click()
        p.wait_for_timeout(100)
        rows=list(csv.reader(io.StringIO(exports['orders-1.csv'].decode('utf-8-sig'),newline='')))
        assert rows[1][2]==cell
        wb=load_workbook(io.BytesIO(exports['orders-1.xlsx']));assert wb.active['C2'].value==cell and wb.active['D2'].value==2
        p.evaluate("""()=>Explorer.exportData({columns:['A','B'],rows:[['=1+1',true],['<&\\r\\n>','😊']]},'xlsx',testAPI,'literal')""")
        wb=load_workbook(io.BytesIO(exports['literal.xlsx']));assert wb.active['A2'].data_type=='s' and wb.active['B2'].value is True and wb.active['A3'].value=='<&\r\n>'
        p.evaluate("""()=>Explorer.exportData({columns:['Number','Large','Label','Flag','Underflow'],rows:[['800','1234567890123456','00100','t','1e-320']],cellTypes:[['N','N','S','B','N']]},'xlsx',testAPI,'typed')""")
        ws=load_workbook(io.BytesIO(exports['typed.xlsx'])).active
        assert [ws.cell(2,i).data_type for i in range(1,6)]==['n','s','s','b','s'] and ws['A2'].value==800 and ws['C2'].value=='00100'
        blocked=p.evaluate("""async()=>{try{await Explorer.exportData({columns:['binary'],rows:[['⟪BLOB: 512 bytes⟫']]},'csv',testAPI,'unsafe');return false;}catch(e){return e.message.includes('HEX');}}""")
        assert blocked and 'unsafe.csv' not in exports
        b('＋ Add row').click()
        for col,val in [('account_id','7'),('seq','51'),('title','New')]:
            field=p.locator('.fx-field').filter(has=p.get_by_text(col,exact=True));field.locator('select').select_option('set');field.locator('textarea').fill(val)
        b('Review SQL').click();p.wait_for_selector('.fx-grid');assert db.execute('SELECT title FROM orders WHERE seq=51').fetchone()[0]=='New'
        p.locator('.fx-row-action').first.click();b('Delete').click();p.wait_for_selector('.fx-grid');assert db.execute('SELECT COUNT(*) FROM orders WHERE seq=1').fetchone()[0]==0
        tab('Columns');p.wait_for_selector('.fx-record');assert 'accounts.id' in p.locator('.fx-area').inner_text()
        b('＋ Add column').click();p.get_by_label('Column name',exact=True).fill("staff's note");b('Review SQL').click();p.wait_for_selector('.fx-record')
        assert "staff's note" in [c[1] for c in db.execute('PRAGMA table_info(orders)')]
        tab('Indexes');p.get_by_text('named_orders',exact=True).wait_for();tab('DDL');b('Copy').click();p.wait_for_timeout(50)
        assert 'CREATE TABLE orders' in p.evaluate('window.copied')
        b('‹ Back to objects').click();p.locator('.fx-filter').fill('no_key');opened('no_key');p.get_by_text('Read-only: no primary key',exact=True).wait_for()
        p.set_viewport_size({'width':850,'height':460});p.locator('.fx-filter').fill('');opened('report');p.get_by_text('Read-only view',exact=True).wait_for()
        assert p.locator('.fx-side').is_visible() and p.locator('.fx-detail').is_visible()
        opened('orders');tab('Columns');p.wait_for_selector('.fx-record')
        if args.screens:p.screenshot(path=str(args.screens/'explorer-landscape.png'))
        p.set_viewport_size({'width':800,'height':1100})
        p.wait_for_function("document.querySelector('.fx-layout').classList.contains('fx-wide')")
        p.evaluate("document.querySelector('main').hidden=true")
        p.wait_for_timeout(50)
        assert p.locator('.fx-layout').evaluate("e=>e.classList.contains('fx-wide')")
        p.evaluate("document.querySelector('main').hidden=false")
        b('Refresh').click();p.wait_for_selector('.fx-record')
        assert p.get_by_role('tab',name='Columns',exact=True).get_attribute('aria-selected')=='true' and not p.evaluate('ex.back()')
        p.evaluate("document.querySelector('main').style.width='550px'")
        p.wait_for_function("!document.querySelector('.fx-layout').classList.contains('fx-wide')")
        assert not p.locator('.fx-side').is_visible() and b('‹ Back to objects').is_visible()
        p.evaluate("document.querySelector('main').style.width=''")
        db.executescript("CREATE TABLE pages_test(id INTEGER PRIMARY KEY); WITH RECURSIVE n(x) AS(SELECT 1 UNION ALL SELECT x+1 FROM n WHERE x<61) INSERT INTO pages_test SELECT x FROM n;")
        p.evaluate("ex.invalidate()");assert p.evaluate("ex.openTable(null,'pages_test')")
        for start in ('21–40','41–60','61–61'):
            b('Next ›').click();p.wait_for_function(f"document.querySelector('.fx-pager small')?.textContent.includes('{start}')")
        db.execute("DELETE FROM pages_test WHERE id>34");assert p.evaluate('ex.refresh()')
        assert '21–34' in p.locator('.fx-pager small').inner_text()
        db.execute('DROP TABLE pages_test');p.evaluate("document.querySelector('main').hidden=true;ex.invalidate()")
        assert p.locator('.fx-heading h2').text_content()=='pages_test'
        p.evaluate("document.querySelector('main').hidden=false");p.evaluate('ex.resume()')
        p.wait_for_function("!document.querySelector('.fx-object[aria-current=true]')")
        assert not errors and notices==['Copied'], (errors,notices)
        check_mysql(p)
        check_pg_link(p)
        browser.close()
    print('PASS: SQL/exports/row edits, narrow back, persistent drafts/filter/page, hidden resize/invalidation, deleted objects and page clamp; exact schema/table links, MySQL column reorder.')

def check_mysql(p):
    p.evaluate("""async()=>{const q=async sql=>{let rows;
    if(sql.includes('information_schema.routines'))rows=[{ns:'demo',name:'items',kind:'table',oid:null}];
    else if(sql.includes('information_schema.columns'))rows=['id','label','rank'].map((name,i)=>({name,dtype:'varchar(90)',nullable:'YES',dflt:null,extra:'',pk:i?0:1,fk:null}));
    else if(sql.startsWith('SHOW CREATE'))rows=[{'Create Table':"CREATE TABLE `items` (\n  `id` int NOT NULL,\n  `label` enum('a,b','b') NOT NULL DEFAULT 'a,b' COMMENT 'accent, key',\n  `rank` int CHECK ((`rank` > 0)),\n  PRIMARY KEY (`id`)\n) ENGINE=InnoDB"}];
    else rows=[];
    return {columns:rows[0]?Object.keys(rows[0]):[],rows,truncated:false};};
    window.ord=[]; const api={...testAPI,engine:'mysql',query:q,confirmRun:async s=>{ord.push(s);return false},quoteIdent:s=>'`'+s.replaceAll('`','``')+'`'};await Explorer.mount(document.querySelector('main'),api);}""".replace('CREATE TABLE `items` (\n','CREATE TABLE `items` (\\n').replace(",\n",",\\n").replace("\n) ENGINE","\\n) ENGINE"))
    p.locator('.fx-object').click();p.get_by_role('tab',name='Columns',exact=True).click();p.get_by_role('button',name='Move down label',exact=True).click()
    p.wait_for_function('window.ord.length===1')
    assert p.evaluate('ord[0]')=="ALTER TABLE `demo`.`items` MODIFY COLUMN `label` enum('a,b','b') NOT NULL DEFAULT 'a,b' COMMENT 'accent, key' AFTER `rank`"

def check_pg_link(p):
    p.evaluate("""async()=>{window.pgSQL=[];const catalog=[['public','Twin','table','1'],['audit','Twin','view','2'],['audit','Twin','function','3']];const q=async sql=>{
        pgSQL.push(sql);if(sql.includes('UNION ALL'))return {columns:['ns','name','kind','oid'],rows:catalog};return {columns:['id'],rows:[]};};
        window.ex=Explorer.mount(document.querySelector('main'),{...testAPI,engine:'postgres',query:q});await ex.ready;}""")
    assert p.evaluate("ex.openTable('audit','Twin')")
    assert any('FROM "audit"."Twin"' in s for s in p.evaluate('pgSQL'))
    assert not p.evaluate("ex.openTable('audit','twin')") and not p.evaluate("ex.openTable(null,'Twin')")
    assert p.evaluate("ex.openTable('public','Twin')")
    assert any('FROM "public"."Twin"' in s for s in p.evaluate('pgSQL'))

if __name__=='__main__': main()
