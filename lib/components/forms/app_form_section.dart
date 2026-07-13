import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';

class AppFormSection extends StatelessWidget {
  const AppFormSection({
    required this.title,
    required this.children,
    this.description,
    this.showDivider = false,
    super.key,
  });

  final String title;
  final String? description;
  final List<Widget> children;
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: Theme.of(context).textTheme.titleMedium),
        if (description != null) ...[
          const SizedBox(height: AppSpacing.xSmall),
          Text(
            description!,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
          ),
        ],
        const SizedBox(height: AppSpacing.large),
        for (var index = 0; index < children.length; index++) ...[
          children[index],
          if (index != children.length - 1)
            const SizedBox(height: AppSpacing.medium),
        ],
        if (showDivider) ...[
          const SizedBox(height: AppSpacing.xLarge),
          const Divider(),
        ],
      ],
    );
  }
}
