# FieldSQL Studio

FieldSQL 采用暗色优先的数据工作台：连接页回答“打开哪个数据库”，查询页回答“在哪个库、写了什么、返回了什么”。平板增加信息并排显示的空间，不把手机按钮按比例放大。

## 取自哪些开源软件

- [Supabase Studio](https://github.com/supabase/supabase/tree/master/apps/studio) 的[实际 SQL 编辑器](https://supabase.com/features/sql-editor)：采用窄导航、独立的对象区、脚本与结果上下排列，压低工具栏的视觉权重。不复制 Supabase 的标志、截图、绿色品牌或服务端。
- [Radix Colors](https://github.com/radix-ui/colors)（MIT）：随 APK 打包 Slate / Iris 两套明暗色阶。深色界面以石墨画布、接近画布的面板及 Iris 11 主操作建立层级；亮色用深紫保持对比。绿色仅用作已连接状态。
- [IBM Carbon Icons](https://github.com/carbon-design-system/carbon/tree/main/packages/icons)（Apache 2.0）、[IBM Plex](https://github.com/IBM/plex)（OFL）：离线 SVG 图标和实际字体。保留图标与字型，不采用 Carbon 的整套企业外壳。
- [CodeMirror](https://codemirror.net/)（MIT）：SQL 编辑器的行号、高亮、选择、补全、撤销和外接键盘操作都来自实际代码编辑控件。

## 空间如何分配

| 可用窗口 | 查询、浏览与 AI |
|---|---|
| 手机和窄分屏 | 底部导航，编辑器和结果上下排。对象列表点入详情，可返回列表。 |
| 中等窗口 | 导航移到侧边；按实际剩余宽度决定是否并排，而非按设备型号或仅按横竖屏决定。 |
| 宽平板 | 左侧导航；查询页额外固定数据库结构；主区保留编辑器与结果。对象浏览在其自身宽度达到 600px 时分栏。AI 在窗口达到 720dp 时把任务输入和回答并排。 |

正常文本和结果优先阅读，只有代码、列名和耗时使用等宽。主按钮只留给当前操作；执行状态和错误贴在对应结果旁。模型、账号凭据、服务器操作和查询结果都不经过外部 UI 服务。
