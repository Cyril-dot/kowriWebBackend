package com.kowriWeb.KworiWebSite.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class UserFinancialSummaryResponse {
    private BigDecimal balance;
    private BigDecimal totalDeposited;
    private BigDecimal totalRewardsEarned;
    private BigDecimal totalWithdrawn;
}