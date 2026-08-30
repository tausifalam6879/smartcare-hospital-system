package com.raahmediq.navigation.repository;

import com.raahmediq.navigation.domain.HospitalLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HospitalLocationRepository extends JpaRepository<HospitalLocation, UUID> {
    List<HospitalLocation> findAllByHospitalIdAndActiveTrueOrderByFloorLabelAscNameEnAsc(UUID hospitalId);
    Optional<HospitalLocation> findByHospitalIdAndCodeIgnoreCase(UUID hospitalId, String code);
    Optional<HospitalLocation> findFirstByHospitalIdAndBuildingIgnoreCaseAndFloorLabelIgnoreCaseAndRoomNumberIgnoreCase(
            UUID hospitalId, String building, String floorLabel, String roomNumber);
}
