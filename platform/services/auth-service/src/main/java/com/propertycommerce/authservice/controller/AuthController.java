package com.propertycommerce.authservice.controller;
import com.propertycommerce.authservice.service.AuthService;
import com.propertycommerce.shared.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String,String>>> register(@RequestBody Map<String, Object> req) {
        var result = authService.register(
                (String) req.get("email"), (String) req.get("password"),
                (String) req.get("firstName"), (String) req.get("lastName"),
                (String) req.get("phone"), (List<String>) req.get("roles"));
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(Map.of("userId", result.userId(),
                        "message", "Registration successful. Check your email to verify your account.")));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthService.AuthResult>> login(
            @RequestBody Map<String, String> req, HttpServletRequest httpReq) {
        var result = authService.login(req.get("email"), req.get("password"),
                httpReq.getRemoteAddr(), httpReq.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthService.AuthResult>> refresh(@RequestBody Map<String,String> req) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshTokens(req.get("refreshToken"))));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody Map<String,String> req, @AuthenticationPrincipal String userId) {
        authService.logout(userId, req.get("refreshToken"));
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody Map<String,String> req) {
        authService.requestPasswordReset(req.get("email"));
        return ResponseEntity.ok(ApiResponse.success(null, "If that email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody Map<String,String> req) {
        authService.resetPassword(req.get("token"), req.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successful"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Object>> updateProfile(
            @RequestBody Map<String,String> req, @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(authService.updateProfile(userId, req)));
    }
}
