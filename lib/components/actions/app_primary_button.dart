import 'package:flutter/material.dart';

class AppPrimaryButton extends StatelessWidget {
  const AppPrimaryButton({
    required this.label,
    required this.onPressed,
    this.icon,
    this.expanded = false,
    this.padding,
    this.labelMaxLines,
    super.key,
  });

  final String label;
  final VoidCallback? onPressed;
  final Widget? icon;
  final bool expanded;
  final EdgeInsetsGeometry? padding;
  final int? labelMaxLines;

  @override
  Widget build(BuildContext context) {
    final style =
        padding == null ? null : FilledButton.styleFrom(padding: padding);
    final labelWidget = Text(
      label,
      maxLines: labelMaxLines,
      overflow: labelMaxLines == null ? null : TextOverflow.ellipsis,
    );
    final button = icon == null
        ? FilledButton(
            onPressed: onPressed,
            style: style,
            child: labelWidget,
          )
        : FilledButton.icon(
            onPressed: onPressed,
            style: style,
            icon: icon!,
            label: labelWidget,
          );

    return expanded ? SizedBox(width: double.infinity, child: button) : button;
  }
}
