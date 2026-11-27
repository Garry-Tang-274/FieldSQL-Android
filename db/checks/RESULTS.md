# 数据库驱动联测 · 2026-11-27

Windows OpenJDK 21.0.13.1 实际调用 APK 数据库模块的同一份 Java 源码；后台是真实的 PostgreSQL 17.10、MariaDB 10.11.5 与 Oracle MySQL Community Server 8.4.7。SSH 服务采用一次性 RSA 公钥与固定 SHA256 指纹；数据库 TLS 证书只对 localhost 有效，由临时 CA 签发。每次运行都使用独立新建的数据库。

三种数据库在显式明文 TCP、校验证书的 TLS、固定主机公钥的 SSH + 校验数据库证书这三种路径均完成以下回读：创建含复合主键的表、插入多行与 NULL、查询索引列、分页截断、以完整主键修改带英文引号的文本、拒绝不完整主键、事务回滚后恢复原值。后端服务器错误域名（127.0.0.1 而非 localhost）在直连及隧道两种情况下都被拒绝；错误的 SSH 主机公钥指纹也被拒绝。

将查询限时设为 1 秒后，在服务端执行原本需 4 秒的 sleep，记录从发起查询到返回的实际用时。每次随后的 SELECT 9 均成功。

| 服务端 | 明文 TCP | 直接 TLS | SSH + TLS |
|---|---:|---:|---:|
| PostgreSQL 17.10 | 1010 ms | 1018 ms | 1006 ms |
| MariaDB 10.11.5 | 1005 ms | 1026 ms | 1031 ms |
| MySQL 8.4.7 | 1011 ms | 1022 ms | 1021 ms |

另外，用禁用多版本 JAR 的 Java 8 兼容路径，以 Bouncy Castle 完成 Ed25519 主机指纹验证和带口令的 OpenSSH Ed25519 私钥登录，再通过隧道对 PostgreSQL 使用 TLS 并回读已知记录。脚本为 `ed25519.py` 与 `Ed25519Smoke.java`。

可复运行入口：`prepare-fixtures.py`、`fixtures.py`、`RemoteSmoke.java`。这些时延来自桌面 JVM，不衡量手机性能。

## Android APK 界面与协议回读

在 Android API 30 模拟设备的已签名 Release APK 中，由界面分别打开 PostgreSQL 17.10、MariaDB 10.11.5 和 Oracle MySQL Community Server 8.4.7 的独立真实服务端。三端的 SQL 编辑器返回真实版本及专用 qa_catalog 内容，同时对象页展示对应表和主键。这三组直连手工关闭了数据库 TLS（隔离测试服务器的临时 CA 不属于 Android 系统信任库）。

同一 Android 测试环境通过系统文件选择器导入 2048 位 RSA PEM 私钥，并锁定 OpenSSH SHA-256 主机公钥指纹，建立了通往 PostgreSQL 17.10 的 SSH 隧道，随后回读版本和 qa_catalog。该项的数据库 TLS 已关闭；三引擎 SSH 同时验证数据库 TLS 及错误主机名拦截的证据来自上面的桌面驱动联测。

现场发现旧 MariaDB 驱动注册入口要求 Android 没有的 java.sql.DriverAction。新版改为使用该驱动本身的公开连接工厂后，MariaDB 与 MySQL 都通过了 Android APK 实测；没有以桌面通过代替修复。

## 每连接私有 CA · 0.1.2

在新的临时 PostgreSQL 17.10、MariaDB 10.11.5 和 Oracle MySQL 8.4.7 上，服务端发送叶证书及中间证书，客户端配置独立私有根。三引擎均在直连 TLS 和 SSH + TLS 下成功读取已知行。对同一组运行中的服务器，错误根、系统默认不信任该根、以及正确根配错主机名都被拒绝；同时打开的正确连接仍可查询。第二个测试 JVM 把正确根明确加入其系统信任：旧接口正常连接，但显式提供错误根的连接仍然拒绝。MySQL/MariaDB 的超时控制连接同样使用该配置；直接或经 SSH 取消慢查询后，原连接均可继续查询。

导入检查覆盖单 DER、单/多 PEM、带 BOM 或 CRLF 的 PEM，以及私钥、证书混入私钥、非 CA 叶证书、尾随字节和超限文件的拒绝。上述为真实服务器的桌面驱动层验证。复运行：在专用 `fixtures.py --hold` 启动的新实例上，执行 `check-private-ca.py <本次 ui.json>`。

在最终 Android Release APK 中，通过系统文件选择器导入专用 CA 后，以受信任 IP 连接 Oracle MySQL 8.4.7，实际读取了 `qa_catalog.id=7`。选择无关 CA 或移除所需 CA 时连接失败；通过端口映射改用不在证书 SAN 中的 IP 连接同一实例时，得到主机名不匹配的提示。改回证书内的 IP 后再次读取同一行。测试没有使用用户的云数据库。

`check-hostnames.py` 使用真实 X.509 叶证书检查了仅 CN 正确但缺少 SAN、CN 正确但 SAN 错误、用 DNS SAN 冒充 IP 的拒绝；IPv6 等价地址和只跨一个标签的 DNS 通配符匹配正确。该版不调用驱动的桌面 JNDI/CN 后备路径。三台专用数据库及 SSH 服务均已关停，本次临时目录已清理。


