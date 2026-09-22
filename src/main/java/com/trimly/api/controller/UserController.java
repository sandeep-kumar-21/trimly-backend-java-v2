package com.trimly.api.controller;

import com.trimly.api.dto.request.ChangePasswordRequest;
import com.trimly.api.dto.request.DeleteAccountRequest;
import com.trimly.api.dto.request.UpdatePreferencesRequest;
import com.trimly.api.dto.request.UpdateProfileRequest;
import com.trimly.api.dto.response.ExportJobResponse;
import com.trimly.api.dto.response.UserResponse;
import com.trimly.api.exception.UnauthorizedException;
import com.trimly.api.security.SecurityUtils;
import com.trimly.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Users", description = "User profile, preferences, and account management")
public class UserController {

    private final UserService userService;

    @PatchMapping("/profile")
    @Operation(summary = "Update authenticated user profile (name and avatar URL)")
    @ApiResponse(responseCode = "200", description = "Profile updated successfully.")
    public ResponseEntity<UserResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        Long userId = getCurrentUserId();
        UserResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/preferences")
    @Operation(summary = "Update user settings and preferences (theme, timezone, notifications)")
    @ApiResponse(responseCode = "200", description = "Preferences updated successfully.")
    public ResponseEntity<UserResponse> updatePreferences(@RequestBody UpdatePreferencesRequest request) {
        Long userId = getCurrentUserId();
        UserResponse response = userService.updatePreferences(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change authenticated user password")
    @ApiResponse(responseCode = "200", description = "Password changed successfully.")
    @ApiResponse(responseCode = "400", description = "Incorrect current password.")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = getCurrentUserId();
        userService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @GetMapping("/export-data")
    @Operation(summary = "Initiate complete account data export")
    @ApiResponse(responseCode = "202", description = "Export job queued/completed. Returns jobId.")
    public ResponseEntity<ExportJobResponse> exportData() {
        Long userId = getCurrentUserId();
        ExportJobResponse response = userService.initiateDataExport(userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/export-data/{jobId}")
    @Operation(summary = "Retrieve result for a queued export-data job")
    @ApiResponse(responseCode = "200", description = "Returns job status and exported JSON payload.")
    public ResponseEntity<ExportJobResponse> getExportStatus(@PathVariable String jobId) {
        ExportJobResponse response = userService.getExportStatus(jobId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/account")
    @Operation(summary = "Permanently delete account and all associated resources")
    @ApiResponse(responseCode = "202", description = "Account deletion initiated and completed.")
    @ApiResponse(responseCode = "400", description = "Password confirmation failed.")
    public ResponseEntity<Map<String, String>> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        Long userId = getCurrentUserId();
        userService.deleteAccount(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "jobId", "account-deleted-" + userId,
                "message", "Account deleted successfully"
        ));
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));
    }
}
