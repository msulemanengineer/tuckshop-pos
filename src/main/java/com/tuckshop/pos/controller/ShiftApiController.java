package com.tuckshop.pos.controller;

import com.tuckshop.pos.dto.ApiError;
import com.tuckshop.pos.dto.ShiftCloseRequest;
import com.tuckshop.pos.dto.ShiftOpenRequest;
import com.tuckshop.pos.model.Shift;
import com.tuckshop.pos.service.ShiftService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/shifts")
public class ShiftApiController {

    private final ShiftService shiftService;

    public ShiftApiController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @GetMapping("/current")
    public ResponseEntity<?> current(Authentication auth) {
        Optional<Shift> shift = shiftService.currentOpenShift(auth.getName());
        if (shift.isPresent()) {
            return ResponseEntity.ok(shift.get());
        }
        return ResponseEntity.ok(java.util.Map.of("open", false));
    }

    @PostMapping("/open")
    public ResponseEntity<?> open(@RequestBody ShiftOpenRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(shiftService.openShift(auth.getName(), request.getOpeningCash()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    @PostMapping("/close")
    public ResponseEntity<?> close(@RequestBody ShiftCloseRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(shiftService.closeShift(auth.getName(), request.getActualClosingCash()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/history")
    public List<Shift> history() {
        return shiftService.history();
    }
}
