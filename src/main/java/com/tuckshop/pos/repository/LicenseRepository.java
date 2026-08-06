package com.tuckshop.pos.repository;

import com.tuckshop.pos.model.LicenseInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseRepository extends JpaRepository<LicenseInfo, Long> {
}
