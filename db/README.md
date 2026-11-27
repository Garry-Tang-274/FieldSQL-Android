# FieldSQL 数据库模块

在 Android 的后台串行线程中打开一个连接，执行一条 SQL，然后用结果的列名、行和更新计数渲染界面：

```java
try (Database db = Databases.openRemote("postgres","db.example.org",5432,
        "sales","reader",password,true)) {
    db.setQueryTimeoutSeconds(30);
    Database.Result result = db.execute("SELECT order_id, total FROM orders",50);
    // result.rows 只含实际显示的行。truncated=true 表示至少还有一行。
}
```

- 远程支持 PostgreSQL、MySQL 与 MariaDB；仅连接用户指定的主机和数据库。TLS 开关默认为开时，信任 Android 证书库并验证数据库主机名；不会自动降级到明文。对使用私有 CA 的服务器，须保证该 CA 受应用的 Android 网络安全配置认可。
- SSH 私钥通过 Android 文件选择器传入，另需可信来源给出的 OpenSSH `SHA256:…` 主机公钥指纹。私钥、私钥口令和数据库密码都不由本模块持久化。隧道内的 TLS 检查同样针对数据库的真实域名，而非 127.0.0.1。JSch 会在连接建立后关闭本机临时转发监听，保持已建立的通道。
- `setQueryTimeoutSeconds(1..3600)` 只用于远端，默认 60 秒；PostgreSQL 用服务端 statement_timeout。MySQL/MariaDB 超时由另一个相同身份、相同 TLS/SSH 路径的连接请求取消这条查询。对耗时写操作，客户端超时后应核对实际提交结果，再决定是否重试。
- SQLite 库以名字存放于应用私有目录。导入只读原 URI，创建私有副本；同名拒绝覆盖。导入前应从源应用导出已完成 checkpoint 的单文件快照，而不是从运行中的 WAL 库只复制主文件。卸载应用时，系统会清除私有工作副本。
- `tables/describe` 返回 SQLite 主库、PostgreSQL 当前 schema 或 MySQL/MariaDB 当前 database 的表/视图、列、主键与索引。PostgreSQL 描述查询使用 PostgreSQL 11+ 的 pg_catalog 字段。
- 一次运行一条 SQL。行数可设为 1–10000。结构化单行修改要求完整主键，值绑定、标识符按数据库方言引用。无主键的对象可在 SQL 编辑器中手动修改。SELECT 保留每个原始显示字符串，并用与行对齐的 cellTypes（N=数值、B=布尔、S=其他）协助类型化导出；导出程序应保留超过电子表格数值精度的文本。SQL NULL 保留为 Java null；二进制值明确显示为 `⟪BLOB:…⟫` 预览，不作可编辑主键或无损导出值。超长单元格会提示用 substr/指定列，不会冒充完整数据。
- 一个连接在其后台串行线程中使用。事务控制每次传一条 `BEGIN`、DML、`ROLLBACK` 或 `COMMIT`；特别是 SQLite，必须在同一后台线程上完成。客户端不将多个语句静默拆开或自动提交。

## 驱动版本与许可

| 依赖 | 许可和对应源码 |
|---|---|
| PostgreSQL JDBC 42.7.15 | [BSD-2-Clause / 源码](https://github.com/pgjdbc/pgjdbc/tree/REL42.7.15) |
| MariaDB Connector/J 2.7.15 | [LGPL-2.1-or-later / 源码](https://github.com/mariadb-corporation/mariadb-connector-j/tree/2.7.15) |
| JSch 2.28.8（mwiede）| [BSD / 源码](https://github.com/mwiede/jsch/tree/jsch-2.28.8) |
| Bouncy Castle bcprov-jdk15to18 1.87 | [Bouncy Castle License / 源码](https://github.com/bcgit/bc-java) |

许可证随模块一起进入 APK 的 `assets/licenses/`。构建脚本用 Maven 坐标引用未修改的驱动。需要更换或重构驱动时，从对应源码生成本地 Maven 发布物，调整 `db/build.gradle` 中的依赖后重新构建应用。

## 真实服务集成检查

Windows 需要 Java 17+、Python 3.12+、paramiko、cryptography。运行 `checks/prepare-fixtures.py` 会在忽略版本控制的 `db/build/probe` 中下载并解压临时服务及测试依赖（约 310 MB 压缩数据，解包后更多）；随后运行 `checks/fixtures.py db/build/probe`。两个命令均以项目根目录为当前目录，脚本路径前加 `db/`。

检查会创建全新的回环 PostgreSQL 17.10、Oracle MySQL 8.4.7 和 MariaDB 10.11.5，测试跨三引擎的读写、元数据、超时、TLS、SSH+TLS、主机认证失败；测试脚本会向专用实例请求正常关机，再清理本次临时目录。[集成检查结果](checks/RESULTS.md) 分开记录了桌面驱动的 TLS、SSH 与查询超时，以及已签名 Android APK 界面实际连接三种服务器、经 SSH 回读 PostgreSQL 的验证。



