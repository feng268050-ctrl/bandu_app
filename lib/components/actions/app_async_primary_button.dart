import 'package:bandu_wrong_notebook/components/actions/app_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_sizes.dart';
import 'package:flutter/material.dart';

class AppAsyncPrimaryButton extends StatelessWidget {
  const AppAsyncPrimaryButton({
    required this.label,
    required this.onPressed,
    required this.isLoading,
    this.icon,
    this.expanded = false,
    super.key,
  });

  final String label;
  final VoidCallback? onPressed;
  final bool isLoading;
  final Widget? icon;
  final bool expanded;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      liveRegion: true,
      label: isLoading ? '$label，处理中' : null,
      child: AppPrimaryButton(
        label: label,
        onPressed: isLoading ? null : onPressed,
        expanded: expanded,
        icon: isLoading
            ? const SizedBox.square(
                dimension: AppSizes.iconSmall,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : icon,
      ),
    );
  }
}
