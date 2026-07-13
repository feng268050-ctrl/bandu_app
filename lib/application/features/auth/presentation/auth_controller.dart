import 'dart:async';

import 'package:bandu_wrong_notebook/application/features/auth/auth_providers.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final authControllerProvider = NotifierProvider<AuthController, AuthState>(
  AuthController.new,
);

enum AuthStatus { unknown, signedOut, signedIn }

class AuthState {
  const AuthState({
    required this.status,
    this.user,
    this.isBusy = false,
    this.errorMessage,
  });

  final AuthStatus status;
  final UserProfile? user;
  final bool isBusy;
  final String? errorMessage;

  AuthState copyWith({
    AuthStatus? status,
    UserProfile? user,
    bool? isBusy,
    String? errorMessage,
  }) {
    return AuthState(
      status: status ?? this.status,
      user: user ?? this.user,
      isBusy: isBusy ?? this.isBusy,
      errorMessage: errorMessage,
    );
  }
}

class AuthController extends Notifier<AuthState> {
  bool get _bypassAuth => ref.read(authBypassProvider);

  @override
  AuthState build() {
    if (_bypassAuth) {
      return const AuthState(
        status: AuthStatus.signedIn,
        user: guestUserProfile,
      );
    }

    unawaited(_restoreSession());
    return const AuthState(status: AuthStatus.unknown);
  }

  Future<void> login({required String email, required String password}) async {
    if (_bypassAuth) {
      return;
    }

    state = state.copyWith(isBusy: true, errorMessage: null);

    try {
      final session = await ref
          .read(loginUseCaseProvider)
          .call(email: email, password: password);
      state = AuthState(status: AuthStatus.signedIn, user: session.user);
    } catch (error) {
      state = AuthState(
        status: AuthStatus.signedOut,
        isBusy: false,
        errorMessage: error.toString(),
      );
    }
  }

  Future<void> register({
    required String email,
    required String password,
    String? name,
  }) async {
    if (_bypassAuth) {
      return;
    }

    state = state.copyWith(isBusy: true, errorMessage: null);

    try {
      final session = await ref
          .read(registerUseCaseProvider)
          .call(email: email, password: password, name: name);
      state = AuthState(status: AuthStatus.signedIn, user: session.user);
    } catch (error) {
      state = AuthState(
        status: AuthStatus.signedOut,
        isBusy: false,
        errorMessage: error.toString(),
      );
    }
  }

  Future<void> logout() async {
    if (_bypassAuth) {
      return;
    }

    state = state.copyWith(isBusy: true, errorMessage: null);
    await ref.read(logoutUseCaseProvider).call();
    state = const AuthState(status: AuthStatus.signedOut);
  }

  Future<bool> updateProfile({
    required String name,
    required String educationStage,
    required int enrollmentYear,
  }) async {
    state = state.copyWith(isBusy: true, errorMessage: null);
    try {
      final user = await ref.read(updateProfileUseCaseProvider).call(
            name: name,
            educationStage: educationStage,
            enrollmentYear: enrollmentYear,
          );
      state = AuthState(status: AuthStatus.signedIn, user: user);
      return true;
    } catch (error) {
      state = state.copyWith(isBusy: false, errorMessage: error.toString());
      return false;
    }
  }

  Future<void> _restoreSession() async {
    try {
      final session = await ref.read(restoreSessionUseCaseProvider).call();
      if (session == null) {
        state = const AuthState(status: AuthStatus.signedOut);
      } else {
        state = AuthState(status: AuthStatus.signedIn, user: session.user);
      }
    } catch (_) {
      state = const AuthState(status: AuthStatus.signedOut);
    }
  }
}
