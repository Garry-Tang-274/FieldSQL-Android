# FieldSQL · Android 数据库工作台

在 Android 手机或平板上浏览 PostgreSQL、MySQL、MariaDB 和 SQLite，也可以让设备本地的小模型根据表结构写 SQL。无需 FieldSQL 账户。

**[下载 Android 安装包（ARM64）](https://github.com/Garry-Tang-274/FieldSQL-Android/releases/download/v0.1.1/FieldSQL-0.1.1-arm64.apk)** · [所有版本](https://github.com/Garry-Tang-274/FieldSQL-Android/releases)

第一次打开可以选 **Field Notes**；平板再点右侧 **打开工作区**。执行预置查询，会从本机的 SQLite 示例库读出杭州 800、上海 792、成都 536。

![平板横屏：左侧浏览表结构，右侧编辑 SQL 并查看结果](docs/screenshots/tablet-workbench.png)

## 让 AI 在手机上写一条 SQL

1. 在 **查询 → 端侧 AI** 点 **[下载推荐模型](docs/screenshots/download-model.png)**。也可以从 **设置 → 端侧 AI** 进入，无须先连接数据库。
2. 推荐的 Qwen2.5-Coder 1.5B 下载量为 **1.12 GB**，建议使用 Wi-Fi，并至少留出模型大小之外的可用空间。模型从 Qwen 的官方 Hugging Face 仓库下载到这台设备，不用在电脑上另下或传文件。下载完成后应用选用该模型；已有 GGUF 的用户仍可手动导入。
3. 在 Field Notes 中输入 **“按地区汇总订单销售额，按总额从高到低排序”**。先看生成的 SQL 是否使用 `GROUP BY region` 和降序排序，再点 **放入编辑器 → 执行 SQL**。推理时不需要联网、API Key 或电脑服务。

APK 面向 **64 位 ARM Android 8.0+**，本地 AI 需要 **Android 9.0+** 和足够的可用内存。使用 CPU 推理；1.5B 是推荐模型，空间紧张可选 0.5B。数据库连接可访问远程服务器，只有模型推理和 SQLite 示例不依赖外网。手机的系统 WebView 应保持更新。

## 连接自己的数据库

**数据源 → 新建连接**，选择 PostgreSQL、MySQL 或 MariaDB，填写服务器、端口、库名和数据库账户。默认要求 TLS 并验证数据库主机名。需要跳板机时打开 **SSH 私钥隧道**，选择私钥，填写管理员提供的 SSH 主机指纹。SSH 指纹与数据库 TLS 证书是两项不同的身份检查；不要因为证书错误就关闭校验。连接配置及密码以 Android Keystore 保护的密钥加密保存在本机。

打开连接后，顶部连接名可切库、建库、刷新结构或断开。**对象**页查看表、视图、函数、过程，以及列、索引和建表语句。修改已有行须有准确主键；无主键对象仅供浏览。显示上限控制一次返回的行数，不限制行编辑次数。导出的是当前查询结果或当前数据页，可以选择保存 CSV/XLSX 或交给 Android 分享。

**查询**页支持多标签、语法高亮、表列补全和外接键盘 Ctrl/Cmd+Enter；**查询库**保留收藏和最近 500 次执行。写入与改表操作会显示 SQL 确认。平板宽窗把表结构放在编辑器旁边；点列名即可插入当前光标位置。缩成窄分屏后，编辑器优先占用可用宽度。

## 对照 MonoDB

参考产品是 [Wovex MonoDB 的 Android 商店页](https://play.google.com/store/apps/details?id=com.wovex.mono_db) 及[开发者隐私说明](https://wovexapp.com/mono_db/privacy-policy)。FieldSQL 是独立应用，包名 `dev.fieldsql.app`，可与原版并存。

| 公开功能 | 本版实现 |
|---|---|
| Pro 五项 | SSH 私钥隧道、连接配置和 SQL 标签不设数量配额、行编辑不计次、无广告。无会员账户或购买入口。 |
| 对象与编辑 | 表/视图/过程/函数（按引擎）；数据、列、索引、DDL；主键行增改删、外键提示、增列、MySQL/MariaDB 支持的列调序。 |
| 查询与导出 | 多标签、表列补全、危险语句确认、20/50/100/500 或 1–10000 自定返回行数，收藏、500 条历史、CSV 及原生 XLSX。 |
| 切库与连接 | MySQL/MariaDB 使用 USE，PostgreSQL 重连目标库；连接超时设置中的查询时限按每个远程配置保存。 |
| 本机 AI | GGUF 导入、Qwen 下载、多模型切换/删除、中文请求生成 SQL、解释与修正、当前会话记录。SQL 只回填为草稿。 |
| 移动系统 | 深浅色与跟随系统、中英日韩西主要界面、平板/分屏布局、生物识别锁、后台连接通知、列出已保存连接的桌面小组件。 |

**与原版的具体差别：** 本地模型路线是 llama.cpp + GGUF，而不是复用 Wovex 的模型 CDN。PostgreSQL 表 DDL 是依据列与约束重建的结构预览，索引另页展示，不是完整的 `pg_dump`。不把原版付费、广告或分析服务集成进应用。兼容性按上面的系统版本和架构判断；真机推理速度要看芯片和内存。系统文件选择器及数据库服务器本身返回的提示使用各自的语言。

## 源码与构建

原生壳、连接与 Android 文件/密钥交互在 `app/`；驱动与 SSH 在 `db/`；端内推理与模型管理在 `ai/`。前端全部随包离线交付。设计来源见 [DESIGN.md](DESIGN.md)，实测范围见 [VERIFICATION.md](VERIFICATION.md)。第三方许可放在模块的 `assets/licenses` 和 `ai-fonts` 内；模型权重不随 APK 或 Git 仓库分发，[推荐模型的来源及许可](ai/models/NOTICE.txt)单独列出。

- JDK 17/21、Android SDK 35、Build Tools 35.0.0、随项目的 Gradle Wrapper。Android Studio 可直接打开根目录。
- Windows 本机签名构建：`./build-local.ps1`（设置 `ANDROID_HOME` 或传 `-Sdk`）。脚本使用当前 Windows 用户独立的签名密钥，不随源码包交付。另一台电脑重新签名的 APK 不能覆盖安装本发行包。
- 单纯重编 APK 不需要 Node。改动编辑器或设计依赖时，在 `ui-kit/` 执行 `npm ci` 与 `npm run build` 后再构建。普通调试构建可直接执行 `gradlew.bat :app:assembleDebug`。
- MariaDB Connector/J 的对应源码随 `third-party/` 提供，可替换依赖后重新构建。开源 UI 依赖版本固定在 `ui-kit/package-lock.json`。
