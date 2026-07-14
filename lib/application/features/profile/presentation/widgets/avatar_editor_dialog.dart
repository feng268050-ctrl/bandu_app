import 'package:bandu_wrong_notebook/application/features/profile/domain/avatar_settings.dart';
import 'package:bandu_wrong_notebook/components/actions/app_async_primary_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_sizes.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/media/app_avatar.dart';
import 'package:flutter/material.dart';

Future<void> showAvatarEditorDialog({
  required BuildContext context,
  required AvatarSettings current,
  required String initial,
  required Future<String?> Function() onPickImage,
  required Future<void> Function({required int colorValue, String? imagePath})
      onSave,
}) {
  return showDialog<void>(
    context: context,
    builder: (context) => _AvatarEditorDialog(
      current: current,
      initial: initial,
      onPickImage: onPickImage,
      onSave: onSave,
    ),
  );
}

class _AvatarEditorDialog extends StatefulWidget {
  const _AvatarEditorDialog({
    required this.current,
    required this.initial,
    required this.onPickImage,
    required this.onSave,
  });

  final AvatarSettings current;
  final String initial;
  final Future<String?> Function() onPickImage;
  final Future<void> Function({required int colorValue, String? imagePath})
      onSave;

  @override
  State<_AvatarEditorDialog> createState() => _AvatarEditorDialogState();
}

class _AvatarEditorDialogState extends State<_AvatarEditorDialog> {
  static const _colors = [
    Color(0xff202124),
    Color(0xff455a64),
    Color(0xff00695c),
    Color(0xff00897b),
    Color(0xff0277bd),
    Color(0xff1565c0),
    Color(0xff5e35b1),
    Color(0xff8e24aa),
    Color(0xffd81b60),
    Color(0xffc62828),
    Color(0xffe65100),
  ];

  late Color _draftColor;
  late String? _draftImagePath;
  bool _saving = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _draftColor = Color(widget.current.colorValue);
    _draftImagePath = widget.current.imagePath;
  }

  @override
  Widget build(BuildContext context) {
    final hasImage = _draftImagePath != null;
    final colorScheme = Theme.of(context).colorScheme;
    return AlertDialog(
      title: const Text('头像'),
      content: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 360),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            AppAvatar(
              initial: widget.initial,
              size: 96,
              backgroundColor: _draftColor,
              imagePath: _draftImagePath,
            ),
            const SizedBox(height: AppSpacing.xLarge),
            SizedBox(
              width: AppSizes.minTouchTarget * 4 + AppSpacing.medium * 3,
              child: Wrap(
                alignment: WrapAlignment.center,
                spacing: AppSpacing.medium,
                runSpacing: AppSpacing.medium,
                children: [
                  _AvatarAddChoice(
                    selected: hasImage,
                    onTap: _saving ? null : _pickImage,
                  ),
                  for (final color in _colors)
                    _AvatarColorChoice(
                      color: color,
                      selected: !hasImage &&
                          color.toARGB32() == _draftColor.toARGB32(),
                      onTap: _saving
                          ? null
                          : () => setState(() {
                                _draftColor = color;
                                _draftImagePath = null;
                                _errorMessage = null;
                              }),
                    ),
                ],
              ),
            ),
            if (_errorMessage != null) ...[
              const SizedBox(height: AppSpacing.medium),
              Text(
                _errorMessage!,
                style: Theme.of(
                  context,
                ).textTheme.bodyMedium?.copyWith(color: colorScheme.error),
              ),
            ],
          ],
        ),
      ),
      actions: [
        AppDefaultButton(
          label: '取消',
          onPressed: _saving ? null : () => Navigator.of(context).pop(),
        ),
        AppAsyncPrimaryButton(
          label: '确认',
          isLoading: _saving,
          onPressed: _save,
        ),
      ],
    );
  }

  Future<void> _pickImage() async {
    final path = await widget.onPickImage();
    if (path != null && mounted) {
      setState(() {
        _draftImagePath = path;
        _errorMessage = null;
      });
    }
  }

  Future<void> _save() async {
    setState(() {
      _saving = true;
      _errorMessage = null;
    });
    try {
      await widget.onSave(
        colorValue: _draftColor.toARGB32(),
        imagePath: _draftImagePath,
      );
      if (mounted) {
        Navigator.of(context).pop();
      }
    } catch (error) {
      if (mounted) {
        setState(() {
          _saving = false;
          _errorMessage = error.toString();
        });
      }
    }
  }
}

class _AvatarAddChoice extends StatelessWidget {
  const _AvatarAddChoice({required this.selected, required this.onTap});

  final bool selected;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return InkResponse(
      radius: AppSizes.minTouchTarget / 2,
      onTap: onTap,
      child: SizedBox.square(
        dimension: AppSizes.minTouchTarget,
        child: DecoratedBox(
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            border: Border.all(
              color:
                  selected ? colorScheme.primary : colorScheme.outlineVariant,
              width: selected ? 3 : 1,
            ),
          ),
          child: const Icon(Icons.add),
        ),
      ),
    );
  }
}

class _AvatarColorChoice extends StatelessWidget {
  const _AvatarColorChoice({
    required this.color,
    required this.selected,
    required this.onTap,
  });

  final Color color;
  final bool selected;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final checkColor =
        ThemeData.estimateBrightnessForColor(color) == Brightness.dark
            ? Colors.white
            : Colors.black;
    return InkResponse(
      radius: AppSizes.minTouchTarget / 2,
      onTap: onTap,
      child: SizedBox.square(
        dimension: AppSizes.minTouchTarget,
        child: DecoratedBox(
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
            border: Border.all(
              color: selected
                  ? Theme.of(context).colorScheme.onSurface
                  : Colors.transparent,
              width: 3,
            ),
          ),
          child: selected ? Icon(Icons.check, color: checkColor) : null,
        ),
      ),
    );
  }
}
