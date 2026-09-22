package com.trimly.api.service.impl;

import com.trimly.api.dto.request.AddCampaignLinksRequest;
import com.trimly.api.dto.request.AssignExistingLinksRequest;
import com.trimly.api.dto.request.CreateCampaignRequest;
import com.trimly.api.dto.request.UpdateCampaignRequest;
import com.trimly.api.dto.response.CampaignDetailsResponse;
import com.trimly.api.dto.response.CampaignDetailsResponse.ChannelGroup;
import com.trimly.api.dto.response.CampaignResponse;
import com.trimly.api.dto.response.UrlResponse;
import com.trimly.api.exception.BadRequestException;
import com.trimly.api.exception.ConflictException;
import com.trimly.api.exception.ForbiddenException;
import com.trimly.api.exception.ResourceNotFoundException;
import com.trimly.api.model.entity.Campaign;
import com.trimly.api.model.entity.Url;
import com.trimly.api.repository.CampaignRepository;
import com.trimly.api.repository.UrlRepository;
import com.trimly.api.service.CampaignService;
import com.trimly.api.util.Base62Util;
import com.trimly.api.util.TitleScraperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final UrlRepository urlRepository;

    @Value("${app.base-url:http://localhost:4000}")
    private String baseUrl;

    @Override
    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request, Long userId) {
        List<String> channels = request.getChannels() != null && !request.getChannels().isEmpty()
                ? request.getChannels()
                : List.of("email", "social", "sms", "paid");

        Campaign campaign = Campaign.builder()
                .userId(userId)
                .name(request.getName().trim())
                .description(request.getDescription())
                .channels(channels)
                .build();

        Campaign saved = campaignRepository.save(campaign);
        log.info("Created campaign {} for user ID: {}", saved.getId(), userId);
        return CampaignResponse.fromEntity(saved, 0L, 0L, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignResponse> getUserCampaigns(Long userId) {
        List<Campaign> campaigns = campaignRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return campaigns.stream().map(c -> {
            List<Url> links = urlRepository.findByCampaignId(c.getId());
            long totalLinks = links.size();
            long totalClicks = links.stream().mapToLong(Url::getClickCount).sum();

            Map<String, Object> topChannel = computeTopChannel(links);
            return CampaignResponse.fromEntity(c, totalLinks, totalClicks, topChannel);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserChannels(Long userId) {
        return campaignRepository.findDistinctChannelsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignDetailsResponse getCampaignDetails(Long campaignId, Long userId) {
        Campaign campaign = getCampaignOrThrow(campaignId);
        verifyOwnership(campaign, userId);

        List<Url> links = urlRepository.findByCampaignId(campaignId);
        long totalClicks = links.stream().mapToLong(Url::getClickCount).sum();
        long totalLinks = links.size();
        Map<String, Object> topChannel = computeTopChannel(links);

        // Group links by channel
        Map<String, List<Url>> groupedByChannel = links.stream()
                .collect(Collectors.groupingBy(u -> u.getChannel() != null ? u.getChannel() : "general"));

        List<ChannelGroup> channelGroups = new ArrayList<>();
        for (Map.Entry<String, List<Url>> entry : groupedByChannel.entrySet()) {
            String ch = entry.getKey();
            List<Url> chLinks = entry.getValue();
            long chClicks = chLinks.stream().mapToLong(Url::getClickCount).sum();
            long chCount = chLinks.size();
            double pct = totalClicks > 0 ? ((double) chClicks / totalClicks) * 100.0 : 0.0;

            List<UrlResponse> mappedLinks = chLinks.stream()
                    .map(u -> UrlResponse.fromEntity(u, baseUrl))
                    .toList();

            channelGroups.add(new ChannelGroup(ch, chClicks, chCount, Math.round(pct * 10.0) / 10.0, mappedLinks));
        }

        CampaignResponse campaignResp = CampaignResponse.fromEntity(campaign, totalLinks, totalClicks, topChannel);

        return CampaignDetailsResponse.builder()
                .campaign(campaignResp)
                .totalClicks(totalClicks)
                .totalLinks(totalLinks)
                .topChannel(topChannel)
                .channels(channelGroups)
                .build();
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaign(Long campaignId, UpdateCampaignRequest request, Long userId) {
        Campaign campaign = getCampaignOrThrow(campaignId);
        verifyOwnership(campaign, userId);

        if (request.getName() != null) campaign.setName(request.getName().trim());
        if (request.getDescription() != null) campaign.setDescription(request.getDescription().trim());
        if (request.getChannels() != null) campaign.setChannels(request.getChannels());

        Campaign updated = campaignRepository.save(campaign);
        return CampaignResponse.fromEntity(updated, 0L, 0L, null);
    }

    @Override
    @Transactional
    public void deleteCampaign(Long campaignId, Long userId) {
        Campaign campaign = getCampaignOrThrow(campaignId);
        verifyOwnership(campaign, userId);

        // Unlink associated URLs
        List<Url> links = urlRepository.findByCampaignId(campaignId);
        for (Url link : links) {
            link.setCampaignId(null);
            urlRepository.save(link);
        }

        campaignRepository.delete(campaign);
        log.info("Deleted campaign ID: {} and unlinked {} URLs", campaignId, links.size());
    }

    @Override
    @Transactional
    public List<UrlResponse> addCampaignLinksBatch(Long campaignId, AddCampaignLinksRequest request, Long userId) {
        Campaign campaign = getCampaignOrThrow(campaignId);
        verifyOwnership(campaign, userId);

        String title = request.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = TitleScraperUtil.scrapeTitle(request.getDestinationUrl());
        }

        List<UrlResponse> createdLinks = new ArrayList<>();

        for (String channel : request.getChannels()) {
            String shortCode;
            boolean isCustom = false;

            if (request.getCustomAliasPrefix() != null && !request.getCustomAliasPrefix().trim().isEmpty()) {
                String candidate = request.getCustomAliasPrefix().trim() + "-" + channel;
                if (!urlRepository.existsByShortCode(candidate)) {
                    shortCode = candidate;
                    isCustom = true;
                } else {
                    shortCode = Base62Util.encode(urlRepository.getNextUrlSequenceValue());
                }
            } else {
                shortCode = Base62Util.encode(urlRepository.getNextUrlSequenceValue());
            }

            String utmSource = Boolean.TRUE.equals(request.getAutoUtm()) ? channel : null;
            String utmMedium = Boolean.TRUE.equals(request.getAutoUtm()) ? channel : null;
            String utmCampaign = Boolean.TRUE.equals(request.getAutoUtm()) ? campaign.getName() : null;

            Url url = Url.builder()
                    .shortCode(shortCode)
                    .longUrl(request.getDestinationUrl().trim())
                    .userId(userId)
                    .campaignId(campaignId)
                    .channel(channel)
                    .title(title)
                    .utmSource(utmSource)
                    .utmMedium(utmMedium)
                    .utmCampaign(utmCampaign)
                    .isCustomAlias(isCustom)
                    .build();

            Url saved = urlRepository.save(url);
            createdLinks.add(UrlResponse.fromEntity(saved, baseUrl));
        }

        return createdLinks;
    }

    @Override
    @Transactional
    public Map<String, Object> assignExistingLinks(Long campaignId, AssignExistingLinksRequest request, Long userId) {
        Campaign campaign = getCampaignOrThrow(campaignId);
        verifyOwnership(campaign, userId);

        int assigned = 0;
        for (String linkIdStr : request.getLinkIds()) {
            try {
                Long linkId = Long.parseLong(linkIdStr);
                Optional<Url> urlOpt = urlRepository.findById(linkId);
                if (urlOpt.isPresent()) {
                    Url url = urlOpt.get();
                    if (userId.equals(url.getUserId())) {
                        url.setCampaignId(campaignId);
                        if (request.getChannel() != null) {
                            url.setChannel(request.getChannel());
                        }
                        urlRepository.save(url);
                        assigned++;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        return Map.of("success", true, "assigned", assigned);
    }

    @Override
    @Transactional
    public void unlinkLinkFromCampaign(Long campaignId, Long linkId, Long userId) {
        Campaign campaign = getCampaignOrThrow(campaignId);
        verifyOwnership(campaign, userId);

        Url url = urlRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

        if (!campaignId.equals(url.getCampaignId())) {
            throw new BadRequestException("Link is not associated with this campaign");
        }

        url.setCampaignId(null);
        urlRepository.save(url);
    }

    private Campaign getCampaignOrThrow(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
    }

    private void verifyOwnership(Campaign campaign, Long userId) {
        if (!campaign.getUserId().equals(userId)) {
            throw new ForbiddenException("Forbidden: You do not own this campaign");
        }
    }

    private Map<String, Object> computeTopChannel(List<Url> links) {
        Map<String, Long> channelClicks = new HashMap<>();
        for (Url link : links) {
            String ch = link.getChannel() != null ? link.getChannel() : "general";
            channelClicks.put(ch, channelClicks.getOrDefault(ch, 0L) + link.getClickCount());
        }

        return channelClicks.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> Map.<String, Object>of("channel", e.getKey(), "clicks", e.getValue()))
                .orElse(null);
    }
}
