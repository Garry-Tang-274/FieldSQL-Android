/* One row per concept; keys are stable across the shell and persisted choices. */
if (!Element.prototype.replaceChildren) Element.prototype.replaceChildren=function(...children){this.textContent='';this.append(...children);};
window.languages=['zh','en','ja','ko','es'];
window.lex={};
for(const row of `
conn|连接|Connections|接続|연결|Conexiones
sql|查询|Query|クエリ|쿼리|Consulta
objects|对象|Objects|オブジェクト|객체|Objetos
library|查询库|Library|ライブラリ|보관함|Biblioteca
offline|未连接|Disconnected|未接続|연결 안 됨|Sin conexión
home|数据库工作台|Database workspace|データベース管理|데이터베이스 작업대|Espacio de datos
settings|设置|Settings|設定|설정|Ajustes
demo|离线示例库|Offline sample|オフラインサンプル|오프라인 예제|Ejemplo sin conexión
demoHint|用六笔咖啡订单，试一次分组查询。|Explore six coffee orders with a grouped query.|コーヒーの注文6件を集計しましょう。|커피 주문 6건을 집계해 보세요.|Agrupa seis pedidos de café.
demoOpen|打开示例 →|Open sample →|サンプルを開く →|예제 열기 →|Abrir ejemplo →
profiles|我的服务器|My servers|サーバー|내 서버|Mis servidores
new|新建连接|New connection|接続を追加|새 연결|Nueva conexión
noProfiles|还没有服务器连接。|No saved servers yet.|保存した接続はありません。|저장한 서버가 없습니다.|No hay servidores guardados.
local|本机数据库|On this device|この端末|기기 데이터베이스|En este dispositivo
create|新建|Create|新規|새로 만들기|Crear
import|导入副本|Import a copy|コピーを読込|복사본 가져오기|Importar copia
localHint|SQLite 文件会复制到应用内，原文件保持不变。|SQLite imports are private copies. The original stays unchanged.|SQLite はコピーとして読み込みます。|SQLite 파일은 원본 대신 복사본으로 엽니다.|Se importa una copia de SQLite sin cambiar el original.
name|名称|Name|名前|이름|Nombre
cancel|取消|Cancel|キャンセル|취소|Cancelar
ok|确定|OK|確定|확인|Aceptar
save|保存|Save|保存|저장|Guardar
connect|连接|Connect|接続|연결|Conectar
edit|编辑|Edit|編集|편집|Editar
delete|删除|Delete|削除|삭제|Eliminar
remove|移除连接配置？|Remove saved connection?|保存済みの接続を削除？|저장한 연결을 삭제할까요?|¿Eliminar la conexión guardada?
engine|数据库类型|Engine|エンジン|엔진|Motor
host|主机|Host|ホスト|호스트|Servidor
port|端口|Port|ポート|포트|Puerto
database|数据库名|Database|データベース|데이터베이스|Base de datos
user|用户名|User|ユーザー名|사용자|Usuario
pass|密码|Password|パスワード|비밀번호|Contraseña
keepSecret|留空保留已保存的值|Blank keeps existing value|空欄で既存値を保持|비우면 기존 값 유지|Vacío conserva el valor
tls|TLS 加密并校验服务器身份|Verify server identity with TLS|TLS でサーバーを検証|TLS 서버 신원 검증|Verificar servidor con TLS
plain|关闭 TLS：数据库连接将不验证服务器证书。|TLS off: the database server certificate will not be verified.|TLS 無効：サーバー証明書を検証しません。|TLS 해제: 서버 인증서를 검증하지 않습니다.|Sin TLS: no se validará el certificado del servidor.
ssh|通过 SSH 私钥隧道连接|Use an SSH key tunnel|SSH 鍵トンネル|SSH 키 터널|Túnel con clave SSH
fp|SSH 主机指纹（SHA256:…）|SSH host fingerprint (SHA256:…)|SSH ホスト指紋 (SHA256:…)|SSH 호스트 지문 (SHA256:…)|Huella SSH (SHA256:…)
key|选择 SSH 私钥|Choose SSH private key|SSH 秘密鍵を選択|SSH 개인 키 선택|Elegir clave privada SSH
keyOK|已选私钥|Key selected|鍵を選択済み|키 선택됨|Clave seleccionada
phrase|私钥口令|Key passphrase|鍵のパスフレーズ|키 암호|Frase de la clave
workspace|SQL 工作区|SQL worksheet|SQL ワークシート|SQL 작업실|Editor SQL
choose|先打开一个数据库连接。|Open a database to get started.|先にデータベースを開いてください。|먼저 데이터베이스를 여세요.|Abre una base de datos.
tab|查询|Query|クエリ|쿼리|Consulta
close|关闭|Close|閉じる|닫기|Cerrar
run|执行 SQL|Run SQL|SQL 実行|SQL 실행|Ejecutar SQL
busy|执行中…|Working…|実行中…|실행 중…|En curso…
ai|端侧 AI|Local AI|端末内 AI|기기 AI|IA local
favorite|收藏|Save query|お気に入り|즐겨찾기|Guardar consulta
result|执行结果|Result|結果|결과|Resultado
idle|执行查询后，结果会出现在这里。|Run a query to see its result.|クエリを実行すると結果が表示されます。|쿼리를 실행하면 결과가 표시됩니다.|Ejecuta una consulta para ver el resultado.
rows|行|rows|行|행|filas
limited|已达显示上限|Display limit reached|表示上限|표시 제한 도달|Límite de vista
affected|受影响行数|Rows affected|変更した行|영향받은 행|Filas afectadas
done|已执行|Executed|実行済み|실행 완료|Ejecutada
confirm|确认执行这条 SQL？|Execute this SQL?|この SQL を実行？|이 SQL을 실행할까요?|¿Ejecutar este SQL?
limit|行数上限|Row limit|行数上限|행 제한|Límite de filas
custom|自定|Custom|指定|직접 입력|Personalizado
saved|已收藏|Saved queries|保存済み|저장한 쿼리|Guardadas
history|执行历史|History|履歴|기록|Historial
noSaved|还没有收藏的查询。|No saved queries yet.|保存したクエリはありません。|저장한 쿼리가 없습니다.|No hay consultas guardadas.
noHistory|执行过的最近 500 条查询会保存在这里。|Your last 500 executions appear here.|直近500件の実行履歴を表示します。|최근 500건의 실행 기록을 표시합니다.|Aquí se guardan las últimas 500 ejecuciones.
load|载入编辑器|Load in editor|エディターに読込|편집기에 열기|Abrir en editor
rerun|重新执行|Run again|再実行|다시 실행|Volver a ejecutar
theme|外观|Appearance|外観|모양|Apariencia
light|浅色|Light|ライト|밝게|Claro
dark|深色|Dark|ダーク|어둡게|Oscuro
auto|跟随系统|System|システム|시스템|Sistema
lang|语言|Language|言語|언어|Idioma
lock|返回应用时验证生物识别|Biometric lock on return|復帰時に生体認証|복귀 시 생체 인증|Biometría al volver
service|后台保留远程连接|Keep remote connection in background|バックグラウンド接続|백그라운드 연결 유지|Mantener conexión en segundo plano
disconnect|断开连接|Disconnect|切断|연결 끊기|Desconectar
switch|切换数据库|Switch database|データベースの切替|데이터베이스 전환|Cambiar base de datos
createDB|创建数据库|Create database|データベース作成|데이터베이스 생성|Crear base de datos
generate|生成 SQL|Generate SQL|SQL を生成|SQL 생성|Generar SQL
explain|解释 SQL|Explain SQL|SQL を解説|SQL 설명|Explicar SQL
fix|修正 SQL|Fix SQL|SQL を修正|SQL 수정|Corregir SQL
newDraft|新查询|New query|新規クエリ|새 쿼리|Nueva consulta
chooseMode|选择 AI 操作|Choose an AI task|AI の操作|AI 작업 선택|Elegir tarea de IA
copy|复制|Copy|コピー|복사|Copiar
refresh|刷新结构|Refresh schema|スキーマ更新|스키마 새로 고침|Actualizar esquema
loading|正在连接…|Connecting…|接続中…|연결 중…|Conectando…
required|请补全必填字段|Complete required fields|必須項目を入力してください|필수 항목을 입력하세요|Completa los campos obligatorios
savedOK|已保存|Saved|保存しました|저장됨|Guardado
emptyQuery|请先输入 SQL|Enter SQL first|SQL を入力してください|먼저 SQL을 입력하세요|Introduce SQL
share|分享文件|Share file|共有|파일 공유|Compartir
export|导出|Export|エクスポート|내보내기|Exportar
clearHist|清空历史|Clear history|履歴を消去|기록 지우기|Borrar historial
timeout|查询超时（秒）|Query timeout (seconds)|タイムアウト（秒）|쿼리 제한 시간(초)|Tiempo límite (segundos)
sources|数据源|Data sources|データソース|데이터 소스|Fuentes de datos
details|连接详情|Connection details|接続の詳細|연결 상세|Detalles de conexión
structure|数据库结构|Database structure|データベース構造|데이터베이스 구조|Estructura
schemaSearch|查找表或列|Find table or column|テーブル・列を検索|테이블·열 검색|Buscar tabla o columna
sampleTag|内置示例|Built-in sample|サンプル|기본 예제|Ejemplo incluido
workspaceNav|工作空间|Workspace|ワークスペース|작업 공간|Espacio de trabajo
remote|服务器|Server|サーバー|서버|Servidor
device|此设备|This device|この端末|이 기기|Este dispositivo
open|打开工作区|Open workspace|ワークスペースを開く|작업 공간 열기|Abrir espacio
sampleDesc|咖啡订单与地区销售汇总|Coffee orders and regional sales|コーヒーの注文と地域別売上|커피 주문과 지역별 매출|Pedidos de café y ventas regionales
filterSources|筛选数据源|Filter sources|ソースを検索|소스 필터|Filtrar fuentes
noMatch|没有匹配项|No matches|該当なし|일치 항목 없음|Sin resultados
tables|表与视图|Tables and views|テーブルとビュー|테이블 및 뷰|Tablas y vistas
script|脚本|Script|スクリプト|스크립트|Script
kind|类型|Type|種類|유형|Tipo
insertName|插入名称|Insert name|名前を挿入|이름 삽입|Insertar nombre
openQuery|打开查询|Open query|クエリを開く|쿼리 열기|Abrir consulta
`.trim().split('\n')){const [k,...v]=row.split('|');window.lex[k]=v;}
