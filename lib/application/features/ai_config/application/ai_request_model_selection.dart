import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_model_repository.dart';

class AiRequestModelSelection {
  const AiRequestModelSelection(this._preferenceRepository);

  final AiPreferenceRepository _preferenceRepository;

  Future<AiPurposePreference> forPurpose(AiPurpose purpose) async {
    final preferences = await _preferenceRepository.fetchPreferences();
    return preferences.where((item) => item.purpose == purpose).firstOrNull ??
        AiPurposePreference(purpose: purpose);
  }
}
