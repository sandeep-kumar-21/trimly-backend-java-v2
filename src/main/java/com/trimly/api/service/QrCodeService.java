package com.trimly.api.service;

import com.trimly.api.dto.request.CreateQrCodeRequest;
import com.trimly.api.dto.request.UpdateQrCodeRequest;
import com.trimly.api.dto.response.QrCodeResponse;

import java.util.List;
import java.util.Map;

public interface QrCodeService {

    record QrImageResult(byte[] data, String contentType, boolean fromCache) {}

    Map<String, Object> createQrCode(CreateQrCodeRequest request, Long userId);

    List<QrCodeResponse> getUserQrCodes(Long userId, String qrExpiration, String linkAttachment);

    QrCodeResponse getQrCodeDetails(String code, Long userId);

    QrImageResult getQrCodeImage(String code, Long userId, String format);

    void deleteQrCode(String code, Long userId);

    Map<String, Object> duplicateQrCode(String code, String targetShortCode, Long userId);

    QrCodeResponse updateQrCode(String code, UpdateQrCodeRequest request, Long userId);
}
