

// ─────────────────────────────────────────────────────────
// FILE: dto/ChangeRoleRequest.java  (admin promotes/demotes users)
// ─────────────────────────────────────────────────────────
package com.kowriWeb.KworiWebSite.dto;

import com.kowriWeb.KworiWebSite.entity.Role;
import lombok.Data;

@Data
public class ChangeRoleRequest {
    private Role role;
}