package com.tuckshop.pos.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Single-row table: one device ID per install, one active license key at a time. */
@Entity
@Table(name = "license_info")
public class LicenseInfo {

    @Id
    private Long id = 1L;

    @Column(nullable = false, unique = true)
    private String deviceId;

    private String activeKey;

    private LocalDate expiresOn;

    private LocalDateTime activatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getActiveKey() {
        return activeKey;
    }

    public void setActiveKey(String activeKey) {
        this.activeKey = activeKey;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }
}

// package com.tuckshop.pos.model;

// import jakarta.persistence.*;

// import java.time.LocalDateTime;

// /** Single-row table: one device ID per install, one active license key at a time. */
// @Entity
// @Table(name = "license_info")
// public class LicenseInfo {

//     @Id
//     private Long id = 1L;

//     @Column(nullable = false, unique = true)
//     private String deviceId;

//     private String activeKey;

//     private LocalDateTime expiresOn;

//     private LocalDateTime activatedAt;

//     public Long getId() {
//         return id;
//     }

//     public void setId(Long id) {
//         this.id = id;
//     }

//     public String getDeviceId() {
//         return deviceId;
//     }

//     public void setDeviceId(String deviceId) {
//         this.deviceId = deviceId;
//     }

//     public String getActiveKey() {
//         return activeKey;
//     }

//     public void setActiveKey(String activeKey) {
//         this.activeKey = activeKey;
//     }

//     public LocalDateTime getExpiresOn() {
//         return expiresOn;
//     }

//     public void setExpiresOn(LocalDateTime expiresOn) {
//         this.expiresOn = expiresOn;
//     }

//     public LocalDateTime getActivatedAt() {
//         return activatedAt;
//     }

//     public void setActivatedAt(LocalDateTime activatedAt) {
//         this.activatedAt = activatedAt;
//     }
// }
