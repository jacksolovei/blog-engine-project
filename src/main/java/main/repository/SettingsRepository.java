package main.repository;

import main.model.GlobalSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingsRepository extends JpaRepository<GlobalSetting, Integer> {

    @Query(value = "SELECT value FROM global_settings WHERE code = :code",
            nativeQuery = true)
    String findSettingValue(@Param("code") String code);
}
