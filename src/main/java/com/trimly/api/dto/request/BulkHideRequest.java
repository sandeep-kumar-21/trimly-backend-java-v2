package com.trimly.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkHideRequest {

    @NotEmpty(message = "linkIds cannot be empty")
    private List<String> linkIds;

    @NotNull(message = "hide flag is required")
    private Boolean hide;
}
