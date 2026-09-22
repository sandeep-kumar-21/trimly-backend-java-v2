package com.trimly.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQrCodeRequest {

    private String shortCode;
    private String longUrl;
    private String title;
    private List<String> tags;
    private Boolean createLink;
    private Map<String, Object> qrConfig;
}
