package com.opentraum.auth.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshRequest {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "refreshToken은 필수입니다")
    private String refreshToken;
}
