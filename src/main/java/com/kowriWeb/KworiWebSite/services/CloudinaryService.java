package com.kowriWeb.KworiWebSite.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final String DEPOSIT_FOLDER = "kowri/deposit-proofs";

    // ──────────────────────────────────────────
    // UPLOAD
    // ──────────────────────────────────────────

    public Map uploadImage(MultipartFile file, String folder) throws IOException {
        log.info("Uploading image to Cloudinary folder: {}", folder);
        return cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "image"
                )
        );
    }

    // ──────────────────────────────────────────
    // DELETE
    // ──────────────────────────────────────────

    public void deleteImage(String publicId) throws IOException {
        log.info("Deleting image from Cloudinary: {}", publicId);
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    // ──────────────────────────────────────────
    // ALL SCREENSHOTS in the deposit-proofs folder
    // ──────────────────────────────────────────

    public List<Map<String, Object>> getAllScreenshots() throws Exception {
        log.info("Fetching all deposit screenshots from Cloudinary");

        ApiResponse response = cloudinary.api().resources(
                ObjectUtils.asMap(
                        "type",        "upload",
                        "prefix",      DEPOSIT_FOLDER,
                        "max_results", 500,
                        "tags",        true
                )
        );

        return extractResources(response);
    }

    // ──────────────────────────────────────────
    // TODAY'S SCREENSHOTS
    // ──────────────────────────────────────────

    public List<Map<String, Object>> getDailyScreenshots() throws Exception {
        log.info("Fetching today's deposit screenshots from Cloudinary");

        // Start of today in UTC as a Unix timestamp
        long startOfDay = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay(ZoneOffset.UTC)
                .toEpochSecond();

        ApiResponse response = cloudinary.api().resources(
                ObjectUtils.asMap(
                        "type",        "upload",
                        "prefix",      DEPOSIT_FOLDER,
                        "max_results", 500,
                        "tags",        true
                )
        );

        // Cloudinary doesn't filter by date server-side on the basic plan,
        // so we filter the returned list by created_at ourselves
        return extractResources(response).stream()
                .filter(r -> {
                    Object createdAt = r.get("created_at");
                    if (createdAt == null) return false;
                    // created_at is an ISO-8601 string e.g. "2025-05-10T14:23:00Z"
                    long uploadedAt = java.time.Instant.parse(createdAt.toString()).getEpochSecond();
                    return uploadedAt >= startOfDay;
                })
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────
    // THIS WEEK'S SCREENSHOTS  (Mon → now)
    // ──────────────────────────────────────────

    public List<Map<String, Object>> getWeeklyScreenshots() throws Exception {
        log.info("Fetching this week's deposit screenshots from Cloudinary");

        // Start of current ISO week (Monday) in UTC
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);

        long startOfWeek = monday
                .atStartOfDay(ZoneOffset.UTC)
                .toEpochSecond();

        ApiResponse response = cloudinary.api().resources(
                ObjectUtils.asMap(
                        "type",        "upload",
                        "prefix",      DEPOSIT_FOLDER,
                        "max_results", 500,
                        "tags",        true
                )
        );

        return extractResources(response).stream()
                .filter(r -> {
                    Object createdAt = r.get("created_at");
                    if (createdAt == null) return false;
                    long uploadedAt = java.time.Instant.parse(createdAt.toString()).getEpochSecond();
                    return uploadedAt >= startOfWeek;
                })
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────
    // PRIVATE HELPER
    // ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractResources(ApiResponse response) {
        List<Map<String, Object>> resources = (List<Map<String, Object>>) response.get("resources");
        if (resources == null) return List.of();

        // Return only the fields the frontend/admin actually needs
        return resources.stream()
                .map(r -> Map.<String, Object>of(
                        "publicId",   r.getOrDefault("public_id",   ""),
                        "secureUrl",  r.getOrDefault("secure_url",  ""),
                        "format",     r.getOrDefault("format",      ""),
                        "bytes",      r.getOrDefault("bytes",       0),
                        "createdAt",  r.getOrDefault("created_at",  ""),
                        "width",      r.getOrDefault("width",       0),
                        "height",     r.getOrDefault("height",      0)
                ))
                .collect(Collectors.toList());
    }
}