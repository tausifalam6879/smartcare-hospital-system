package com.raahmediq.navigation.repository;

import com.raahmediq.navigation.domain.NavigationPath;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NavigationPathRepository extends JpaRepository<NavigationPath, UUID> {
    List<NavigationPath> findAllByHospitalIdAndActiveTrue(UUID hospitalId);
}
