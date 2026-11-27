# Android 验收记录

## 0.1.0 基线 · 2026-11-27

- 在专用 Android 11 模拟设备安装正式签名 APK。使用 FieldSQL 界面连接 PostgreSQL 17.10、MariaDB 10.11.5 和 Oracle MySQL 8.4.7；从各服务器读取实际版本和测试表数据，在各自的对象浏览器读回主键。三个数据库使用全新的临时实例，不是用户数据库。
- 从 Android 文件选择器导入 RSA 私钥，给出预先核对的跳板机指纹，经过 SSH 隧道连接 PostgreSQL 并读取测试行。这次设备测试的数据库连接未启用额外的 TLS。
- 从系统文件选择器导入 1,117,320,768 字节的 Qwen2.5-Coder-1.5B-Instruct Q4_K_M。端内引擎读取示例库结构，按中文请求生成按地区分组、按总额降序的 SQL。将它放入编辑器时结果为空；点执行才得到杭州 800、上海 792、成都 536。该模拟器使用 ARM64 翻译层，耗时不作为物理手机性能数据。
- 在 SQLite 工作区执行真实查询，查看写入确认、DDL 和对象树；通过系统文件选择器把结果保存为 XLSX，在主机读回导出的工作簿。最终发行包导出的三个汇总单元格分别为 800、792、536，均为 Excel 原生数值（n），不是看似数字的文本。

Nexus 10 Android 虚拟平板在 800dp 竖屏与 1280dp 横屏回读了完整的查询和对象界面。1280×800dp 的原生 AI 页实际呈现左侧输入、右侧回答两个独立区域。Android 系统分屏给 FieldSQL 分配 432dp（864px @ 320dpi）时，对象浏览转为单栏；实际从列表进入 orders 并出现返回入口，另一半是系统设置。[平板查询界面](docs/screenshots/tablet-workbench.png)来自 Android 模拟设备截图。

## 驱动与界面分别验证

- 驱动集成：三种服务都测试过普通 TCP、验证证书的 TLS、SSH + 验证证书的 TLS；错误证书主机名或错误 SSH 指纹被拒绝。写入、复合主键编辑、回滚、分页与长查询取消均用真实数据库测试。详见 [数据库验证](db/checks/RESULTS.md)。没有把主机侧的 TLS 验证写成设备上的 TLS 验证。
- 浏览器 UI：用真实 SQLite 驱动生产版 Explorer，覆盖复合主键增改删、取消不写入、无键只读、分页、列与索引、CSV/XLSX 回读；UI 壳还验证了 CodeMirror 编辑、执行和标签结果保持。可复运行 `checks/explorer/check.py` 与 `checks/studio.py`。
- 开发电脑、Android 模拟器和实物设备是不同的验证层。本次没有用户实物手机/平板上的速度、温升和续航数据，也没有使用用户的指纹或人脸登记。

## 安装包

发行版使用独立的 Android Release 签名。最低 API 26，目标 API 35；端侧模型引擎需要 API 28+。从 [GitHub Releases](https://github.com/Garry-Tang-274/FieldSQL-Android/releases) 下载 APK，模型权重在应用内按需下载。
