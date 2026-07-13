import 'package:flutter/material.dart';

class StatsTile extends StatelessWidget {
  const StatsTile({
    required this.label,
    required this.value,
    required this.icon,
    this.isHighlighted = false,
    super.key,
  });

  final String label;
  final String value;
  final IconData icon;
  final bool isHighlighted;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Card.filled(
      color: isHighlighted ? colorScheme.primaryContainer : null,
      child: ListTile(
        leading: Icon(icon),
        title: Text(label),
        trailing: Text(value, style: Theme.of(context).textTheme.titleLarge),
      ),
    );
  }
}
