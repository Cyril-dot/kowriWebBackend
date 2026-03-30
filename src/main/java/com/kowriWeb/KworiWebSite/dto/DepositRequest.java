// ─────────────────────────────────────────────────────────────────────
// File: dto/DepositRequest.java
// Sent by the user when submitting a deposit (multipart form fields)
// ─────────────────────────────────────────────────────────────────────
package com.kowriWeb.KworiWebSite.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DepositRequest {
    private String tokenId;
    private String secretCode;
    private String walletId;
    private BigDecimal amount;
    // The proof screenshot is sent as a MultipartFile — handled separately
    // in the controller via @RequestPart
}
