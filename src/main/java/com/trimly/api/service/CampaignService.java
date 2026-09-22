package com.trimly.api.service;

import com.trimly.api.dto.request.AddCampaignLinksRequest;
import com.trimly.api.dto.request.AssignExistingLinksRequest;
import com.trimly.api.dto.request.CreateCampaignRequest;
import com.trimly.api.dto.request.UpdateCampaignRequest;
import com.trimly.api.dto.response.CampaignDetailsResponse;
import com.trimly.api.dto.response.CampaignResponse;
import com.trimly.api.dto.response.UrlResponse;

import java.util.List;
import java.util.Map;

public interface CampaignService {

    CampaignResponse createCampaign(CreateCampaignRequest request, Long userId);

    List<CampaignResponse> getUserCampaigns(Long userId);

    List<String> getUserChannels(Long userId);

    CampaignDetailsResponse getCampaignDetails(Long campaignId, Long userId);

    CampaignResponse updateCampaign(Long campaignId, UpdateCampaignRequest request, Long userId);

    void deleteCampaign(Long campaignId, Long userId);

    List<UrlResponse> addCampaignLinksBatch(Long campaignId, AddCampaignLinksRequest request, Long userId);

    Map<String, Object> assignExistingLinks(Long campaignId, AssignExistingLinksRequest request, Long userId);

    void unlinkLinkFromCampaign(Long campaignId, Long linkId, Long userId);
}
