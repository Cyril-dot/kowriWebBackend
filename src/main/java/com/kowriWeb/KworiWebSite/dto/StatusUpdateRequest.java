package com.kowriWeb.KworiWebSite.dto;

import com.kowriWeb.KworiWebSite.entity.DepositStatus;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class StatusUpdateRequest {
    private DepositStatus status;
    private BigDecimal rewardAmount; // required only when approving a FLEXIBLE deposit
}