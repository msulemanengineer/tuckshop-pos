package com.tuckshop.pos.controller;

import com.tuckshop.pos.dto.ApiError;
import com.tuckshop.pos.model.LicenseInfo;
import com.tuckshop.pos.service.LicenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Owner-only - a cashier should never see the device ID or be able to try activation codes.
@RestController
@RequestMapping("/api/license")
@PreAuthorize("hasRole('OWNER')")
public class LicenseApiController {

    private final LicenseService licenseService;

    public LicenseApiController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @GetMapping
    public Map<String, Object> status() {
        LicenseInfo info = licenseService.current();
        return Map.of(
                "deviceId", info.getDeviceId(),
                "expiresOn", info.getExpiresOn(),
                "valid", licenseService.isValid(),
                "daysRemaining", licenseService.daysRemaining()
        );
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody Map<String, String> body) {
        try {
            LicenseInfo info = licenseService.activate(body.get("key"));
            return ResponseEntity.ok(Map.of("expiresOn", info.getExpiresOn()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }
}


// package com.tuckshop.pos.controller;

// import com.tuckshop.pos.dto.ApiError;
// import com.tuckshop.pos.model.LicenseInfo;
// import com.tuckshop.pos.service.LicenseService;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.util.Map;

// // Owner-only - a cashier should never see the device ID or be able to try activation codes.
// @RestController
// @RequestMapping("/api/license")
// @PreAuthorize("hasRole('OWNER')")
// public class LicenseApiController {

//     private final LicenseService licenseService;

//     public LicenseApiController(LicenseService licenseService) {
//         this.licenseService = licenseService;
//     }

//     @GetMapping
//     public Map<String, Object> status() {
//         LicenseInfo info = licenseService.current();

//         return Map.of(
//                 "deviceId", info.getDeviceId(),
//                 "expiresOn", info.getExpiresOn(),
//                 "valid", licenseService.isValid(),
//                 "minutesRemaining", licenseService.minutesRemaining()
//         );
//     }

//     @PostMapping("/activate")
//     public ResponseEntity<?> activate(
//             @RequestBody Map<String, String> body
//     ) {
//         try {
//             LicenseInfo info = licenseService.activate(body.get("key"));

//             return ResponseEntity.ok(
//                     Map.of(
//                             "expiresOn", info.getExpiresOn()
//                     )
//             );

//         } catch (IllegalArgumentException e) {
//             return ResponseEntity
//                     .badRequest()
//                     .body(new ApiError(e.getMessage()));
//         }
//     }
// }
