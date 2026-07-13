import 'package:bandu_wrong_notebook/application/features/auth/domain/token_store.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

final secureTokenStoreProvider = Provider<TokenStore>((ref) {
  return SecureTokenStore(const FlutterSecureStorage());
});

class SecureTokenStore implements TokenStore {
  SecureTokenStore(this._storage);

  static const _refreshTokenKey = 'bandu.refreshToken';

  final FlutterSecureStorage _storage;
  String? _accessToken;

  @override
  Future<String?> readAccessToken() async => _accessToken;

  @override
  Future<String?> readRefreshToken() {
    return _storage.read(key: _refreshTokenKey);
  }

  @override
  Future<void> save(TokenPair tokenPair) async {
    _accessToken = tokenPair.accessToken;
    await _storage.write(key: _refreshTokenKey, value: tokenPair.refreshToken);
  }

  @override
  Future<void> clear() async {
    _accessToken = null;
    await _storage.delete(key: _refreshTokenKey);
  }
}
