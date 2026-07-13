import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_primary_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_secondary_button.dart';
import 'package:flutter/material.dart';

class AppConfirmDialog extends StatelessWidget {
  const AppConfirmDialog({
    required this.title,
    required this.message,
    required this.confirmLabel,
    this.cancelLabel = '取消',
    this.isDestructive = false,
    super.key,
  });

  final String title;
  final String message;
  final String confirmLabel;
  final String cancelLabel;
  final bool isDestructive;

  @override
  Widget build(BuildContext context) {
    final confirmButton = isDestructive
        ? AppSecondaryButton(
            label: confirmLabel,
            onPressed: () => Navigator.of(context).pop(true),
          )
        : AppPrimaryButton(
            label: confirmLabel,
            onPressed: () => Navigator.of(context).pop(true),
          );

    return AlertDialog(
      title: Text(title),
      content: Text(message),
      actions: [
        AppDefaultButton(
          label: cancelLabel,
          onPressed: () => Navigator.of(context).pop(false),
        ),
        confirmButton,
      ],
    );
  }
}

Future<bool> showAppConfirmDialog({
  required BuildContext context,
  required String title,
  required String message,
  required String confirmLabel,
  String cancelLabel = '取消',
  bool isDestructive = false,
}) async {
  return await showDialog<bool>(
        context: context,
        builder: (context) => AppConfirmDialog(
          title: title,
          message: message,
          confirmLabel: confirmLabel,
          cancelLabel: cancelLabel,
          isDestructive: isDestructive,
        ),
      ) ??
      false;
}
