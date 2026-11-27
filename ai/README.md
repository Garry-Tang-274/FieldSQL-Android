# 在 FieldSQL 下载离线 SQL 模型

进入 **查询 → 端侧 AI**（或 **设置 → 端侧 AI**），点击 **下载推荐模型 · 1.12 GB**。应用会显示下载进度和总量。完成后自动校验、选用模型；可直接输入问题，无需打开浏览器、手动找文件或配置 API 密钥。

推荐的 **Qwen2.5 Coder 1.5B Instruct（Q4_K_M）** 需要约 1.12 GB 存储。下载由 Android 管理，离开助手或锁屏可继续。网络中断时，系统会根据网络和服务器条件续传或重试；如果系统要求 Wi-Fi，页面会标明。重新打开助手即可查看状态。最终失败可点 **重试下载** 发起新传输；**取消下载** 则移除未完成的文件。回到助手后，完成的文件按发布方的 SHA-256 校验，就地转为模型，不再另复制一份。

模型就绪后，断网也能写 SQL、解释和纠错。**放入编辑器** 只填入草稿；核对表名、列名、筛选及分组后再执行。**本次对话** 可回看及追问，退出后不保留聊天记录。窗口宽度达到 720 dp 时，输入和输出分列显示，较窄窗口使用单栏。

## 模型和设备

- Android 9 及以上的 64 位 ARM 设备。CPU 推理需要的可用内存高于文件大小。
- 内存较少时，可在 **管理模型 → 下载模型** 选择 491 MB 的 0.5B 版本。轻量模型在中文分组查询中曾漏掉聚合条件，优先使用 1.5B。
- 已有兼容模型时，**管理模型 → 从文件导入 GGUF** 可读取单文件指令型 GGUF。引擎读取模型自带的对话模板；Ollama 目录、分片和 safetensors 不能直接导入。
- 下载只从固定版本的官方 Qwen 仓库取模型；不发送 SQL、表结构、查询结果或口令。推理子进程直接读取设备上的权重，不访问网络。

| 模型来源 | 文件大小 |
|---|---:|
| [Qwen2.5-Coder-1.5B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/tree/f86cb2c1fa58255f8052cc32aeede1b7482d4361)，Q4_K_M，Apache 2.0 | 1,117,320,768 字节 |
| [Qwen2.5-Coder-0.5B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen2.5-Coder-0.5B-Instruct-GGUF/tree/ebb2015119c907b064c512bf053e945850b5875f)，Q4_K_M，Apache 2.0 | 491,400,064 字节 |

## Android 集成

模块 `:ai` 基于 [llama.cpp b12774](https://github.com/ggml-org/llama.cpp/releases/tag/b12774)。原生引擎随 APK 安装，权重独立下载。应用包须设置 `android.packaging.jniLibs.useLegacyPackaging=true`，让系统将 ARM64 ELF 提取到 `nativeLibraryDir`。保留原生库文件名。CPU 上下文 3072 tokens，单次最多生成 384 tokens。

显式启动 `dev.fieldsql.ai.AssistantActivity`。传入 `dark`（boolean）、`lang`（zh/en/ja/ko/es），以及字符串 `dialect`、`schema`、`sql`、`error`、`mode`（generate/explain/fix）。用户选择回填时，`RESULT_OK` 的 `sql` 是待编辑的草稿。解释模式不回填。
