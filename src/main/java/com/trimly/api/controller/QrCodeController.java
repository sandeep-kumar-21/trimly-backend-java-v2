package com.trimly.api.controller;

import com.trimly.api.dto.request.CreateQrCodeRequest;
import com.trimly.api.dto.request.DuplicateQrCodeRequest;
import com.trimly.api.dto.request.UpdateQrCodeRequest;
import com.trimly.api.dto.response.QrCodeResponse;
import com.trimly.api.exception.UnauthorizedException;
import com.trimly.api.security.SecurityUtils;
import com.trimly.api.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qrcodes")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "QR Codes", description = "QR Code studio, customization, and raster/vector image rendering")
public class QrCodeController {

    private final QrCodeService qrCodeService;

    @PostMapping
    @Operation(summary = "Generate and save a customized QR code for owned short URL")
    @ApiResponse(responseCode = "201", description = "QR code generated and saved.")
    public ResponseEntity<Map<String, Object>> createQrCode(@Valid @RequestBody CreateQrCodeRequest request) {
        Long userId = getCurrentUserId();
        Map<String, Object> response = qrCodeService.createQrCode(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all saved QR codes for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Returns list of user QR code configurations.")
    public ResponseEntity<List<QrCodeResponse>> getUserQrCodes(
            @RequestParam(required = false) String qrExpiration,
            @RequestParam(required = false) String linkAttachment) {
        Long userId = getCurrentUserId();
        List<QrCodeResponse> response = qrCodeService.getUserQrCodes(userId, qrExpiration, linkAttachment);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{code}/details")
    @Operation(summary = "Get single QR code details and metadata")
    @ApiResponse(responseCode = "200", description = "Returns QR code configuration and linked URL details.")
    public ResponseEntity<QrCodeResponse> getQrCodeDetails(@PathVariable String code) {
        Long userId = getCurrentUserId();
        QrCodeResponse response = qrCodeService.getQrCodeDetails(code, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Fetch rendered QR code image (SVG by default, PNG on demand)")
    @ApiResponse(responseCode = "200", description = "Returns SVG or PNG image binary.")
    public ResponseEntity<byte[]> getQrCodeImage(
            @PathVariable String code,
            @RequestParam(required = false, defaultValue = "svg") String format) {
        Long userId = getCurrentUserId();
        QrCodeService.QrImageResult result = qrCodeService.getQrCodeImage(code, userId, format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, result.contentType())
                .header("X-Cache", result.fromCache() ? "HIT" : "MISS")
                .body(result.data());
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Delete a saved QR code configuration")
    @ApiResponse(responseCode = "200", description = "QR code configuration deleted.")
    public ResponseEntity<Map<String, String>> deleteQrCode(@PathVariable String code) {
        Long userId = getCurrentUserId();
        qrCodeService.deleteQrCode(code, userId);
        return ResponseEntity.ok(Map.of("message", "QR Code deleted successfully"));
    }

    @PostMapping("/{code}/duplicate")
    @Operation(summary = "Duplicate a QR code design to another short URL")
    @ApiResponse(responseCode = "201", description = "QR code duplicated successfully.")
    public ResponseEntity<Map<String, Object>> duplicateQrCode(
            @PathVariable String code,
            @Valid @RequestBody DuplicateQrCodeRequest request) {
        Long userId = getCurrentUserId();
        Map<String, Object> response = qrCodeService.duplicateQrCode(code, request.getTargetShortCode(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{code}")
    @Operation(summary = "Update a QR code configuration (e.g. visibility)")
    @ApiResponse(responseCode = "200", description = "QR code updated successfully.")
    public ResponseEntity<QrCodeResponse> updateQrCode(
            @PathVariable String code,
            @RequestBody UpdateQrCodeRequest request) {
        Long userId = getCurrentUserId();
        QrCodeResponse response = qrCodeService.updateQrCode(code, request, userId);
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Unauthorized: Login required"));
    }
}
