// ═══════════════════════════════════════════════════════════
// FILE: dto/SendNotificationRequest.java
// ═══════════════════════════════════════════════════════════
package com.kowriWeb.KworiWebSite.dto;

import com.kowriWeb.KworiWebSite.entity.NotificationType;
import lombok.Data;

/**
 * Sent as multipart/form-data so an image file can be included.
 * Fields:
 *   type    → TEXT_ONLY | IMAGE_ONLY | TEXT_IMAGE
 *   message → required for TEXT_ONLY and TEXT_IMAGE; omit for IMAGE_ONLY
 *   image   → required for IMAGE_ONLY and TEXT_IMAGE; omit for TEXT_ONLY
 *             (the actual MultipartFile is handled in the controller)
 */
@Data
public class SendNotificationRequest {
    private NotificationType type;
    private String message;
    // image is bound separately as MultipartFile in the controller
}



