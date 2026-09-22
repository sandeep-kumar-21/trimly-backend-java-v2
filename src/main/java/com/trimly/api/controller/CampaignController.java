package com.trimly.api.controller;

import com.trimly.api.dto.request.AddCampaignLinksRequest;
import com.trimly.api.dto.request.AssignExistingLinksRequest;
import com.trimly.api.dto.request.CreateCampaignRequest;
import com.trimly.api.dto.request.UpdateCampaignRequest;
import com.trimly.api.dto.response.CampaignDetailsResponse;
import com.trimly.api.dto.response.CampaignResponse;
import com.trimly.api.dto.response.UrlResponse;
import com.trimly.api.exception.UnauthorizedException;
import com.trimly.api.security.SecurityUtils;
import com.trimly.api.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Campaigns", description = "Marketing campaigns and batch multi-channel UTM generator")
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    @Operation(summary = "Create a new campaign")
    @ApiResponse(responseCode = "201", description = "Campaign created successfully.")
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        Long userId = getCurrentUserId();
        CampaignResponse response = campaignService.createCampaign(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all campaigns created by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Returns list of user campaigns with aggregated metrics.")
    public ResponseEntity<List<CampaignResponse>> getUserCampaigns() {
        Long userId = getCurrentUserId();
        List<CampaignResponse> response = campaignService.getUserCampaigns(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/channels/all")
    @Operation(summary = "Get all distinct marketing channels used by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Returns array of channel names.")
    public ResponseEntity<List<String>> getUserChannels() {
        Long userId = getCurrentUserId();
        List<String> response = campaignService.getUserChannels(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get campaign details and grouped links by channel")
    @ApiResponse(responseCode = "200", description = "Returns campaign details and channel link analytics.")
    public ResponseEntity<CampaignDetailsResponse> getCampaignDetails(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        CampaignDetailsResponse response = campaignService.getCampaignDetails(id, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update campaign name, description, or channels")
    @ApiResponse(responseCode = "200", description = "Campaign updated successfully.")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable Long id,
            @RequestBody UpdateCampaignRequest request) {
        Long userId = getCurrentUserId();
        CampaignResponse response = campaignService.updateCampaign(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete campaign and unlink associated short URLs")
    @ApiResponse(responseCode = "200", description = "Campaign deleted successfully.")
    public ResponseEntity<Map<String, String>> deleteCampaign(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        campaignService.deleteCampaign(id, userId);
        return ResponseEntity.ok(Map.of("message", "Campaign deleted successfully"));
    }

    @PostMapping("/{id}/links")
    @Operation(summary = "Batch generate short links across selected marketing channels with automated UTM tracking")
    @ApiResponse(responseCode = "201", description = "Multi-channel links generated successfully.")
    public ResponseEntity<List<UrlResponse>> addCampaignLinksBatch(
            @PathVariable Long id,
            @Valid @RequestBody AddCampaignLinksRequest request) {
        Long userId = getCurrentUserId();
        List<UrlResponse> response = campaignService.addCampaignLinksBatch(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/assign-links")
    @Operation(summary = "Assign existing short links to this campaign")
    @ApiResponse(responseCode = "200", description = "Links assigned to campaign successfully.")
    public ResponseEntity<Map<String, Object>> assignExistingLinks(
            @PathVariable Long id,
            @Valid @RequestBody AssignExistingLinksRequest request) {
        Long userId = getCurrentUserId();
        Map<String, Object> response = campaignService.assignExistingLinks(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/links/{linkId}")
    @Operation(summary = "Unlink a short link from this campaign")
    @ApiResponse(responseCode = "200", description = "Link unlinked from campaign successfully.")
    public ResponseEntity<Map<String, String>> unlinkLinkFromCampaign(
            @PathVariable Long id,
            @PathVariable Long linkId) {
        Long userId = getCurrentUserId();
        campaignService.unlinkLinkFromCampaign(id, linkId, userId);
        return ResponseEntity.ok(Map.of("message", "Link unlinked successfully"));
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Unauthorized: Login required"));
    }
}
