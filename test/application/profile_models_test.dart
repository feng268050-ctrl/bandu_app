import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('grade is calculated from current year minus enrollment year', () {
    final user = UserProfile(
      id: 'u1',
      email: 'student@example.com',
      name: '小伴',
      educationStage: 'primary',
      enrollmentYear: DateTime.now().year - 4,
    );

    final summary = StudentProfileSummary.fromUser(user);

    expect(summary.nickname, '小伴');
    expect(summary.educationStageLabel, '小学');
    expect(summary.gradeLabel, '4年级');
  });
}
