package com.kowriWeb.KworiWebSite.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AdminCreditRequest {
    private UUID userId;
    private BigDecimal amount;
}