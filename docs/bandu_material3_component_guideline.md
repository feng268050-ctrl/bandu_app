# bandu_app Flutter Material 3 组件层规范

> 适用范围：`lib/components/`
> 技术基础：Flutter Material 3
> 目标：统一全项目组件视觉、交互、状态和封装边界。

---

## 1. 基本原则

组件层统一遵循：

```text
Material 3 原生组件优先
ThemeData 统一视觉
通用行为才封装
业务组件留在 Feature 内
页面不重复定义样式
```

禁止：

```text
使用 Container + GestureDetector 模拟标准按钮
页面内重复定义圆角、颜色、按钮样式
组件层依赖 Repository、Controller、Riverpod Provider
组件层直接调用 API、文件、相机或存储
把所有 Material 组件重新包装一遍
```

---

## 2. 组件分级

### Level 1：直接使用 Material 3

不进入 `components/`：

```text
Scaffold
AppBar
NavigationBar
FilledButton
OutlinedButton
TextButton
IconButton
Card
ListTile
TextFormField
DropdownMenu
SegmentedButton
FilterChip
ChoiceChip
InputChip
AlertDialog
SnackBar
MaterialBanner
Divider
CircularProgressIndicator
LinearProgressIndicator
```

### Level 2：通用组合组件

进入 `lib/components/`：

```text
AppPrimaryButton
AppSecondaryButton
AppDefaultButton
AppAsyncPrimaryButton
AppLoadingView
AppErrorView
AppEmptyView
AppConfirmDialog
AppFormSection
AppAvatar
LocalFileImage
```

### Level 3：Feature 业务组件

放在对应 Feature：

```text
ErrorItemCard
AnalyzeResultCard
PracticeQuestionCard
ProfileHeader
StatsTile
```

路径示例：

```text
lib/application/features/library/presentation/widgets/error_item_card.dart
```

---

# 3. 设计 Token

推荐：

```text
lib/components/design_system/tokens/
├── app_spacing.dart
├── app_radius.dart
├── app_sizes.dart
└── app_duration.dart
```

## 3.1 间距

```dart
abstract final class AppSpacing {
  static const double xSmall = 4;
  static const double small = 8;
  static const double medium = 12;
  static const double large = 16;
  static const double xLarge = 24;
  static const double page = 16;
}
```

## 3.2 圆角

```dart
abstract final class AppRadius {
  static const double small = 8;
  static const double medium = 12;
  static const double large = 20;
}
```

## 3.3 尺寸

```dart
abstract final class AppSizes {
  static const double minTouchTarget = 48;
  static const double iconSmall = 18;
  static const double iconMedium = 24;
  static const double avatarSmall = 32;
  static const double avatarMedium = 48;
  static const double avatarLarge = 72;
}
```

规则：

```text
颜色来自 ColorScheme
文字来自 TextTheme
间距来自 AppSpacing
圆角来自 AppRadius
禁止页面硬编码品牌色
```

---

# 4. 按钮组件

按钮分为：

```text
Primary
Secondary
Default
```

## 4.1 Primary

用于：

```text
确认
保存
提交
登录
注册
开始分析
保存错题
```

视觉：

```text
背景：colorScheme.primary
文字：colorScheme.onPrimary
```

Material 组件：

```text
FilledButton
FilledButton.icon
```

## 4.2 Secondary

用于：

```text
删除
移除
清空
退出登录
注销账号
```

视觉：

```text
背景：透明或默认
文字：colorScheme.error
图标：colorScheme.error
```

Material 组件：

```text
TextButton
OutlinedButton
```

## 4.3 Default

用于：

```text
取消
返回
重试
选择
重新选择
稍后处理
```

视觉：

```text
使用 Material 默认中性样式
```

Material 组件：

```text
OutlinedButton
TextButton
```

## 4.4 按钮顺序

```text
Default → Secondary → Primary
```

例如：

```text
取消    删除    保存
```

## 4.5 状态

所有按钮支持：

```text
Normal
Pressed
Focused
Disabled
Loading
```

禁用统一使用：

```dart
onPressed: null
```

异步主按钮必须防止重复点击。

---

# 5. 输入与表单组件

## 5.1 基础输入

统一使用：

```text
Form
TextFormField
InputDecorationTheme
```

不用：

