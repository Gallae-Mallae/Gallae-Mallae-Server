package com.practice.OAuth2.domain.plan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// 친구 초대 코드로 입장
public class PlanJoinRequest {

    @NotBlank
    private String inviteCode;
}
