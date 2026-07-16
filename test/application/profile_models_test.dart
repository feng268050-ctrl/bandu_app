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

  test('grade is capped by education stage', () {
    final user = UserProfile(
      id: 'u1',
      email: 'student@example.com',
      name: '小伴',
      educationStage: 'primary',
      enrollmentYear: DateTime.now().year - 24,
    );

    final summary = StudentProfileSummary.fromUser(user);

    expect(summary.headline, '小学');
    expect(summary.gradeLabel, isNull);
  });

  test('profile overview hides diagnostics and manual upload tasks', () {
    final overview = ProfileOverviewState.fromUser(null);

    expect(overview.sections, isNot(contains(ProfileSection.network)));
    expect(overview.sections, isNot(contains(ProfileSection.pendingTasks)));
    expect(
      overview.sections,
      containsAll([
        ProfileSection.student,
        ProfileSection.ai,
        ProfileSection.device,
        ProfileSection.settings,
        ProfileSection.data,
        ProfileSection.about,
      ]),
    );
  });
}
