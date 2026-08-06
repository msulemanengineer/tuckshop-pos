package com.tuckshop.pos.service;

import com.tuckshop.pos.licensing.LicenseUtil;
import com.tuckshop.pos.model.LicenseInfo;
import com.tuckshop.pos.repository.LicenseRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class LicenseService {

    private final LicenseRepository licenseRepository;
    private final ActivityLogService activityLogService;

    @Value("${license.secret}")
    private String secret;

    @Value("${license.trial-days:30}")
    private int trialDays;

    public LicenseService(LicenseRepository licenseRepository, ActivityLogService activityLogService) {
        this.licenseRepository = licenseRepository;
        this.activityLogService = activityLogService;
    }

    // Runs once, on the very first startup on a given install - generates the device ID
    // and starts the trial clock so the shop can use the system right away.
    @PostConstruct
    @Transactional
    public void ensureInitialized() {
        if (licenseRepository.existsById(1L)) {
            return;
        }
        LicenseInfo info = new LicenseInfo();
        info.setDeviceId(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        info.setExpiresOn(LocalDate.now().plusDays(trialDays));
        licenseRepository.save(info);
    }

    public LicenseInfo current() {
        return licenseRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("License record missing - restart the app."));
    }

    public boolean isValid() {
        LicenseInfo info = current();
        return info.getExpiresOn() != null && !LocalDate.now().isAfter(info.getExpiresOn());
    }

    public long daysRemaining() {
        LicenseInfo info = current();
        if (info.getExpiresOn() == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), info.getExpiresOn());
    }

    @Transactional
    public LicenseInfo activate(String key) {
        LicenseInfo info = current();
        LocalDate newExpiry = LicenseUtil.verifyAndGetExpiry(info.getDeviceId(), key, secret);
        if (newExpiry == null) {
            throw new IllegalArgumentException("That license key isn't valid for this install.");
        }
        info.setActiveKey(key.trim().toUpperCase());
        info.setExpiresOn(newExpiry);
        info.setActivatedAt(java.time.LocalDateTime.now());
        LicenseInfo saved = licenseRepository.save(info);
        activityLogService.log("LICENSE_ACTIVATED", "License activated, valid until " + newExpiry);
        return saved;
    }

    /** Message shown to a cashier/owner when checkout is blocked - never blocks viewing existing data. */
    public String blockedMessage() {
        return "This system's license has expired. New sales are paused until it's renewed - "
                + "contact your system provider with the Device ID on the License page.";
    }
}

// package com.tuckshop.pos.service;

// import com.tuckshop.pos.licensing.LicenseUtil;
// import com.tuckshop.pos.model.LicenseInfo;
// import com.tuckshop.pos.repository.LicenseRepository;
// import jakarta.annotation.PostConstruct;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.time.LocalDateTime;
// import java.time.temporal.ChronoUnit;
// import java.util.UUID;

// @Service
// public class LicenseService {

//     private final LicenseRepository licenseRepository;
//     private final ActivityLogService activityLogService;

//     @Value("${license.secret}")
//     private String secret;

//     @Value("${license.trial-minutes:30}")
//     private int trialMinutes;

//     public LicenseService(
//             LicenseRepository licenseRepository,
//             ActivityLogService activityLogService
//     ) {
//         this.licenseRepository = licenseRepository;
//         this.activityLogService = activityLogService;
//     }

//     @PostConstruct
//     @Transactional
//     public void ensureInitialized() {

//         if (licenseRepository.existsById(1L)) {
//             return;
//         }

//         LicenseInfo info = new LicenseInfo();

//         info.setDeviceId(
//                 UUID.randomUUID()
//                         .toString()
//                         .substring(0, 8)
//                         .toUpperCase()
//         );

//         // Trial expires after X minutes
//         info.setExpiresOn(
//                 LocalDateTime.now().plusMinutes(trialMinutes)
//         );

//         licenseRepository.save(info);
//     }

//     public LicenseInfo current() {
//         return licenseRepository.findById(1L)
//                 .orElseThrow(() ->
//                         new IllegalStateException(
//                                 "License record missing - restart the app."
//                         )
//                 );
//     }

//     public boolean isValid() {
//         LicenseInfo info = current();

//         return info.getExpiresOn() != null
//                 && !LocalDateTime.now().isAfter(info.getExpiresOn());
//     }

//     public long minutesRemaining() {
//         LicenseInfo info = current();

//         if (info.getExpiresOn() == null) {
//             return 0;
//         }

//         return ChronoUnit.MINUTES.between(
//                 LocalDateTime.now(),
//                 info.getExpiresOn()
//         );
//     }

//     @Transactional
//     public LicenseInfo activate(String key) {

//         LicenseInfo info = current();

//         LocalDateTime newExpiry =
//                 LicenseUtil.verifyAndGetExpiry(
//                         info.getDeviceId(),
//                         key,
//                         secret
//                 );

//         if (newExpiry == null) {
//             throw new IllegalArgumentException(
//                     "That license key isn't valid for this install."
//             );
//         }

//         info.setActiveKey(key.trim().toUpperCase());
//         info.setExpiresOn(newExpiry);
//         info.setActivatedAt(LocalDateTime.now());

//         LicenseInfo saved = licenseRepository.save(info);

//         activityLogService.log(
//                 "LICENSE_ACTIVATED",
//                 "License activated, valid until " + newExpiry
//         );

//         return saved;
//     }

//     public String blockedMessage() {
//         return "This system's license has expired. New sales are paused until it's renewed - "
//                 + "contact your system provider with the Device ID on the License page.";
//     }
// }