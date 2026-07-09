import 'package:bandu_wrong_notebook/app/app_shell.dart';
import 'package:bandu_wrong_notebook/features/auth/presentation/auth_controller.dart';
import 'package:bandu_wrong_notebook/features/auth/presentation/login_page.dart';
import 'package:bandu_wrong_notebook/features/capture/presentation/capture_page.dart';
import 'package:bandu_wrong_notebook/features/home/presentation/home_page.dart';
import 'package:bandu_wrong_notebook/features/library/presentation/error_item_detail_page.dart';
import 'package:bandu_wrong_notebook/features/library/presentation/library_page.dart';
import 'package:bandu_wrong_notebook/features/practice/presentation/practice_page.dart';
import 'package:bandu_wrong_notebook/features/profile/presentation/profile_page.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final authState = ref.watch(authControllerProvider);

  return GoRouter(
    initialLocation: '/home',
    redirect: (context, state) {
      final isLoginRoute = state.matchedLocation == '/login';
      final isSignedIn = authState.status == AuthStatus.signedIn;
      final isSignedOut = authState.status == AuthStatus.signedOut;

      if (isSignedOut && !isLoginRoute) {
        return '/login';
      }

      if (isSignedIn && isLoginRoute) {
        return '/home';
      }

      return null;
    },
    routes: [
      GoRoute(
        path: '/login',
        builder: (context, state) => const LoginPage(),
      ),
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) {
          return AppShell(navigationShell: navigationShell);
        },
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/home',
                builder: (context, state) => const HomePage(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/library',
                builder: (context, state) => const LibraryPage(),
                routes: [
                  GoRoute(
                    path: ':id',
                    builder: (context, state) {
                      return ErrorItemDetailPage(
                        errorItemId: state.pathParameters['id']!,
                      );
                    },
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/capture',
                builder: (context, state) => const CapturePage(),
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
              ),
            ],
          ),
        ],
      ),
    ],
  );
});
