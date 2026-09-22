package com.trimly.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignExistingLinksRequest {

    @NotEmpty(message = "linkIds cannot be empty")
    private List<String> linkIds;

    private String channel;
}