```text
裸 TextField + 页面内手写错误文本
```

## 5.2 文本输入

```dart
TextFormField(
  decoration: const InputDecoration(
    labelText: '邮箱',
    prefixIcon: Icon(Icons.email_outlined),
  ),
  keyboardType: TextInputType.emailAddress,
  validator: validateEmail,
)
```

## 5.3 密码输入

允许封装：

```text
AppPasswordField
```

统一处理：

```text
显示/隐藏密码
password keyboard
validator
前缀和后缀图标
```

## 5.4 数值输入

使用：

```text
TextInputType.number
FilteringTextInputFormatter
```

禁止依赖字符串错误处理。

## 5.5 多行输入

用于：

```text
题目
答案
解析
用户笔记
```

统一：

```dart
TextFormField(
  minLines: 3,
  maxLines: 8,
)
```

## 5.6 表单分区

跨多个表单页复用时允许封装：

```text
AppFormSection
```

组成：

```text
标题
说明
字段列表
分隔
```

---

# 6. 选择器组件

## 6.1 少量固定选项

使用：

```text
SegmentedButton
```

适用：

```text
登录 / 注册
列表 / 网格
简单状态切换
```

## 6.2 单选列表

使用：

```text
RadioListTile
```

适用：

```text
学段
年级
难度
```

## 6.3 下拉选择

使用：

```text
DropdownMenu
```

适用：

```text
科目
年级
学期
排序方式
```

## 6.4 开关

使用：

```text
SwitchListTile
```

适用：

```text
自动保存
显示答案
启用本地缓存
```

## 6.5 标签

```text
FilterChip
ChoiceChip
InputChip
```

规则：

```text
FilterChip：多选筛选
ChoiceChip：单选
InputChip：已选择且可删除
```

---

# 7. 卡片与容器

## 7.1 默认卡片

优先：

```text
Card.filled
```

适用：

```text
分析结果
统计摘要
个人资料摘要
普通内容块
```

## 7.2 带边界卡片

使用：

```text
Card.outlined
```

适用：

```text
图片选择区域
可点击入口
需要明显边界的内容
```

## 7.3 业务卡片

Feature 内定义：

```text
ErrorItemCard
AnalyzeResultCard
PracticeQuestionCard
```

业务卡片可以组合：

```text
Card
ListTile
Chip
IconButton
```

但不能调用 Repository 或导航状态。

## 7.4 禁止

```text
ListTile.tileColor + shape 模拟 Card
页面内大量 DecoratedBox 复制相同样式
Container 直接写固定颜色和圆角
```

---

# 8. 列表组件

## 8.1 标准列表

统一：

```text
ListView
ListTile
Divider
```

## 8.2 可点击列表项

```dart
ListTile(
  leading: const Icon(Icons.book_outlined),
  title: const Text('错题本'),
  subtitle: const Text('查看全部错题'),
  trailing: const Icon(Icons.chevron_right),
  onTap: onTap,
)
```

## 8.3 设置项

```text
ListTile
SwitchListTile
RadioListTile
```

## 8.4 滑动操作

危险操作优先：

```text
Dismissible + 二次确认
```

但不建议第一版大量使用隐藏手势。

---

# 9. 导航组件

## 9.1 主导航

统一：

```text
NavigationBar
NavigationDestination
```

当前主导航：

```text
首页
错题本
拍题
练习
我的
```

## 9.2 页面顶部

统一：

```text
AppBar
SliverAppBar
```

## 9.3 返回

使用系统默认返回按钮，不自定义相同图标。

## 9.4 页面级主要动作

优先：

```text
AppBar actions
FloatingActionButton
底部 Primary 按钮
```

不要一个页面同时出现多个等权重 Primary 操作。

---

# 10. 反馈组件

推荐：

```text
lib/components/feedback/
├── app_loading_view.dart
├── app_error_view.dart
├── app_empty_view.dart
├── app_progress_view.dart
└── app_snackbars.dart
```

## 10.1 加载

页面加载：

```text
AppLoadingView
```

局部加载：

```text
CircularProgressIndicator
```

上传进度：

```text
LinearProgressIndicator
```

## 10.2 错误

页面级错误：

```text
AppErrorView
```

内容：

```text
错误图标
错误说明
重试按钮
```

