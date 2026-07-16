import 'package:flutter/material.dart';

class AppDefaultButton extends StatelessWidget {
  const AppDefaultButton({
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
        padding == null ? null : OutlinedButton.styleFrom(padding: padding);
    final labelWidget = Text(
      label,
      maxLines: labelMaxLines,
      overflow: labelMaxLines == null ? null : TextOverflow.ellipsis,
    );
    final button = icon == null
        ? OutlinedButton(
            onPressed: onPressed,
            style: style,
            child: labelWidget,
          )
        : OutlinedButton.icon(
            onPressed: onPressed,
            style: style,
            icon: icon!,
            label: labelWidget,
          );

    return expanded ? SizedBox(width: double.infinity, child: button) : button;
  }
}
