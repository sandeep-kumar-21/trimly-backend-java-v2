package com.trimly.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCampaignLinksRequest {

    @NotBlank(message = "destinationUrl is required")
    @URL(message = "Invalid URL format. Must start with http:// or https://")
    private String destinationUrl;

    private String title;

    @NotEmpty(message = "At least one channel must be specified")
    private List<String> channels;

    @Builder.Default
    private Boolean autoUtm = true;

    private String customAliasPrefix;
}
