import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';

class HomeMetricCard extends StatelessWidget {
  const HomeMetricCard({
    required this.label,
    required this.value,
    required this.icon,
    required this.onTap,
    this.showLabel = true,
    super.key,
  });

  final String label;
  final String value;
  final IconData icon;
  final VoidCallback onTap;
  final bool showLabel;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: '查看$label详情',
      child: Card.filled(
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          child: SizedBox(
            height: 156,
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.large),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(icon),
                      const Spacer(),
                      const Icon(Icons.chevron_right),
                    ],
                  ),
                  const Spacer(),
                  Text(
                    value,
                    style: Theme.of(context).textTheme.headlineMedium,
                  ),
                  if (showLabel)
                    Text(label, style: Theme.of(context).textTheme.bodyMedium),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
