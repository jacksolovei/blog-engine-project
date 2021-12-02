package main.service;

import lombok.AllArgsConstructor;
import main.api.request.EmailRequest;
import main.api.request.LoginRequest;
import main.api.request.PasswordRequest;
import main.api.request.RegRequest;
import main.api.response.AuthCheckResponse;
import main.api.response.ResultResponse;
import main.api.response.RegResponse;
import main.dto.UserDto;
import main.model.CaptchaCode;
import main.model.User;
import main.repository.CaptchaRepository;
import main.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AuthCheckService {
    public static final int PASSWORD_LENGTH = 6;
    public static final int MAX_LENGTH = 255;
    public static final PasswordEncoder BCRYPT = new BCryptPasswordEncoder(12);

    private final UserRepository userRepository;
    private final CaptchaRepository captchaRepository;
    private final MapperService mapperService;
    private final AuthenticationManager authenticationManager;
    private final EmailServiceImpl emailService;

    public AuthCheckResponse getAuthCheck(Principal principal) {
        if (principal == null) {
            AuthCheckResponse authCheck = new AuthCheckResponse();
            authCheck.setResult(false);
            return authCheck;
        }
        return getResponse(principal.getName());
    }

    public AuthCheckResponse login(LoginRequest loginRequest) {
        Authentication auth = authenticationManager
                .authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginRequest.getEmail(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        org.springframework.security.core.userdetails.User user =
                (org.springframework.security.core.userdetails.User) auth.getPrincipal();
        return getResponse(user.getUsername());
    }

    private AuthCheckResponse getResponse(String email) {
        main.model.User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User " + email + " not found"));
        UserDto userDto = mapperService.convertUserToDto(currentUser);
        AuthCheckResponse authCheck = new AuthCheckResponse();
        authCheck.setResult(true);
        authCheck.setUser(userDto);
        return authCheck;
    }

    public ResultResponse getLogoutResponse() {
        ResultResponse resultResponse = new ResultResponse();
        resultResponse.setResult(true);
        return resultResponse;
    }

    public RegResponse getRegResponse(RegRequest regRequest) {
        RegResponse regResponse = new RegResponse();
        Map<String, String> errors = new HashMap<>();
        List<String> emails = userRepository.findAll().stream()
                .map(User::getEmail).collect(Collectors.toList());
        String email = regRequest.getEmail();
        if (emails.contains(email)) {
            errors.put("email", "Этот e-mail уже зарегистрирован");
        }
        String name = regRequest.getName();
        if (name.length() > MAX_LENGTH || !name.matches("[А-Яа-яA-Za-z]+([А-Яа-яA-Za-z\\s]+)?")) {
            errors.put("name", "Имя указано неверно");
        }
        String password = regRequest.getPassword();
        if (password.length() < PASSWORD_LENGTH) {
            errors.put("password", "Пароль короче 6-ти символов");
        }
        String captcha = regRequest.getCaptcha();
        String secret = regRequest.getCaptchaSecret();
        Optional<CaptchaCode> optionalCaptcha = captchaRepository.findCaptchaBySecretCode(secret);
        if (optionalCaptcha.isPresent()) {
            if (!optionalCaptcha.get().getCode().equals(captcha)) {
                errors.put("captcha", "Код с картинки введён неверно");
            }
        } else {
            errors.put("captcha", "код устарел");
        }
        if (errors.isEmpty()) {
            regResponse.setResult(true);
            User user = new User();
            user.setIsModerator((byte) 0);
            user.setRegTime(new Date());
            user.setName(name);
            user.setEmail(email);
            user.setPassword(BCRYPT.encode(password));
            userRepository.save(user);
        } else {
            regResponse.setResult(false);
            regResponse.setErrors(errors);
        }
        return regResponse;
    }

    public ResultResponse restorePassword(EmailRequest emailRequest) {
        ResultResponse resultResponse = new ResultResponse();
        String email = emailRequest.getEmail();
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            String code = UUID.randomUUID().toString().replaceAll("-", "");
            String baseUrl = ServletUriComponentsBuilder
                    .fromCurrentContextPath().build().toUriString();
            String link = baseUrl + "/login/change-password/" + code;
            emailService.sendSimpleMessage(email, "смена пароля",
                    "Ссылка для смены пароля\n" + link);
            user.setCode(code);
            userRepository.save(user);
            resultResponse.setResult(true);
        } else {
            resultResponse.setResult(false);
        }
        return resultResponse;
    }

    public RegResponse updatePassword(PasswordRequest passwordRequest) {
        RegResponse regResponse = new RegResponse();
        Map<String, String> errors = new HashMap<>();
        String code = passwordRequest.getCode();
        if (userRepository.findByCode(code).isEmpty()) {
            errors.put("code",
                    "Ссылка для восстановления пароля устарела. " +
                            "<a href=\"/auth/restore\">Запросить ссылку снова</a>");
        }
        String password = passwordRequest.getPassword();
        if (password.length() < PASSWORD_LENGTH) {
            errors.put("password", "Пароль короче 6-ти символов");
        }
        String captcha = passwordRequest.getCaptcha();
        String secret = passwordRequest.getCaptchaSecret();
        Optional<CaptchaCode> optionalCaptcha = captchaRepository
                .findCaptchaBySecretCode(secret);
        if (optionalCaptcha.isPresent()) {
            if (!optionalCaptcha.get().getCode().equals(captcha)) {
                errors.put("captcha", "Код с картинки введён неверно");
            }
        } else {
            errors.put("captcha", "код устарел");
        }
        if (errors.isEmpty()) {
            regResponse.setResult(true);
            User user = userRepository.findByCode(code)
                    .orElseThrow(() -> new NoSuchElementException("User not found"));
            user.setPassword(BCRYPT.encode(password));
            user.setCode(null);
            userRepository.save(user);
        } else {
            regResponse.setResult(false);
            regResponse.setErrors(errors);
        }
        return regResponse;
    }
}