短暂错误：

```text
SnackBar
```

持续提示：

```text
MaterialBanner
```

## 10.3 空状态

```text
AppEmptyView
```

内容：

```text
Material 图标
标题
简短说明
可选 Primary / Default 操作
```

## 10.4 成功提示

短暂成功：

```text
SnackBar
```

例如：

```text
保存成功
删除成功
资料已更新
```

## 10.5 组件层不得依赖 Riverpod

禁止：

```text
AppAsyncView<T> 直接接收 AsyncValue<T>
```

Feature 内自行适配：

```dart
value.when(
  loading: () => const AppLoadingView(),
  error: (error, _) => AppErrorView(
    message: error.toString(),
    onRetry: retry,
  ),
  data: buildContent,
)
```

---

# 11. 对话框与底部面板

## 11.1 简单确认

使用：

```text
AlertDialog
```

适用：

```text
删除确认
退出登录确认
取消编辑确认
```

## 11.2 多字段编辑

优先：

```text
独立页面
```

次选：

```text
showModalBottomSheet(
  isScrollControlled: true,
)
```

不建议：

```text
在 AlertDialog 内塞入复杂长表单
```

## 11.3 确认按钮规则

```text
取消：Default
删除：Secondary
保存：Primary
```

## 11.4 通用封装

可封装：

```text
AppConfirmDialog
showAppConfirmDialog()
```

仅统一：

```text
标题
说明
按钮顺序
危险色
返回结果
```

---

# 12. 媒体组件

推荐：

```text
lib/components/media/
├── app_avatar.dart
├── local_file_image.dart
├── app_network_image.dart
└── image_placeholder.dart
```

## 12.1 头像

统一：

```text
CircleAvatar
```

允许 `AppAvatar` 处理：

```text
本地路径
网络 URL
默认头像
不同尺寸
加载失败
```

## 12.2 本地图片

`LocalFileImage` 只负责：

```text
读取本地文件
fit
clip
错误占位
```

不负责：

```text
选择图片
删除文件
上传图片
```

## 12.3 图片选择区域

Feature 内使用：

```text
Card.outlined + InkWell + AspectRatio
```

不需要放入全局组件，除非多个 Feature 复用。

---

# 13. 图标规范

统一使用 Material Symbols / Flutter Icons。

常见操作：

| 操作 | 图标 |
|---|---|
| 确认 | `Icons.check` |
| 保存 | `Icons.save_outlined` |
| 提交 | `Icons.send_outlined` |
| 删除 | `Icons.delete_outline` |
| 编辑 | `Icons.edit_outlined` |
| 重试 | `Icons.refresh` |
| 拍照 | `Icons.camera_alt_outlined` |
| 相册 | `Icons.photo_library_outlined` |
| 搜索 | `Icons.search` |
| 筛选 | `Icons.filter_list` |
| 返回 | 系统默认 |
| 进入详情 | `Icons.chevron_right` |

规则：

```text
同一操作使用同一图标
危险图标使用 error 色
图标不能替代关键按钮文字
```

---

# 14. 文字规范

统一使用：

```text
Theme.of(context).textTheme
```

推荐语义：

```text
display / headline：页面核心标题
titleLarge：页面标题
titleMedium：区块标题
bodyLarge：主要正文
bodyMedium：普通正文
bodySmall：辅助说明
labelLarge：按钮文字
```

禁止页面手写大量：

```dart
TextStyle(
  fontSize: ...,
  fontWeight: ...,
  color: ...,
)
```

特殊业务强调除外。

---

# 15. 布局规范

## 15.1 页面边距

```text
默认水平边距：16
大区块间距：24
普通区块间距：16
控件间距：8 或 12
```

## 15.2 触控区域

所有交互区域至少：

```text
48 × 48
```

## 15.3 页面滚动

表单和详情页：

```text
SafeArea
SingleChildScrollView
Padding
```

列表页：

```text
ListView
CustomScrollView
```

## 15.4 键盘

表单页确保：

```text
resizeToAvoidBottomInset
滚动可见
提交时隐藏键盘
```

---

# 16. ThemeData 统一范围

`AppTheme` 至少统一：

