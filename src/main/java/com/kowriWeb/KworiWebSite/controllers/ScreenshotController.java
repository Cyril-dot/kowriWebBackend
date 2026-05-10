package com.kowriWeb.KworiWebSite.controllers;

import com.kowriWeb.KworiWebSite.services.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/screenshots")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class ScreenshotController {

    private final CloudinaryService cloudinaryService;

    /**
     * GET /api/admin/screenshots/all
     * Returns every deposit proof screenshot ever uploaded.
     */
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllScreenshots() throws Exception {
        return ResponseEntity.ok(cloudinaryService.getAllScreenshots());
    }

    /**
     * GET /api/admin/screenshots/daily
     * Returns screenshots uploaded today (UTC).
     */
    @GetMapping("/daily")
    public ResponseEntity<List<Map<String, Object>>> getDailyScreenshots() throws Exception {
        return ResponseEntity.ok(cloudinaryService.getDailyScreenshots());
    }

    /**
     * GET /api/admin/screenshots/weekly
     * Returns screenshots uploaded this week, Monday → now (UTC).
     */
    @GetMapping("/weekly")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyScreenshots() throws Exception {
        return ResponseEntity.ok(cloudinaryService.getWeeklyScreenshots());
    }
}