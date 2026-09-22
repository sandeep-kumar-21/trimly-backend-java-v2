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
public class BulkTagsRequest {

    @NotEmpty(message = "linkIds cannot be empty")
    private List<String> linkIds;

    @NotEmpty(message = "tags cannot be empty")
    private List<String> tags;

    @Builder.Default
    private String action = "add"; // "add", "remove", "replace"
}
