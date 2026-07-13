import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';

void showAppSuccessSnackBar(BuildContext context, String message) {
  _showAppSnackBar(context, message: message, icon: Icons.check_circle_outline);
}

void showAppErrorSnackBar(BuildContext context, String message) {
  _showAppSnackBar(context, message: message, icon: Icons.error_outline);
}

void _showAppSnackBar(
  BuildContext context, {
  required String message,
  required IconData icon,
}) {
  final messenger = ScaffoldMessenger.of(context);
  messenger
    ..hideCurrentSnackBar()
    ..showSnackBar(
      SnackBar(
        content: Row(
          children: [
            Icon(icon, color: Theme.of(context).colorScheme.onInverseSurface),
            const SizedBox(width: AppSpacing.medium),
            Expanded(child: Text(message)),
          ],
        ),
      ),
    );
}
