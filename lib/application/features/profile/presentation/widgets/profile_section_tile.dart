import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_models.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/widgets/profile_section_ui.dart';
import 'package:flutter/material.dart';

class ProfileSectionTile extends StatelessWidget {
  const ProfileSectionTile({
    required this.section,
    required this.onTap,
    super.key,
  });

  final ProfileSection section;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card.outlined(
      child: ListTile(
        onTap: onTap,
        leading: Icon(section.icon),
        title: Text(section.title),
        subtitle: Text(section.description),
        trailing: const Icon(Icons.chevron_right),
      ),
    );
  }
}
