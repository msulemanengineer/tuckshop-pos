package com.tuckshop.pos.controller;

import com.tuckshop.pos.model.ActivityLog;
import com.tuckshop.pos.service.ActivityLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activity-log")
@PreAuthorize("hasRole('OWNER')")
public class ActivityLogApiController {

    private final ActivityLogService activityLogService;

    public ActivityLogApiController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public List<ActivityLog> recent() {
        return activityLogService.recent();
    }
}
