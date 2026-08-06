package com.tuckshop.pos.service;

import com.tuckshop.pos.model.ActivityLog;
import com.tuckshop.pos.repository.ActivityLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void log(String action, String details) {
        String username = currentUsername();
        activityLogRepository.save(new ActivityLog(username, action, details));
    }

    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    public List<ActivityLog> recent() {
        return activityLogRepository.findTop50ByOrderByCreatedAtDesc();
    }
}
