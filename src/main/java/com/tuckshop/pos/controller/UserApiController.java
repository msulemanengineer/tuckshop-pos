package com.tuckshop.pos.controller;

import com.tuckshop.pos.dto.ApiError;
import com.tuckshop.pos.dto.CreateUserRequest;
import com.tuckshop.pos.model.AppUser;
import com.tuckshop.pos.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Every endpoint here is owner-only, enforced both by SecurityConfig (/api/users/**)
// and again here with @PreAuthorize as a second layer of defense.
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('OWNER')")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<AppUser> all() {
        return userService.all();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateUserRequest request) {
        try {
            return ResponseEntity.ok(userService.create(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    @PostMapping("/{id}/active")
    public AppUser setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return userService.setActive(id, Boolean.TRUE.equals(body.get("active")));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            userService.resetPassword(id, body.get("password"));
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }
}
