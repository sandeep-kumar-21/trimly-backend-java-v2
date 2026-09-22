package com.trimly.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditBackHalfRequest {

    @NotBlank(message = "Custom back-half alias is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,50}$", message = "Custom back-half must be 3-50 alphanumeric characters, hyphens, or underscores")
    private String customBackHalf;
}
