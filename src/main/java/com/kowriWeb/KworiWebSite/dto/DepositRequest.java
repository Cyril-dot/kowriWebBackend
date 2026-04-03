package com.kowriWeb.KworiWebSite.dto;

import com.kowriWeb.KworiWebSite.entity.DepositType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DepositRequest {
    private String tokenId;
    private String secretCode;
    private String walletId;
    private BigDecimal amount;
    private DepositType depositType; // FIXED or FLEXIBLE
}