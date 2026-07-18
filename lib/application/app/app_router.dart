import 'package:bandu_wrong_notebook/application/app/app_shell.dart';
import 'package:bandu_wrong_notebook/application/features/auth/auth_providers.dart';
import 'package:bandu_wrong_notebook/application/features/auth/presentation/auth_controller.dart';
import 'package:bandu_wrong_notebook/application/features/auth/presentation/login_page.dart';
import 'package:bandu_wrong_notebook/application/features/capture/presentation/capture_page.dart';
import 'package:bandu_wrong_notebook/application/features/home/presentation/home_page.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/error_item_detail_page.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/library_page.dart';
import 'package:bandu_wrong_notebook/application/features/practice/presentation/practice_page.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_models.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/profile_page.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/exam_management_page.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/exam_session_page.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/pdf_question_bank_page.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_set_detail_page.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_set_management_page.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_metric.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_period.dart';
import 'package:bandu_wrong_notebook/application/features/stats/presentation/stats_page.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/presentation/tutor_page.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final authStatus = ref.watch(
    authControllerProvider.select((state) => state.status),
  );
  final bypassAuth = ref.watch(authBypassProvider);

  return GoRouter(
    initialLocation: '/home',
    redirect: (context, state) {
      final isLoginRoute = state.matchedLocation == '/login';

      if (bypassAuth) {
        return isLoginRoute ? '/home' : null;
      }

      final isSignedIn = authStatus == AuthStatus.signedIn;
      final isSignedOut = authStatus == AuthStatus.signedOut;

      if (isSignedOut && !isLoginRoute) {
        return '/login';
      }

      if (isSignedIn && isLoginRoute) {
        return '/home';
      }

      return null;
    },
    routes: [
      GoRoute(path: '/login', builder: (context, state) => const LoginPage()),
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) {
          return AppShell(
            navigationShell: navigationShell,
            showPrimaryNavigation: shouldShowPrimaryNavigation(
              state.uri.path,
            ),
          );
        },
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/home',
                builder: (context, state) => const HomePage(),
                routes: [
                  GoRoute(
                    path: 'library',
                    builder: (context, state) => const LibraryPage(),
                    routes: [
                      GoRoute(
                        path: ':id',
                        builder: (context, state) => ErrorItemDetailPage(
                          errorItemId: state.pathParameters['id']!,
                        ),
                      ),
                    ],
                  ),
                  GoRoute(
                    path: 'stats/:metric',
                    builder: (context, state) => StatsPage(
                      initialPeriod: StatsPeriod.fromApiValue(
                        state.uri.queryParameters['period'],
                      ),
                      metric: StatsMetric.fromRouteValue(
                        state.pathParameters['metric'],
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/tutor',
                builder: (context, state) => const TutorPage(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/capture',
                builder: (context, state) => const CapturePage(),
                routes: buildQuestionBankRoutes(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/practice',
                builder: (context, state) => const PracticePage(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/profile',
                builder: (context, state) => const ProfilePage(),
                routes: [
                  GoRoute(
                    path: 'student',
                    builder: (context, state) => const ProfilePage(
                      section: ProfileSection.student,
                    ),
                  ),
                  GoRoute(
                    path: 'settings',
                    builder: (context, state) => const ProfilePage(
                      section: ProfileSection.settings,
                    ),
                    routes: [
                      GoRoute(
                        path: 'ai',
                        builder: (context, state) => const ProfilePage(
                          section: ProfileSection.ai,
                        ),
                      ),
                      GoRoute(
                        path: 'device',
                        builder: (context, state) => const ProfilePage(
                          section: ProfileSection.device,
                        ),
                      ),
                      GoRoute(
                        path: 'data',
                        builder: (context, state) => const ProfilePage(
                          section: ProfileSection.data,
                        ),
                      ),
                    ],
                  ),
                  GoRoute(
                    path: 'about',
                    builder: (context, state) => const ProfilePage(
                      section: ProfileSection.about,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    ],
  );
});

List<RouteBase> buildQuestionBankRoutes() {
  return [
    GoRoute(
      path: 'pdf-import',
      builder: (context, state) => const PdfQuestionBankPage(),
      routes: [
        GoRoute(
          path: 'exams',
          builder: (context, state) => const ExamManagementPage(),
        ),
        GoRoute(
          path: 'banks',
          builder: (context, state) => const QuestionSetManagementPage(),
        ),
        GoRoute(
          path: 'bank/:questionSetId',
          builder: (context, state) => QuestionSetDetailPage(
            questionSetId: state.pathParameters['questionSetId']!,
          ),
        ),
        GoRoute(
          path: 'exam/:sessionId',
          builder: (context, state) => ExamSessionPage(
            sessionId: state.pathParameters['sessionId']!,
          ),
        ),
      ],
    ),
  ];
}
