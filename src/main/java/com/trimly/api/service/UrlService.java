package com.trimly.api.service;

import com.trimly.api.dto.request.*;
import com.trimly.api.dto.response.UrlResponse;

import java.util.List;
import java.util.Map;

public interface UrlService {

    UrlResponse createUrl(CreateUrlRequest request, Long userId);

    List<UrlResponse> getUserUrls(Long userId, List<String> tags, String linkType, String qrAttachment);

    List<String> getDistinctTags(Long userId);

    Map<String, Object> bulkUpdateTags(Long userId, BulkTagsRequest request);

    Map<String, Object> bulkHideUrls(Long userId, BulkHideRequest request);

    UrlResponse promoteToLink(String code, Long userId);

    UrlResponse editBackHalf(String code, Long userId, EditBackHalfRequest request);

    UrlResponse getUrlMetadata(String code);

    Map<String, Object> verifyPassword(String code, String password);

    UrlResponse updateUrl(String code, UpdateUrlRequest request, Long userId);

    Map<String, Object> deleteUrl(String code, Long userId);
}
