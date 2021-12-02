package main.service;

import lombok.AllArgsConstructor;
import main.api.request.SettingsRequest;
import main.api.response.SettingsResponse;
import main.model.GlobalSetting;
import main.repository.SettingsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class SettingsService {
    private final SettingsRepository settingsRepository;

    public SettingsResponse getGlobalSettings() {
        SettingsResponse settingsResponse = new SettingsResponse();
        settingsResponse.setMultiuserMode(
                settingsRepository.findSettingValue("MULTIUSER_MODE").equals("YES"));
        settingsResponse.setPostPremoderation(
                settingsRepository.findSettingValue("POST_PREMODERATION").equals("YES"));
        settingsResponse.setStatisticsIsPublic(
                settingsRepository.findSettingValue("STATISTICS_IS_PUBLIC").equals("YES"));
        return settingsResponse;
    }

    public void setSettings(SettingsRequest settingsRequest) {
        List<GlobalSetting> settings = settingsRepository.findAll();
        for (GlobalSetting setting : settings) {
            switch (setting.getCode()) {
                case "MULTIUSER_MODE":
                    setting.setValue(settingsRequest.isMultiuserMode() ? "YES" : "NO");
                    break;
                case "POST_PREMODERATION":
                    setting.setValue(settingsRequest.isPostPremoderation() ? "YES" : "NO");
                    break;
                case "STATISTICS_IS_PUBLIC":
                    setting.setValue(settingsRequest.isStatisticsIsPublic() ? "YES" : "NO");
                    break;
            }
            settingsRepository.save(setting);
        }
    }

    public boolean isMultiUser() {
        return settingsRepository.findSettingValue("MULTIUSER_MODE").equals("YES");
    }
}
