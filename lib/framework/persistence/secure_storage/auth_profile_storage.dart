import 'dart:convert';

import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_profile_store.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/auth_profile_cache_mapper.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

final secureAuthProfileStoreProvider = Provider<AuthProfileStore>((ref) {
  return SecureAuthProfileStore(const FlutterSecureStorage());
});

class SecureAuthProfileStore implements AuthProfileStore {
  SecureAuthProfileStore(
    this._storage, {
    AuthProfileCacheMapper mapper = const AuthProfileCacheMapper(),
  }) : _mapper = mapper;

  static const _profileKey = 'bandu.authProfile';

  final FlutterSecureStorage _storage;
  final AuthProfileCacheMapper _mapper;

  @override
  Future<UserProfile?> read() async {
    final encoded = await _storage.read(key: _profileKey);
    if (encoded == null || encoded.trim().isEmpty) {
      return null;
    }
    try {
      final record = CachedAuthProfile.fromJson(
        requireJsonObject(jsonDecode(encoded), context: 'cached auth profile'),
      );
      return _mapper.fromCache(record);
    } catch (_) {
      await clear();
      return null;
    }
  }

  @override
  Future<void> save(UserProfile profile) {
    return _storage.write(
      key: _profileKey,
      value: jsonEncode(_mapper.toCache(profile).toJson()),
    );
  }

  @override
  Future<void> clear() {
    return _storage.delete(key: _profileKey);
  }
}
