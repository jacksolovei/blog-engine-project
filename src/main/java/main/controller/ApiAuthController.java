package main.controller;

import lombok.AllArgsConstructor;
import main.api.request.EmailRequest;
import main.api.request.LoginRequest;
import main.api.request.PasswordRequest;
import main.api.request.RegRequest;
import main.api.response.AuthCheckResponse;
import main.api.response.CaptchaResponse;
import main.api.response.ResultResponse;
import main.api.response.RegResponse;
import main.service.AuthCheckService;
import main.service.CaptchaService;
import main.service.SettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class ApiAuthController {
    private final AuthCheckService authCheckService;
    private final CaptchaService captchaService;
    private final SettingsService settingsService;

    @GetMapping("/check")
    public ResponseEntity<AuthCheckResponse> authCheck(Principal principal) {
        return ResponseEntity.ok(authCheckService.getAuthCheck(principal));
    }

    @GetMapping("/captcha")
    public ResponseEntity<CaptchaResponse> getCaptcha() {
        return ResponseEntity.ok(captchaService.getCaptchaCode());
    }

    @PostMapping("/register")
    public ResponseEntity<RegResponse> register(@RequestBody RegRequest regRequest) {
        if (!settingsService.isMultiUser()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(authCheckService.getRegResponse(regRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthCheckResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authCheckService.login(loginRequest));
    }

    @PreAuthorize("hasAuthority('user:write')")
    @GetMapping("/logout")
    public ResponseEntity<ResultResponse> logout() {
        return ResponseEntity.ok(authCheckService.getLogoutResponse());
    }

    @PostMapping("/restore")
    public ResponseEntity<ResultResponse> restore(@RequestBody EmailRequest emailRequest) {
        return ResponseEntity.ok(authCheckService.restorePassword(emailRequest));
    }

    @PostMapping("/password")
    public ResponseEntity<RegResponse> updatePassword(
            @RequestBody PasswordRequest passwordRequest) {
        return ResponseEntity.ok(authCheckService.updatePassword(passwordRequest));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNoSuchElementException(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
