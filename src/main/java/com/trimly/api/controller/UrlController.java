package com.trimly.api.controller;

import com.trimly.api.dto.request.*;
import com.trimly.api.dto.response.UrlResponse;
import com.trimly.api.exception.UnauthorizedException;
import com.trimly.api.security.SecurityUtils;
import com.trimly.api.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
@Tag(name = "URLs", description = "URL creation, management, tags, and custom aliases")
public class UrlController {

    private final UrlService urlService;

    @PostMapping
    @Operation(summary = "Create a short URL (supports anonymous or authenticated users)")
    @ApiResponse(responseCode = "201", description = "Short URL created successfully.")
    @ApiResponse(responseCode = "400", description = "Validation error.")
    @ApiResponse(responseCode = "409", description = "Custom alias is already taken.")
    public ResponseEntity<UrlResponse> createUrl(@Valid @RequestBody CreateUrlRequest request) {
        Long userId = SecurityUtils.getCurrentUserId().orElse(null);
        UrlResponse response = urlService.createUrl(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get all URLs created by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Returns list of user URLs.")
    @ApiResponse(responseCode = "401", description = "Unauthorized.")
    public ResponseEntity<List<UrlResponse>> getUserUrls(
            @RequestParam(required = false) String tags,
            @RequestParam(required = false, defaultValue = "all") String linkType,
            @RequestParam(required = false, defaultValue = "all") String qrAttachment) {
        Long userId = getCurrentUserId();
        List<String> parsedTags = tags != null && !tags.trim().isEmpty()
                ? Arrays.stream(tags.split(",")).map(String::trim).toList()
                : null;

        List<UrlResponse> response = urlService.getUserUrls(userId, parsedTags, linkType, qrAttachment);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tags")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get distinct tags used by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Returns list of distinct tags.")
    public ResponseEntity<List<String>> getDistinctTags() {
        Long userId = getCurrentUserId();
        List<String> tags = urlService.getDistinctTags(userId);
        return ResponseEntity.ok(tags);
    }

    @PatchMapping("/bulk-tags")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Bulk add/remove tags for specified links")
    @ApiResponse(responseCode = "200", description = "Links updated successfully.")
    public ResponseEntity<Map<String, Object>> bulkUpdateTags(@Valid @RequestBody BulkTagsRequest request) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = urlService.bulkUpdateTags(userId, request);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/bulk-hide")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Bulk hide/unhide specified links")
    @ApiResponse(responseCode = "200", description = "Links hidden/unhidden successfully.")
    public ResponseEntity<Map<String, Object>> bulkHideUrls(@Valid @RequestBody BulkHideRequest request) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = urlService.bulkHideUrls(userId, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{code}/promote-to-link")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Promote a QR-only link to a visible link in links list")
    @ApiResponse(responseCode = "200", description = "URL successfully promoted to visible link.")
    public ResponseEntity<UrlResponse> promoteToLink(@PathVariable String code) {
        Long userId = getCurrentUserId();
        UrlResponse response = urlService.promoteToLink(code, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{code}/edit-back-half")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create a new short link with a custom back-half, copying all fields from source URL")
    @ApiResponse(responseCode = "201", description = "New short link created with custom alias.")
    public ResponseEntity<UrlResponse> editBackHalf(
            @PathVariable String code,
            @Valid @RequestBody EditBackHalfRequest request) {
        Long userId = getCurrentUserId();
        UrlResponse response = urlService.editBackHalf(code, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get metadata for a short URL by code")
    @ApiResponse(responseCode = "200", description = "Returns short URL metadata.")
    @ApiResponse(responseCode = "404", description = "Short URL not found.")
    public ResponseEntity<UrlResponse> getUrlMetadata(@PathVariable String code) {
        UrlResponse response = urlService.getUrlMetadata(code);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{code}/verify-password")
    @Operation(summary = "Verify password for a password-protected short URL")
    @ApiResponse(responseCode = "200", description = "Password verified successfully, returns longUrl.")
    @ApiResponse(responseCode = "400", description = "Invalid password.")
    public ResponseEntity<Map<String, Object>> verifyPassword(
            @PathVariable String code,
            @Valid @RequestBody VerifyPasswordRequest request) {
        Map<String, Object> response = urlService.verifyPassword(code, request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{code}")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update short URL target or expiration (link owner only)")
    @ApiResponse(responseCode = "200", description = "Short URL updated successfully.")
    public ResponseEntity<UrlResponse> updateUrl(
            @PathVariable String code,
            @Valid @RequestBody UpdateUrlRequest request) {
        Long userId = getCurrentUserId();
        UrlResponse response = urlService.updateUrl(code, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{code}")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete a short URL (link owner only)")
    @ApiResponse(responseCode = "200", description = "Short URL deleted successfully.")
    public ResponseEntity<Map<String, Object>> deleteUrl(@PathVariable String code) {
        Long userId = getCurrentUserId();
        Map<String, Object> response = urlService.deleteUrl(code, userId);
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Unauthorized: Login required"));
    }
}
