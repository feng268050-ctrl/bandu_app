import 'dart:async';

import 'package:bandu_wrong_notebook/features/auth/data/auth_repository.dart';
import 'package:bandu_wrong_notebook/features/auth/domain/auth_models.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final authControllerProvider =
    NotifierProvider<AuthController, AuthState>(AuthController.new);

enum AuthStatus {
  unknown,
  signedOut,
  signedIn,
}

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
  AuthRepository get _repository => ref.read(authRepositoryProvider);

  @override
  AuthState build() {
    unawaited(_restoreSession());
    return const AuthState(status: AuthStatus.unknown);
  }

  Future<void> login({
    required String email,
    required String password,
  }) async {
    state = state.copyWith(isBusy: true, errorMessage: null);

    try {
      final session = await _repository.login(
        email: email,
        password: password,
      );
      state = AuthState(
        status: AuthStatus.signedIn,
        user: session.user,
      );
    } catch (error) {
      state = AuthState(
        status: AuthStatus.signedOut,
        isBusy: false,
        errorMessage: error.toString(),
      );
    }
  }

  Future<void> logout() async {
    state = state.copyWith(isBusy: true, errorMessage: null);
    await _repository.logout();
    state = const AuthState(status: AuthStatus.signedOut);
  }

  Future<void> _restoreSession() async {
    try {
      final session = await _repository.restoreSession();
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
