package main.repository;

import main.model.CaptchaCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaptchaRepository extends JpaRepository<CaptchaCode, Integer> {

    @Query(value = "SELECT * FROM captcha_codes WHERE time < (NOW() - INTERVAL 1 HOUR)",
            nativeQuery = true)
    List<CaptchaCode> findOldCaptcha();

    @Query(value = "SELECT * FROM captcha_codes WHERE secret_code = :secret_code " +
            "AND time > (NOW() - INTERVAL 1 HOUR)", nativeQuery = true)
    Optional<CaptchaCode> findCaptchaBySecretCode(@Param("secret_code") String secretCode);
}