```text
ColorScheme
TextTheme
AppBarTheme
NavigationBarTheme
CardTheme
InputDecorationTheme
FilledButtonTheme
OutlinedButtonTheme
TextButtonTheme
IconButtonTheme
ListTileTheme
DialogTheme
BottomSheetTheme
SnackBarTheme
ChipTheme
ProgressIndicatorTheme
DividerTheme
```

页面不应重复设置这些全局样式。

---

# 17. 目标组件目录

```text
lib/components/
├── design_system/
│   ├── theme/
│   │   ├── app_theme.dart
│   │   ├── app_component_themes.dart
│   │   └── app_color_scheme.dart
│   └── tokens/
│       ├── app_spacing.dart
│       ├── app_radius.dart
│       ├── app_sizes.dart
│       └── app_duration.dart
│
├── actions/
│   ├── app_primary_button.dart
│   ├── app_secondary_button.dart
│   ├── app_default_button.dart
│   └── app_async_primary_button.dart
│
├── feedback/
│   ├── app_loading_view.dart
│   ├── app_error_view.dart
│   ├── app_empty_view.dart
│   └── app_snackbars.dart
│
├── forms/
│   ├── app_password_field.dart
│   └── app_form_section.dart
│
├── dialogs/
│   └── app_confirm_dialog.dart
│
└── media/
    ├── app_avatar.dart
    └── local_file_image.dart
```

不要一次创建全部空文件。按实际重构逐步增加。

---

# 18. Feature 组件目录示例

```text
application/features/library/presentation/
├── library_page.dart
├── error_item_detail_page.dart
└── widgets/
    ├── error_item_card.dart
    ├── error_item_filter_bar.dart
    └── mastery_level_chip.dart
```

```text
application/features/capture/presentation/
├── capture_page.dart
└── widgets/
    ├── capture_image_selector.dart
    ├── analyze_result_card.dart
    └── capture_action_bar.dart
```

规则：

```text
通用视觉行为 → components
具体业务语义 → Feature widgets
```

---

# 19. 页面改造优先级

## P0

```text
AppTheme
按钮三级规范
AppLoadingView
AppErrorView
AppEmptyView
登录表单
拍题操作区
错题列表
删除确认
```

## P1

```text
Profile 页面拆分
统计卡片
练习页面
统一 SnackBar
统一 BottomSheet
统一选择器
```

## P2

```text
复杂标签组件
图表组件
高级筛选
动画和过渡
```

---

# 20. 架构约束

建议增加测试：

```text
test/architecture/material_component_rules_test.dart
```

检查：

```text
components 不导入 Riverpod
components 不导入 Feature
components 不导入 Framework
components 不调用 Repository
Feature 页面禁止定义 ThemeData
Feature 页面禁止硬编码品牌色
Feature 页面禁止自定义普通按钮容器
```

允许：

```text
Feature widgets 导入 components
Feature widgets 接收业务模型
Feature widgets 触发 callback
```

---

# 21. 验收标准

组件层规范完成后必须满足：

```text
1. 全项目统一使用 Flutter Material 3。

2. 标准组件优先直接使用 Material 原生实现。

3. 按钮统一分为 Primary、Secondary、Default。

4. 全局样式由 ThemeData 管理。

5. 页面不硬编码品牌颜色和通用圆角。

6. 通用加载、错误、空状态只有一套实现。

7. 组件层不依赖 Riverpod、Repository 和 Framework。

8. 业务组件保留在 Feature 内。

9. 删除和危险操作统一二次确认。

10. 表单统一使用 Form + TextFormField。

11. 列表统一使用 ListTile / Card。

12. 导航统一使用 NavigationBar / AppBar。

13. flutter analyze 通过。

14. flutter test 通过。

15. Android 实机界面和交互正常。
```

---

# 22. 最终摘要

```text
按钮
→ Primary / Secondary / Default

表单
→ Form + TextFormField

选择
→ SegmentedButton / DropdownMenu / Chip

卡片
→ Card.filled / Card.outlined

列表
→ ListTile

导航
→ NavigationBar / AppBar

反馈
→ Loading / Error / Empty / SnackBar

确认
→ AlertDialog

复杂编辑
→ 页面或 ModalBottomSheet

媒体
→ AppAvatar / LocalFileImage

样式
→ ThemeData + Design Tokens
```

组件层只负责统一视觉和通用交互，不承担业务流程。
