/* FieldSQL: offline object browser + spreadsheet writer. No CDN or remote script. */
(() => {
  'use strict';
  const words = {
    zh: ['对象浏览','搜索名称或架构','刷新','表','视图','过程','函数','没有匹配的对象','选择一个对象','数据','列','索引','DDL','复制','添加行','编辑','删除','上一页','下一页','行','暂无数据','新增列','列名','数据类型','允许 NULL','取消','预览 SQL','新建行','编辑行','保持默认值','值','设为 NULL','只读：没有可用主键','加载中…','重试','列顺序','上移','下移','主键','外键','默认值','唯一','没有索引','导出','选取数据库对象后查看数据与结构。','点名称打开','第','列定义','查看对象','使用默认值','没有变更','已复制','选填','当前页','仅导出已载入的行','只读视图','列名和类型均为必填项','不支持此列类型表达式','数据库没有返回定义或当前账号没有查看权限。','该行的主键不能精确表示，无法安全编辑。','生成列','索引名','定义','约束','表结构预览','只列出列与约束；索引见「索引」。','超过工作表行列限制，请缩小结果集。','单元格超过 Excel 字符限制，请改用 CSV。','无法导出空的列定义。','字段','请填写字段值，或选择 NULL 或默认值。','导出失败','正序','没有对象。','部分结果','无法获得完整元数据；请提高结果行数限制后重试。','保持原值','提交前会显示完整 SQL。','连续加载更多','返回对象列表','收起','展开'],
    en: ['Objects','Search names or schemas','Refresh','Tables','Views','Procedures','Functions','No matching objects','Select an object','Data','Columns','Indexes','DDL','Copy','Add row','Edit','Delete','Previous','Next','rows','No rows','Add column','Column name','Data type','Allow NULL','Cancel','Review SQL','New row','Edit row','Use default','Value','Set NULL','Read-only: no primary key','Loading…','Retry','Column order','Move up','Move down','Primary key','Foreign key','Default','Unique','No indexes','Export','Select an object to browse its data and structure.','Open an object','Page','Column definition','Browse objects','Use default','No changes','Copied','Optional','This page','Only loaded rows will be exported','Read-only view','Enter a column name and data type','Unsupported column type expression','No definition returned, or permission denied.','This row’s primary key cannot be represented precisely.','Generated','Index name','Definition','Constraint','Table structure preview','Includes columns and constraints. See Indexes for indexes.','Too many rows or columns for Excel. Narrow the result.','Cell exceeds the Excel text limit; use CSV.','No columns to export.','Field','Enter a value, or choose NULL or default.','Export failed','Ascending','No objects.','Partial results','Incomplete metadata. Raise the result row limit and retry.','Leave unchanged','Review the full SQL before applying.','Load more','Back to objects','Collapse','Expand'],
    ja: ['オブジェクト','名前・スキーマを検索','更新','テーブル','ビュー','プロシージャ','関数','一致なし','対象を選択','データ','列','インデックス','DDL','コピー','行を追加','編集','削除','前へ','次へ','行','データなし','列を追加','列名','データ型','NULL を許可','取消','SQL を確認','行の追加','行の編集','既定値','値','NULL に設定','読取専用：主キーなし','読込中…','再試行','列の順番','上へ','下へ','主キー','外部キー','既定値','一意','インデックスなし','エクスポート','データと構造を見る対象を選択してください。','対象を開く','ページ','列定義','対象一覧','既定値','変更なし','コピーしました','任意','現在のページ','読み込み済みの行のみを出力します','読取専用ビュー','列名とデータ型を入力してください','未対応のデータ型です','定義が取得できないか、権限がありません。','主キーを正確に表現できないため編集できません。','生成列','インデックス名','定義','制約','構造プレビュー','列と制約を表示。索引は別タブ。','Excel の行・列制限を超えています','セルが長すぎます。CSV を使用してください。','出力する列なし','フィールド','値・NULL・既定値から選んでください。','出力失敗','昇順','対象なし','一部の結果','メタデータが不完全です。行の上限を増やしてください。','変更しない','適用前に SQL を確認します。','さらに読み込む','一覧に戻る','閉じる','開く'],
    ko: ['개체','이름 또는 스키마 검색','새로고침','테이블','뷰','프로시저','함수','검색 결과 없음','개체 선택','데이터','열','인덱스','DDL','복사','행 추가','편집','삭제','이전','다음','행','데이터 없음','열 추가','열 이름','데이터 형식','NULL 허용','취소','SQL 검토','행 추가','행 편집','기본값 사용','값','NULL 설정','읽기 전용: 기본 키 없음','불러오는 중…','다시 시도','열 순서','위로','아래로','기본 키','외래 키','기본값','고유','인덱스 없음','내보내기','개체를 선택해 데이터와 구조를 확인하세요.','개체 열기','페이지','열 정의','개체 목록','기본값 사용','변경 없음','복사됨','선택 사항','현재 페이지','불러온 행만 내보냅니다','읽기 전용 뷰','열 이름과 형식을 입력하세요','지원하지 않는 형식','정의가 없거나 권한이 없습니다.','기본 키를 정확히 표현할 수 없어 편집할 수 없습니다.','생성 열','인덱스 이름','정의','제약 조건','구조 미리보기','열과 제약 조건을 표시합니다. 인덱스는 별도 탭에 있습니다.','Excel의 행 또는 열 제한 초과','셀 문자 제한 초과. CSV를 사용하세요.','내보낼 열이 없습니다','필드','값, NULL 또는 기본값을 선택하세요.','내보내기 실패','오름차순','개체 없음','일부 결과','메타데이터가 불완전합니다. 행 제한을 높이세요.','변경하지 않음','적용 전 SQL을 검토하세요.','더 보기','목록으로','접기','펼치기'],
    es: ['Objetos','Buscar nombre o esquema','Actualizar','Tablas','Vistas','Procedimientos','Funciones','Sin coincidencias','Selecciona un objeto','Datos','Columnas','Índices','DDL','Copiar','Añadir fila','Editar','Eliminar','Anterior','Siguiente','filas','Sin filas','Añadir columna','Nombre de columna','Tipo de dato','Permitir NULL','Cancelar','Revisar SQL','Nueva fila','Editar fila','Valor por defecto','Valor','Asignar NULL','Solo lectura: sin clave primaria','Cargando…','Reintentar','Orden de columnas','Subir','Bajar','Clave primaria','Clave externa','Predeterminado','Único','Sin índices','Exportar','Selecciona un objeto para explorar sus datos y estructura.','Abrir objeto','Página','Definición de columna','Ver objetos','Por defecto','Sin cambios','Copiado','Opcional','Esta página','Solo se exportan las filas cargadas','Vista de solo lectura','Indica el nombre y el tipo de la columna','Tipo de dato no admitido','Definición no disponible o permiso denegado.','La clave primaria no se puede representar con precisión.','Generada','Nombre de índice','Definición','Restricción','Vista previa de estructura','Incluye columnas y restricciones. Los índices están en su pestaña.','Demasiadas filas o columnas para Excel.','La celda supera el límite de Excel; usa CSV.','No hay columnas para exportar.','Campo','Introduce un valor o selecciona NULL o el predeterminado.','Error al exportar','Ascendente','Sin objetos.','Resultados parciales','Metadatos incompletos. Aumenta el límite de filas.','Sin modificar','Revisa el SQL antes de aplicarlo.','Cargar más','Volver a objetos','Contraer','Expandir']
  };
  const translator = api => {
    const lang = String(api.lang || document.documentElement.lang || 'zh').toLowerCase().split(/[-_]/)[0];
    return i => (words[lang] || words.zh)[i] || words.en[i];
  };
  const enc = new TextEncoder(), normCols = r => (r.columns || []).map(x => typeof x === 'string' ? x : x.name), asArray = (row,cols) => Array.isArray(row) ? row : cols.map(c => row[c]);
  const records = r => {const cols = normCols(r); return (r.rows || []).map(row => Object.fromEntries(cols.map((c,i) => [String(c).toLowerCase(),asArray(row,cols)[i]])));};
  const txt = v => v === null || v === undefined ? '' : typeof v === 'object' ? JSON.stringify(v) : String(v);
  const b64 = bytes => {let s = ''; for(let i=0;i<bytes.length;i+=24576) s+=String.fromCharCode(...bytes.subarray(i,i+24576)); return btoa(s);};
  const crcTable=Array.from({length:256},(_,n)=>{for(let k=0;k<8;k++) n=n&1?0xedb88320^(n>>>1):n>>>1;return n>>>0;});
  const zip = files => {
    const chunks=[],central=[];let off=0,cs=0;
    const h=(n,...fields)=>{const b=new Uint8Array(n),v=new DataView(b.buffer);for(const [w,i,x] of fields)v['setUint'+w](i,x,true);return b;};
    for(const [name,body] of files){const n=enc.encode(name),d=enc.encode(body);let c=0xffffffff;for(const x of d)c=crcTable[(c^x)&255]^(c>>>8);c=(c^0xffffffff)>>>0;
      let l=h(30,[32,0,0x04034b50],[16,4,20],[16,12,33],[32,14,c],[32,18,d.length],[32,22,d.length],[16,26,n.length]);
      chunks.push(l,n,d); let t=h(46,[32,0,0x02014b50],[16,4,20],[16,6,20],[16,14,33],[32,16,c],[32,20,d.length],[32,24,d.length],[16,28,n.length],[32,42,off]);central.push(t,n);cs+=t.length+n.length;off+=l.length+n.length+d.length;}
    const end=h(22,[32,0,0x06054b50],[16,8,files.length],[16,10,files.length],[32,12,cs],[32,16,off]);
    const b=new Uint8Array(off+cs+22);let a=0;for(const s of [...chunks,...central,end]){b.set(s,a);a+=s.length;}return b;
  };
  const xml = v=>txt(v).replace(/_x[0-9a-f]{4}_/gi,m=>'_x005F_'+m.slice(1)).replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F\uFFFE\uFFFF]/g,c=>'_x'+c.charCodeAt(0).toString(16).padStart(4,'0')+'_').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\r/g,'&#13;');
  function xlsx(rows,t){
    if(rows.length>1048576 || rows[0].length>16384)throw Error(t(66));const cols=rows[0].map((_,i)=>{let x=i+1,s='';while(x){x--;s=String.fromCharCode(65+x%26)+s;x=Math.floor(x/26);}return s;});
    const main='http://schemas.openxmlformats.org/spreadsheetml/2006/main', rel='http://schemas.openxmlformats.org/officeDocument/2006/relationships', pkg='http://schemas.openxmlformats.org/package/2006/relationships';
    const sheet='<worksheet xmlns="'+main+'"><sheetData>'+rows.map((row,i)=>'<row r="'+(i+1)+'">'+row.map((v,j)=>{const a='<c r="'+cols[j]+(i+1)+'"';if(v==null)return a+'/>';if(typeof v==='number'&&Number.isFinite(v))return a+'><v>'+v+'</v></c>';if(typeof v==='boolean')return a+' t="b"><v>'+(v?1:0)+'</v></c>';if(txt(v).length>32767)throw Error(t(67));return a+' t="inlineStr"><is><t xml:space="preserve">'+xml(v)+'</t></is></c>';}).join('')+'</row>').join('')+'</sheetData></worksheet>';
    return zip([['[Content_Types].xml','<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>'],['_rels/.rels','<Relationships xmlns="'+pkg+'"><Relationship Id="rId1" Type="'+rel+'/officeDocument" Target="xl/workbook.xml"/></Relationships>'],['xl/workbook.xml','<workbook xmlns="'+main+'" xmlns:r="'+rel+'"><sheets><sheet name="Data" sheetId="1" r:id="rId1"/></sheets></workbook>'],['xl/_rels/workbook.xml.rels','<Relationships xmlns="'+pkg+'"><Relationship Id="rId1" Type="'+rel+'/worksheet" Target="worksheets/sheet1.xml"/></Relationships>'],['xl/worksheets/sheet1.xml',sheet]]);
  }
  async function exportData(result,format,api,filenameStem='result'){
    const t=translator(api),cols=normCols(result);if(!cols.length)throw Error(t(68));const cells=(result.rows||[]).map(r=>asArray(r,cols));if(cells.some(row=>row.some(v=>typeof v==='string'&&(v.startsWith('⟪BLOB:')||v.endsWith('… [已截断]'))))){const l=String(api.lang||document.documentElement.lang||'zh').split(/[-_]/)[0];throw Error(({zh:'结果包含二进制摘要或被截断的文本。请用 HEX、encode 或分段查询提取原值，再导出。',en:'This result includes a binary preview or truncated text. Select the original value with HEX, encode, or substrings before exporting.',ja:'バイナリプレビューまたは省略された文字列があります。HEX、encode または部分文字列で元の値を取得してください。',ko:'바이너리 미리 보기 또는 잘린 텍스트가 있습니다. HEX, encode 또는 부분 문자열로 원래 값을 조회하세요.',es:'Hay una vista previa binaria o texto truncado. Recupera el valor con HEX, encode o subcadenas antes de exportar.'})[l]||'Use HEX or encode to export binary data.');}const rows=[cols,...cells];
    let bytes,mime;format=String(format).toLowerCase();
    if(format==='csv'){const cell=v=>{const s=txt(v);return /[",\r\n]/.test(s)?'"'+s.replace(/"/g,'""')+'"':s;};bytes=enc.encode('\uFEFF'+rows.map(r=>r.map(cell).join(',')).join('\r\n')+'\r\n');mime='text/csv';}
    else if(format==='xlsx'){const typed=(v,k)=>{if(v==null||typeof v!=='string')return v;if(k==='B'){if(/^(?:true|t|1)$/i.test(v))return true;if(/^(?:false|f|0)$/i.test(v))return false;return v;}if(k==='N'&&/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/i.test(v)){const coeff=v.split(/[eE]/)[0].replace(/[+.-]/g,'').replace(/^0+/,'');const n=Number(v);if(coeff.length<=15&&Number.isFinite(n)&&(n===0?!coeff.length:Math.abs(n)>=2.2250738585072014e-308))return n;}return v;};const types=result.cellTypes;bytes=xlsx(types?[cols,...cells.map((r,i)=>r.map((v,j)=>typed(v,types[i]?.[j])))]:rows,t);mime='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';}else{throw Error('CSV / XLSX');}
    if(result.truncated)api.notify(t(54));const name=String(filenameStem||'result').replace(/[<>:"/\\|?*\x00-\x1f]/g,'_').slice(0,100)||'result';await api.saveFile(name+'.'+format,mime,b64(bytes));
  }
  function dialect(api){
    const e=api.engine,pg=e==='postgres',sq=e==='sqlite',my=!pg&&!sq;
    const qi=s=>api.quoteIdent(String(s)), l=s=>{if(s==null)return 'NULL';let x=String(s);if(x.includes('\0'))throw Error('NUL');if(my)return "CONVERT(X'"+Array.from(enc.encode(x),c=>c.toString(16).padStart(2,'0')).join('')+"' USING utf8mb4)";return (pg?"E'":"'")+(pg?x.replace(/\\/g,'\\\\'):x).replace(/'/g,"''")+"'";};
    const f=o=>[o.schema,o.name].filter(x=>x!==null && x!==undefined && x!=='').map(qi).join('.');
    const all=async(sql)=>{let r=await api.query(sql),out=records(r);if(!r.truncated)return out;if(!/^SELECT\s/i.test(sql)||!out.length)throw Error(translator(api)(75));const size=out.length;while(r.truncated){r=await api.query(sql+' LIMIT '+(size+1)+' OFFSET '+out.length);let a=records(r);if(a.length>size){a=a.slice(0,size);r={...r,truncated:true};}if(!a.length&&r.truncated)throw Error(translator(api)(75));out.push(...a);}return out;};
    const kinds = {table:'table',view:'view',procedure:'procedure',function:'function'};
    async function objects(){
      let s=sq?"SELECT NULL AS ns,name,type AS kind,NULL AS oid FROM sqlite_master WHERE type IN ('table','view') AND name <> 'android_metadata' AND name NOT LIKE 'sqlite!_%' ESCAPE '!' ORDER BY type,name":
        pg?"SELECT n.nspname AS ns,c.relname AS name,CASE WHEN c.relkind IN ('v','m') THEN 'view' ELSE 'table' END AS kind,c.oid::text AS oid,'' AS args FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind IN ('r','p','v','m','f') AND n.nspname NOT LIKE 'pg\\_%' AND n.nspname <> 'information_schema' UNION ALL SELECT n.nspname,p.proname,CASE WHEN p.prokind='p' THEN 'procedure' ELSE 'function' END,p.oid::text,pg_get_function_identity_arguments(p.oid) FROM pg_catalog.pg_proc p JOIN pg_catalog.pg_namespace n ON n.oid=p.pronamespace WHERE p.prokind IN ('p','f','w') AND n.nspname NOT LIKE 'pg\\_%' AND n.nspname <> 'information_schema' ORDER BY 1,3,2":
        "SELECT table_schema AS ns,table_name AS name,CASE WHEN table_type='VIEW' THEN 'view' ELSE 'table' END AS kind,NULL AS oid FROM information_schema.tables WHERE table_schema=DATABASE() UNION ALL SELECT routine_schema,routine_name,LOWER(routine_type),NULL FROM information_schema.routines WHERE routine_schema=DATABASE() ORDER BY 1,3,2";
      return (await all(s)).filter(r=>kinds[r.kind]).map(r=>({schema:r.ns,name:r.name,kind:r.kind,oid:r.oid,args:r.args}));
    }
    const sqlitePragma=(cmd,o)=>'PRAGMA '+cmd+'('+qi(o.name)+')';
    async function columns(o){
      if(sq){let rows=await all(sqlitePragma('table_xinfo',o));if(!rows.length)rows=await all(sqlitePragma('table_info',o));const fk=await all(sqlitePragma('foreign_key_list',o));return rows.filter(r=>+r.hidden!==1).map(r=>({name:r.name,type:r.type||'',nullable:!+r.notnull && !+r.pk,def:r.dflt_value,pk:+r.pk,gen:+r.hidden>1,foreign:fk.filter(z=>z.from===r.name).map(z=>z.table+(z.to?'.'+z.to:'' )).join(', ')}));}
      if(pg){const oid=Number(o.oid);if(!Number.isSafeInteger(oid))throw Error('OID');const sql="SELECT a.attname AS name,format_type(a.atttypid,a.atttypmod) AS dtype, CASE WHEN a.attnotnull THEN 0 ELSE 1 END AS nullable, pg_get_expr(d.adbin,d.adrelid) AS dflt, a.attidentity AS ident, a.attgenerated AS generated, COALESCE((SELECT array_position(i.indkey::smallint[],a.attnum)-array_lower(i.indkey::smallint[],1)+1 FROM pg_catalog.pg_index i WHERE i.indrelid=a.attrelid AND i.indisprimary AND a.attnum=ANY(i.indkey) AND array_position(i.indkey::smallint[],a.attnum)-array_lower(i.indkey::smallint[],1)<i.indnkeyatts),0) AS pk, (SELECT string_agg(format('%s.%I',c.confrelid::regclass,b.attname),', ') FROM pg_catalog.pg_constraint c JOIN pg_catalog.pg_attribute b ON b.attrelid=c.confrelid AND b.attnum=c.confkey[array_position(c.conkey,a.attnum)] WHERE c.contype='f' AND c.conrelid=a.attrelid AND a.attnum=ANY(c.conkey)) AS fk FROM pg_catalog.pg_attribute a LEFT JOIN pg_catalog.pg_attrdef d ON d.adrelid=a.attrelid AND d.adnum=a.attnum WHERE a.attrelid="+oid+" AND a.attnum>0 AND NOT a.attisdropped ORDER BY a.attnum";return (await all(sql)).map(r=>({name:r.name,type:r.dtype,nullable:!!+r.nullable,def:r.dflt,pk:+r.pk,ident:r.ident,gen:!!r.generated||r.ident==='a',generated:r.generated,foreign:r.fk}));}
      const s="SELECT c.column_name AS name,c.column_type AS dtype,c.is_nullable AS nullable,c.column_default AS dflt,c.extra AS extra,(SELECT k.ordinal_position FROM information_schema.key_column_usage k WHERE k.table_schema=c.table_schema AND k.table_name=c.table_name AND k.column_name=c.column_name AND k.constraint_name='PRIMARY') AS pk,(SELECT GROUP_CONCAT(CONCAT(k.referenced_table_schema,'.',k.referenced_table_name,'.',k.referenced_column_name) SEPARATOR ', ') FROM information_schema.key_column_usage k WHERE k.table_schema=c.table_schema AND k.table_name=c.table_name AND k.column_name=c.column_name AND k.referenced_table_name IS NOT NULL) AS fk FROM information_schema.columns c WHERE c.table_schema="+l(o.schema)+" AND c.table_name="+l(o.name)+" ORDER BY c.ordinal_position";
      return (await all(s)).map(r=>({name:r.name,type:r.dtype,nullable:r.nullable==='YES',def:r.dflt,pk:+r.pk||0,gen:/\b(?:VIRTUAL|STORED|PERSISTENT) GENERATED\b/i.test(r.extra||''),foreign:r.fk}));
    }
    async function indexes(o){
      if(sq){const rows=await all(sqlitePragma('index_list',o));return Promise.all(rows.map(async r=>{const d=await all('SELECT sql FROM sqlite_master WHERE type=\'index\' AND name='+l(r.name));const c=d[0]?.sql|| (await all('PRAGMA index_info('+qi(r.name)+')')).map(x=>qi(x.name)).join(', ');return {name:r.name,definition:c,unique:!!+r.unique};}));}
      if(pg)return (await all("SELECT ic.relname AS name,pg_get_indexdef(i.indexrelid) AS definition, CASE WHEN i.indisunique THEN 1 ELSE 0 END AS unq FROM pg_catalog.pg_index i JOIN pg_catalog.pg_class ic ON ic.oid=i.indexrelid WHERE i.indrelid="+Number(o.oid)+" ORDER BY 1")).map(r=>({...r,unique:!!+r.unq}));
      const rs=await all('SHOW INDEX FROM '+f(o)),g=new Map();for(const r of rs){if(!g.has(r.key_name))g.set(r.key_name,{name:r.key_name,unique:!+r.non_unique,fields:[]});g.get(r.key_name).fields.push([+r.seq_in_index,r.column_name?qi(r.column_name)+(r.sub_part?'('+Number(r.sub_part)+')':'')+(r.collation==='D'?' DESC':''):r.expression||'']);}return Array.from(g.values(),r=>({name:r.name,unique:r.unique,definition:r.fields.sort((a,b)=>a[0]-b[0]).map(z=>z[1]).join(', ')}));
    }
    async function ddl(o,cols){
      if(sq)return (await all("SELECT sql FROM sqlite_master WHERE name="+l(o.name)+" AND type="+l(o.kind)))[0]?.sql;
      if(my){const r=(await all('SHOW CREATE '+o.kind.toUpperCase()+' '+f(o)))[0]||{};return r['create '+o.kind];}
      const id=Number(o.oid);
      if(o.kind==='view'){const d=(await all("SELECT c.relkind AS kind, pg_get_viewdef(c.oid,true) AS def FROM pg_catalog.pg_class c WHERE c.oid="+id))[0];return d&&(d.kind==='m'?'CREATE MATERIALIZED VIEW ':'CREATE VIEW ')+f(o)+' AS\n'+d.def;}
      if(o.kind!=='table')return (await all('SELECT pg_get_functiondef('+id+') AS def'))[0]?.def;
      const constraints=await all("SELECT conname AS name,pg_get_constraintdef(oid,true) AS def FROM pg_catalog.pg_constraint WHERE conrelid="+id+" AND contype IN ('p','f','u','c','x') ORDER BY conname");
      const c=cols.map(c=>qi(c.name)+' '+c.type+(c.generated?' GENERATED ALWAYS AS ('+c.def+') '+(c.generated==='v'?'VIRTUAL':'STORED'):c.ident?' GENERATED '+(c.ident==='a'?'ALWAYS':'BY DEFAULT')+' AS IDENTITY':c.def!=null?' DEFAULT '+c.def:'')+(c.nullable?'':' NOT NULL'));
      return 'CREATE TABLE '+f(o)+' (\n  '+[...c,...constraints.map(z=>'CONSTRAINT '+qi(z.name)+' '+z.def)].join(',\n  ')+'\n);';
    }
    function value(v,c){
      if(v==null)return 'NULL';if(typeof v==='object'||(typeof v==='number'&&(!Number.isFinite(v)||(Number.isInteger(v)&&!Number.isSafeInteger(v)))))throw Error(translator(api)(59));
      if(typeof v==='boolean')return v?'TRUE':'FALSE';
      if(/^(?:tinyint|smallint|mediumint|int(?:eger)?|bigint|int[248]|numeric|decimal|real|double|float|serial|money)\b/i.test(c.type||'') && /^[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[-+]?\d+)?$/i.test(String(v)))return String(v);
      return l(v);
    }
    return {pg,sq,my,qi,l,f,all,objects,columns,indexes,ddl,value};
  }
  // Extract exactly one complete SHOW CREATE column clause before changing only its position.
  function mysqlClause(ddl,name) {
    const start=ddl.indexOf('(');if(start<0)return null;const parts=[];let depth=0,q='',at=start+1;
    for(let i=at;i<ddl.length;i++){const c=ddl[i];if(q){if(c===q){if(ddl[i+1]===q)i++;else q='';}else if(c==='\\'&&q!=='`')i++;continue;}
      if(c==='`'||c==="'"||c==='"'){q=c;continue;}if(c==='(')depth++;else if(c===')'){if(!depth){parts.push(ddl.slice(at,i).trim());break;}depth--;}
      else if(c===','&&!depth){parts.push(ddl.slice(at,i).trim());at=i+1;}}
    return parts.find(x=>{const m=/^`((?:[^`]|``)*)`\s/.exec(x);return m && m[1].replace(/``/g,'`')===name;})||null;
  }
  function mount(root,api){
    const t=translator(api),d=dialect(api), marker={};root._fsExplorer=marker;const alive=()=>root._fsExplorer===marker&&root.contains(layout);
    const dom=(tag,cl,text)=>{const n=document.createElement(tag);if(cl)n.className=cl;if(text!==undefined)n.textContent=text;return n;};
    const glyph=(key,fallback)=>{const span=dom('span','fx-symbol');span.setAttribute('aria-hidden','true');const svg=window.carbonGlyphs?.[key];if(svg)span.append(new DOMParser().parseFromString(svg,'image/svg+xml').documentElement);else span.textContent=fallback;return span;}; const div=(cl,...els)=>{const x=dom('div',cl);x.append(...els);return x;};
    const action=(label,fn,cls='')=>{const b=dom('button','fx-button '+cls,label);b.type='button';b.onclick=async ev=>{ev.preventDefault();try{await fn(ev);}catch(err){if(alive())api.notify(String(err.message||err));}};return b;};
    let token=0,treeToken=0,items=[],active=null,cols=[],tab='data',page=0,needle='',listError='',lastRowCount=20,offset=0,offsets=[0];
    let pending=false,flight=null,editing=false,dirty=false,loaded=false,listPosition=0;
    const folds=new Set(),visible=()=>alive()&&root.isConnected&&root.clientWidth>0;
    const scrollHost=()=>{for(let p=root.parentElement;p&&p!==document.body;p=p.parentElement){if(/^(auto|scroll)$/.test(getComputedStyle(p).overflowY))return p;}return document.scrollingElement;};
    const layout=div('fx-layout'),side=dom('aside','fx-side'),detail=dom('section','fx-detail'),filter=dom('input','fx-filter'),list=div('fx-list'),sideHead=div('fx-side-head',dom('h2','',t(0)));
    const refresh=action('↻',refreshNow,'fx-icon');refresh.title=t(2);refresh.setAttribute('aria-label',t(2));sideHead.append(refresh);
    filter.type='search';filter.placeholder=t(1);filter.setAttribute('aria-label',t(1));filter.oninput=()=>{needle=filter.value.toLocaleLowerCase();drawList();};side.append(sideHead,filter,list);layout.append(side,detail);root.replaceChildren(layout);
    const kinds=['table','view','procedure','function'],icon=['▦','◫','↳','ƒ'],tabs=['data','cols','idx','ddl'];
    const selected=(o)=>active && o.schema===active.schema&&o.name===active.name&&o.kind===active.kind&&o.oid===active.oid;
    function drawList(){
      const y=list.scrollTop;list.replaceChildren();if(listError){list.append(errorBox(listError,refreshNow));return;}
      const match=items.filter(o=>[o.schema,o.name,o.args].filter(Boolean).join('.').toLocaleLowerCase().includes(needle));if(!match.length){list.append(dom('p','fx-note',t(needle?7:73)));return;}
      kinds.forEach((kind,i)=>{const group=match.filter(o=>o.kind===kind);if(!group.length)return;const fold=dom('details','fx-group');fold.open=!folds.has(kind);fold.ontoggle=()=>{if(fold.isConnected){if(fold.open)folds.delete(kind);else folds.add(kind);}};
        const summary=dom('summary','fx-group-head');summary.append(dom('span','',t(3+i)),dom('span','fx-badge',String(group.length)));fold.append(summary);
        group.forEach(o=>{const b=action('',()=>select(o),'fx-object');b.setAttribute('aria-current',selected(o)?'true':'false');const words=div('fx-object-text',dom('strong','',o.name+(o.args?'('+o.args+')':'')));if(o.schema)words.append(dom('small','',o.schema));b.append(glyph(['table','layers','branch','code'][i],icon[i]),words);fold.append(b);});list.append(fold);});list.scrollTop=y;
    }
    function blank(){detail.replaceChildren(div('fx-blank',glyph('tree-view','▦'),dom('h3','',t(8)),dom('p','fx-note',t(44))));}
    function errorBox(err,retry){const b=div('fx-error',dom('p','',String(err?.message||err)));if(retry)b.append(action(t(34),retry));return b;}
    async function loadObjects(){
      const id=++treeToken,selection=active,section=tab;listError='';
      if(!loaded){list.replaceChildren(dom('p','fx-note',t(33)));if(!active)blank();}
      try{const fresh=await d.objects();if(!alive()||id!==treeToken)return false;
        if(editing){pending=true;return false;}items=fresh;loaded=true;const same=active&&items.find(selected);
        if(!same&&active){active=null;cols=[];token++;gotoList(false);blank();}
        drawList();
        if(same&&active===selection&&tab===section){active=same;cols=[];await render(true);}
        return true;
      }catch(e){if(!alive()||id!==treeToken)return false;listError=e.message||String(e);drawList();return false;}
    }
    function flush(){
      if(flight)return flight;if(!pending||editing||!visible())return Promise.resolve(false);
      flight=(async()=>{let ok=false;do{pending=false;ok=await loadObjects();}while(pending&&!editing&&visible());return ok;})().finally(()=>{flight=null;});
      return flight;
    }
    function refreshNow(){pending=true;return flush();}
    function invalidate(){pending=true;if(visible()&&!editing)void flush();}
    function gotoList(focus=true){layout.classList.remove('fx-open');if(!layout.classList.contains('fx-wide')&&visible()){
      const host=scrollHost();if(host)host.scrollTop=listPosition;if(focus)(list.querySelector('[aria-current="true"]')||refresh).focus({preventScroll:true});}}
    function back(){if(!visible()||layout.classList.contains('fx-wide')||!layout.classList.contains('fx-open'))return false;gotoList();return true;}
    function editWarning(){const lang=String(api.lang||document.documentElement.lang||'zh').split(/[-_]/)[0];
      api.notify(({en:'Save or cancel the current edit first.',ja:'先に現在の編集を保存またはキャンセルしてください。',ko:'먼저 편집을 저장하거나 취소하세요.',es:'Guarda o cancela la edición actual primero.'})[lang]||'请先保存或取消当前编辑。');}
    async function select(o,tb){
      if(!layout.classList.contains('fx-wide')&&!layout.classList.contains('fx-open'))listPosition=scrollHost()?.scrollTop||0;
      layout.classList.add('fx-open');
      if(selected(o)&&(!tb||tb===tab))return true;
      if(editing&&dirty){editWarning();return false;}editing=dirty=false;
      if(selected(o)){tab=tb;await render();return true;}
      active=o;cols=[];tab=(o.kind==='table'||o.kind==='view')?(tabs.includes(tb)?tb:'data'):'ddl';page=0;offset=0;offsets=[0];
      drawList();if(!layout.classList.contains('fx-wide')&&visible()){const host=scrollHost();if(host)host.scrollTop=0;}
      await render();return true;
    }
    async function openTable(schema,name){
      await ready;if(!alive()||typeof name!=='string')return false;
      const match=()=>items.find(o=>(o.kind==='table'||o.kind==='view')&&o.name===name&&(schema!=null?schema===o.schema:!d.pg));
      if(pending&&!editing)await flush();let o=match();if(!o&&!editing){await refreshNow();o=match();}
      return o?select(o,'data'):false;
    }
    const note=s=>dom('p','fx-note',s), strong=s=>dom('strong','',s);
    function top(o,host){
      const back=action('‹ '+t(79),gotoList,'fx-back');const summary=div('fx-heading',div('fx-heading-main',dom('small','fx-eyebrow',o.schema?o.schema+'  /  '+t(3+kinds.indexOf(o.kind)):t(3+kinds.indexOf(o.kind))),dom('h2','',o.name+(o.args?'('+o.args+')':''))));
      host.replaceChildren(back,summary);const strip=div('fx-tabs');strip.setAttribute('role','tablist');
      (o.kind==='view'||o.kind==='table'?tabs:['ddl']).forEach(k=>{const btn=action(t(9+tabs.indexOf(k)),()=>{if(editing&&dirty){editWarning();return;}editing=dirty=false;tab=k;return pending?flush():render();},'fx-tab');btn.setAttribute('role','tab');btn.setAttribute('aria-selected',String(k===tab));strip.append(btn);});const area=div('fx-area');host.append(strip,area);return area;
    }
    async function render(keep=false){
      if(!active)return;const id=++token,o=active,section=tab,stage=keep===true?dom('div'):detail,area=top(o,stage),valid=()=>alive()&&token===id;area.append(note(t(33)));
      try{if(['table','view'].includes(o.kind)&&!cols.length){const found=await d.columns(o);if(!valid())return;cols=found;}
        if(!valid())return;
        if(section==='data')await dataView(o,area,valid);
        if(section==='cols'&&valid())columnsView(o,area);
        if(section==='idx'){const indexes=await d.indexes(o);if(!valid())return;area.replaceChildren();if(!indexes.length){area.append(note(t(42)));return;}
          for(const x of indexes)area.append(div('fx-record',div('fx-record-title',strong(x.name),...(x.unique?[dom('small','fx-pill',t(41))]:[])),dom('code','fx-index',x.definition||'')));}
        if(section==='ddl'){const def=await d.ddl(o,cols);if(!valid())return;area.replaceChildren();if(!def){area.append(note(t(58)));return;}
          const copy=action(t(13),async()=>{await api.copy(def);api.notify(t(51));});const preview=d.pg&&o.kind==='table';area.append(div('fx-bar',strong(preview?t(64):'DDL'),copy));if(preview)area.append(note(t(65)));area.append(codeView(def));}}
      catch(err){if(valid())area.replaceChildren(errorBox(err,()=>render()));}
      finally{if(valid()&&stage!==detail&&!editing){const x=detail.querySelector('.fx-gridwrap')?.scrollLeft||0,host=scrollHost(),y=host?.scrollTop||0;
        detail.replaceChildren(...stage.childNodes);const grid=detail.querySelector('.fx-gridwrap');if(grid)grid.scrollLeft=x;if(host&&visible())host.scrollTop=y;}}
    }
    function codeView(sql){const pre=dom('pre','fx-code'),rx=/('(?:''|[^'])*'|"(?:""|[^"])*"|`(?:``|[^`])*`|\b(?:CREATE|TABLE|VIEW|PRIMARY|KEY|REFERENCES|DEFAULT|NOT|NULL|UNIQUE|INDEX|CONSTRAINT|CHECK|AS|SELECT|FROM|FUNCTION|PROCEDURE|BEGIN|END|ALTER|GENERATED|ALWAYS|IDENTITY|RETURNS|USING|WITH)\b)/gi;let at=0;for(const m of String(sql).matchAll(rx)){pre.append(document.createTextNode(sql.slice(at,m.index)),dom('span',/^['"`]/.test(m[0])?'fx-sql-id':'fx-sql-key',m[0]));at=m.index+m[0].length;}pre.append(document.createTextNode(sql.slice(at)));return pre;}
    const pk=()=>cols.filter(c=>c.pk).sort((a,b)=>a.pk-b.pk);
    async function dataView(o,area,valid){
      const key=pk(),edit=o.kind==='table'&&key.length>0,limit=20;
      const r=await api.query('SELECT * FROM '+d.f(o)+(key.length?' ORDER BY '+key.map(c=>d.qi(c.name)).join(', '):'')+' LIMIT '+(limit+1)+' OFFSET '+offset);if(!valid())return;
      const names=normCols(r),received=(r.rows||[]).map(z=>asArray(z,names)),rows=received.slice(0,limit),hasNext=r.truncated||received.length>limit;lastRowCount=rows.length;
      if(!rows.length&&page>0){
        const count=await api.query('SELECT COUNT(*) AS total FROM '+d.f(o));if(!valid())return;
        const n=Number(asArray(count.rows[0],normCols(count))[0]);
        do{page--;}while(page>0&&offsets[page]>=n);offset=offsets[page];offsets.length=page+1;
        return dataView(o,area,valid);
      }
      area.replaceChildren();const controls=div('fx-actions');if(edit)controls.append(action('＋ '+t(14),()=>editRow(o,null),'fx-primary'));else controls.append(dom('small','fx-pill',t(o.kind==='view'?55:32)));
      for(const fmt of ['csv','xlsx']){const b=action(fmt.toUpperCase(),()=>exportData({columns:names,rows,cellTypes:r.cellTypes?.slice(0,limit)},fmt,api,o.name+'-'+(page+1)));b.setAttribute('aria-label',t(43)+' '+t(53)+' '+fmt.toUpperCase());b.title=t(43)+' · '+t(53);controls.append(b);}area.append(controls);
      const grid=dom('table','fx-grid'),head=dom('thead'),hr=dom('tr');if(edit)hr.append(dom('th','fx-pinned',t(15)));for(const name of names)hr.append(dom('th','',name));head.append(hr);grid.append(head);const body=dom('tbody');grid.append(body);
      rows.forEach(row=>{const tr=dom('tr');if(edit){const b=action(t(15),()=>editRow(o,{names,row}),'fx-row-action');const td=dom('td','fx-pinned');td.append(b);tr.append(td);}
        row.forEach(v=>{const td=dom('td',v==null?'fx-null':'',v==null?'NULL':txt(v));tr.append(td);});body.append(tr);});
      area.append(div('fx-gridwrap',grid));if(!rows.length)area.append(note(t(20)));
      const prev=action('‹ '+t(17),()=>{if(page>0){page--;offset=offsets[page];return render();}}),next=action(t(18)+' ›',()=>{if(hasNext&&lastRowCount){offset+=lastRowCount;offsets[++page]=offset;return render();}});
      prev.disabled=page===0;next.disabled=!hasNext||!rows.length;area.append(div('fx-pager',prev,dom('small','',(rows.length?String(offset+1)+'–'+(offset+rows.length):'0')+' · '+t(19)),next));
    }
    const qref=(o,c)=>'ALTER TABLE '+d.f(o)+' '+c;
    function columnsView(o,area){
      area.replaceChildren();if(o.kind==='table')area.append(div('fx-actions',action('＋ '+t(21),()=>addColumn(o),'fx-primary')));
      cols.forEach((c,idx)=>{const title=div('fx-record-title',strong(c.name),dom('code','fx-type',c.type));const marks=div('fx-marks');if(c.pk)marks.append(dom('small','fx-pill','PK '+c.pk));if(!c.nullable)marks.append(dom('small','fx-pill','NOT NULL'));if(c.gen)marks.append(dom('small','fx-pill',t(60)));const card=div('fx-record',title,marks);
        if(c.foreign)card.append(note(t(39)+' → '+c.foreign));if(c.def!=null)card.append(note(t(40)+': '+c.def));
        if(d.my&&o.kind==='table'){const move=(to)=>reorder(o,c,idx,to);const up=action('↑',()=>move(idx-1),'fx-icon'),down=action('↓',()=>move(idx+1),'fx-icon');up.disabled=idx===0;down.disabled=idx===cols.length-1;up.setAttribute('aria-label',t(36)+' '+c.name);down.setAttribute('aria-label',t(37)+' '+c.name);card.append(div('fx-moves',up,down));card.draggable=true;card.ondragstart=e=>e.dataTransfer.setData('text/plain','fieldsql:'+idx);card.ondragover=e=>e.preventDefault();card.ondrop=e=>{e.preventDefault();const m=/^fieldsql:(\d+)$/.exec(e.dataTransfer.getData('text/plain'));if(m&&cols[+m[1]]&&+m[1]!==idx)reorder(o,cols[+m[1]],+m[1],idx).catch(err=>api.notify(err.message||String(err)));};}area.append(card);});
    }
    async function reorder(o,c,from,to){if(from===to||to<0||to>=cols.length)return;const id=token,saved=cols.slice();const raw=await d.ddl(o,saved);if(!alive()||token!==id)return;const clause=mysqlClause(raw||'',c.name);if(!clause)throw Error(t(58));
      saved.splice(from,1);saved.splice(to,0,c);const sql=qref(o,'MODIFY COLUMN '+clause+' '+(to===0?'FIRST':'AFTER '+d.qi(saved[to-1].name)));if(await api.confirmRun(sql)&&alive()&&id===token){cols=[];await render();}}
    // Use an in-panel sheet, not an OS/WebView modal: the native app owns SQL confirmation.
    function finishEdit(committed=false){editing=dirty=false;if(committed)pending=true;
      if(pending&&visible())return flush();return render();}
    function form(o,label){
      ++token;editing=true;dirty=false;
      const area=detail.querySelector('.fx-area');const panel=div('fx-form'),fields=div('fx-fields'),error=note(''),actions=div('fx-actions');panel.append(dom('h3','',label),fields,error,actions);error.classList.add('fx-inline-error');
      fields.addEventListener('input',()=>{dirty=true;});fields.addEventListener('change',()=>{dirty=true;});
      area.replaceChildren(panel);actions.append(action(t(25),()=>finishEdit()));return {field:(name,input,hint)=>{const row=dom('label','fx-field');row.append(dom('span','',name),input);if(hint)row.append(dom('small','',hint));fields.append(row);},fields,error,actions,
        submit:(s,make,cl='fx-primary')=>{const id=token;const b=action(s,async()=>{if(id!==token)return;try{error.textContent='';const sql=make();if(!sql){error.textContent=t(50);return;}b.disabled=true;const yes=await api.confirmRun(sql);if(yes&&alive()&&token===id)await finishEdit(true);}
          catch(e){error.textContent=e.message||String(e);}finally{b.disabled=false;}},cl);actions.append(b);}};
    }
    function addColumn(o){const f=form(o,t(21)),name=dom('input'),type=dom('input'),allow=dom('input');name.maxLength=128;name.autocomplete='off';type.value='TEXT';allow.type='checkbox';allow.checked=true;f.field(t(22),name);f.field(t(23),type);f.field(t(24),allow);
      f.submit(t(26),()=>{const n=name.value.trim(),s=type.value.trim().replace(/\s+/g,' ').toUpperCase();if(!n||!s)throw Error(t(56));
        if(!/^(?:SMALLINT|TINYINT|MEDIUMINT|INTEGER|INT|BIGINT|REAL|DOUBLE(?: PRECISION)?|FLOAT|NUMERIC|DECIMAL|BOOL(?:EAN)?|CHAR(?:ACTER)?|VARCHAR|CHARACTER VARYING|N?TEXT|BLOB|BYTEA|JSONB?|UUID|DATE|DATETIME|TIMESTAMP(?: WITH(?:OUT)? TIME ZONE)?|TIME|BINARY|VARBINARY)(?:\(\d{1,6}(?:\s*,\s*\d{1,6})?\))?(?: UNSIGNED)?$/.test(s))throw Error(t(57));
        return qref(o,'ADD COLUMN '+d.qi(n)+' '+s+(allow.checked?'':' NOT NULL'));});name.focus();}
    function editRow(o,rec){
      const binary=c=>/\b(?:bytea|binary|varbinary|(?:tiny|medium|long)?blob)\b/i.test(c.type||'');const clipped=v=>typeof v==='string'&&(v.startsWith('⟪BLOB:')||/… \[已截断\]$/.test(v));const keys=pk(),by=new Map(rec?rec.names.map((n,i)=>[n,rec.row[i]]):[]);if(rec&&keys.some(c=>binary(c)||clipped(by.get(c.name))||by.get(c.name)==null||typeof by.get(c.name)==='object'||(typeof by.get(c.name)==='number'&&(!Number.isFinite(by.get(c.name))||!Number.isSafeInteger(by.get(c.name))&&Number.isInteger(by.get(c.name)))))){api.notify(t(59));return;}
      const where=()=>keys.map(c=>d.qi(c.name)+' = '+d.value(by.get(c.name),c)).join(' AND ');const f=form(o,t(rec?28:27)),fields=[];
      for(const c of cols){const value=by.get(c.name);const locked=c.gen||binary(c)||clipped(value)||(rec&&(c.pk||!by.has(c.name)||typeof value==='object'&&value!==null));if(locked){const row=div('fx-field fx-locked',strong(c.name),dom('code','',c.gen?t(60):txt(value)));f.fields.append(row);continue;}
        const mode=dom('select'),input=dom('textarea');input.rows=1;const add=(val,label)=>{const opt=dom('option','',label);opt.value=val;mode.append(opt);};
        add('keep',t(rec?76:29));add('set',t(30));if(c.nullable)add('null',t(31));input.value=value==null?'':txt(value);input.disabled=true;mode.setAttribute('aria-label',c.name);input.setAttribute('aria-label',t(30)+' · '+c.name);
        mode.onchange=()=>{input.disabled=mode.value!=='set';if(!input.disabled)input.focus();};let hints=[c.type,c.pk?'PK '+c.pk:'',!c.nullable?'NOT NULL':'',c.foreign?'→ '+c.foreign:''].filter(Boolean).join(' · ');
        const box=div('fx-field',div('fx-record-title',strong(c.name),dom('small','',hints)),div('fx-inputs',mode,input));f.fields.append(box);fields.push({c,mode,input});}
      f.submit(t(26),()=>{const changed=fields.filter(x=>x.mode.value!=='keep'),vals=changed.map(x=>x.mode.value==='null'?'NULL':d.value(x.input.value,x.c));
        if(rec)return changed.length?'UPDATE '+d.f(o)+' SET '+changed.map((x,i)=>d.qi(x.c.name)+' = '+vals[i]).join(', ')+' WHERE '+where():null;
        if(!changed.length)return 'INSERT INTO '+d.f(o)+(d.my?' () VALUES ()':' DEFAULT VALUES');
        return 'INSERT INTO '+d.f(o)+' ('+changed.map(x=>d.qi(x.c.name)).join(', ')+') VALUES ('+vals.join(', ')+')';});
      if(rec)f.submit(t(16),()=>'DELETE FROM '+d.f(o)+' WHERE '+where(),'fx-danger');
    }
    const size=()=>{if(!visible())return;layout.classList.toggle('fx-wide',root.clientWidth>=600);if(pending&&!editing)void flush();};
    size();const observer=new ResizeObserver(size);observer.observe(root);
    const ready=loadObjects();return Object.assign(ready,{ready,back,openTable,refresh:refreshNow,invalidate,resume:flush,hasDraft:()=>editing&&dirty,
      destroy(){observer.disconnect();token++;treeToken++;if(root._fsExplorer===marker)delete root._fsExplorer;}});
  }
  window.Explorer=Object.freeze({mount,exportData});
})();
