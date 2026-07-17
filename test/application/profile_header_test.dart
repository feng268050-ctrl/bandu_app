import 'package:bandu_wrong_notebook/application/features/profile/domain/avatar_settings.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_models.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/widgets/profile_header.dart';
import 'package:bandu_wrong_notebook/components/design_system/theme/app_theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('profile header keeps model and device summaries',
      (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: buildAppTheme(Brightness.light),
        home: Scaffold(
          body: SizedBox(
            height: 320,
            child: ProfileHeader(
              summary: const StudentProfileSummary(
                nickname: 'Gin',
                educationStageLabel: '小学',
                gradeLabel: '4年级',
              ),
              modelConfigLabel: '未配置',
              deviceNameLabel: 'OPPO PKT110',
              avatar: const AvatarSettings(),
              onTapAvatar: () {},
            ),
          ),
        ),
      ),
    );

    expect(find.text('模型配置：未配置'), findsOneWidget);
    expect(find.text('设备名称：OPPO PKT110'), findsOneWidget);
  });
}
