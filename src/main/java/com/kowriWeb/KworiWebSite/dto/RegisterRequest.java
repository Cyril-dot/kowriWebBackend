// ─────────────────────────────────────────────────────────
// FILE: dto/RegisterRequest.java   (replaces CreateUserRequest & CreateAdminRequest)
// ─────────────────────────────────────────────────────────
package com.kowriWeb.KworiWebSite.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String phoneNumber;
    // role is NOT accepted from the client — assigned by the endpoint/service
}






// ─────────────────────────────────────────────────────────
// FILE: dto/UserResponse.java      (single response DTO for all roles)
// ─────────────────────────────────────────────────────────


// ─────────────────────────────────────────────────────────
// FILE: dto/UserSummaryResponse.java   (list view, no tokens/balance)
// ─────────────────────────────────────────────────────────




