package com.raahmediq.navigation.service;

import com.raahmediq.appointment.domain.Appointment;
import com.raahmediq.appointment.repository.AppointmentRepository;
import com.raahmediq.audit.service.AuditService;
import com.raahmediq.common.error.ConflictException;
import com.raahmediq.common.error.NotFoundException;
import com.raahmediq.hospital.domain.Hospital;
import com.raahmediq.hospital.repository.HospitalRepository;
import com.raahmediq.navigation.domain.HospitalLocation;
import com.raahmediq.navigation.domain.LocationType;
import com.raahmediq.navigation.domain.NavigationPath;
import com.raahmediq.navigation.domain.QrCheckpoint;
import com.raahmediq.navigation.repository.HospitalLocationRepository;
import com.raahmediq.navigation.repository.NavigationPathRepository;
import com.raahmediq.navigation.repository.QrCheckpointRepository;
import com.raahmediq.navigation.web.NavigationDtos.AppointmentDestinationResponse;
import com.raahmediq.navigation.web.NavigationDtos.CheckpointRequest;
import com.raahmediq.navigation.web.NavigationDtos.CheckpointResponse;
import com.raahmediq.navigation.web.NavigationDtos.HospitalMapResponse;
import com.raahmediq.navigation.web.NavigationDtos.LocationRequest;
import com.raahmediq.navigation.web.NavigationDtos.LocationResponse;
import com.raahmediq.navigation.web.NavigationDtos.PathRequest;
import com.raahmediq.navigation.web.NavigationDtos.RouteResponse;
import com.raahmediq.navigation.web.NavigationDtos.RouteStep;
import com.raahmediq.patient.domain.Patient;
import com.raahmediq.patient.repository.PatientRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

@Service
public class NavigationService {

    private final HospitalRepository hospitals;
    private final HospitalLocationRepository locations;
    private final NavigationPathRepository paths;
    private final QrCheckpointRepository checkpoints;
    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final AuditService audit;

    public NavigationService(HospitalRepository hospitals, HospitalLocationRepository locations,
                             NavigationPathRepository paths, QrCheckpointRepository checkpoints,
                             AppointmentRepository appointments, PatientRepository patients, AuditService audit) {
        this.hospitals = hospitals;
        this.locations = locations;
        this.paths = paths;
        this.checkpoints = checkpoints;
        this.appointments = appointments;
        this.patients = patients;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public HospitalMapResponse map(UUID hospitalId) {
        Hospital hospital = requireHospital(hospitalId);
        List<LocationResponse> locationResponses = locations
                .findAllByHospitalIdAndActiveTrueOrderByFloorLabelAscNameEnAsc(hospitalId).stream()
                .map(NavigationService::toResponse).toList();
        List<CheckpointResponse> checkpointResponses = checkpoints
                .findAllByHospitalIdAndActiveTrueOrderByLabelEnAsc(hospitalId).stream()
                .map(NavigationService::toResponse).toList();
        return new HospitalMapResponse(hospital.getId(), hospital.getName(), locationResponses, checkpointResponses);
    }

    @Transactional(readOnly = true)
    public CheckpointResponse checkpoint(String publicCode) {
        return toResponse(requireCheckpoint(publicCode));
    }

    @Transactional(readOnly = true)
    public RouteResponse route(UUID hospitalId, String fromCheckpoint, String destinationCode,
                               String languageValue, boolean stepFree) {
        Hospital hospital = requireHospital(hospitalId);
        QrCheckpoint checkpoint = requireCheckpoint(fromCheckpoint);
        if (!checkpoint.getHospital().getId().equals(hospitalId)) {
            throw new ConflictException("The QR checkpoint belongs to a different hospital.");
        }
        HospitalLocation destination = requireLocation(hospitalId, destinationCode);
        String language = normalizedLanguage(languageValue);
        return shortestRoute(hospital, checkpoint.getLocation(), destination, language, stepFree);
    }

    @Transactional(readOnly = true)
    public AppointmentDestinationResponse appointmentDestination(UUID userId, UUID appointmentId) {
        Patient patient = patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required."));
        Appointment appointment = appointments.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment was not found."));
        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new AccessDeniedException("Appointment belongs to another patient.");
        }
        String building = appointment.getDoctor().getBuilding();
        String floorLabel = appointment.getDoctor().getFloorLabel();
        String roomNumber = appointment.getDoctor().getRoomNumber();
        boolean hasRoom = building != null && !building.isBlank() && floorLabel != null && !floorLabel.isBlank()
                && roomNumber != null && !roomNumber.isBlank();
        HospitalLocation exact = hasRoom ? locations
                .findFirstByHospitalIdAndBuildingIgnoreCaseAndFloorLabelIgnoreCaseAndRoomNumberIgnoreCase(
                        appointment.getHospital().getId(), building, floorLabel, roomNumber).orElse(null) : null;
        List<HospitalLocation> hospitalLocations = locations
                .findAllByHospitalIdAndActiveTrueOrderByFloorLabelAscNameEnAsc(appointment.getHospital().getId());
        HospitalLocation destination = exact != null ? exact : hospitalLocations.stream()
                .filter(location -> location.getType() == LocationType.RECEPTION)
                .filter(location -> building != null && location.getBuilding().equalsIgnoreCase(building))
                .findFirst()
                .or(() -> hospitalLocations.stream().filter(location -> location.getType() == LocationType.RECEPTION).findFirst())
                .or(() -> hospitalLocations.stream().filter(location -> location.getType() == LocationType.REGISTRATION).findFirst())
                .or(() -> hospitalLocations.stream().filter(location -> location.getType() == LocationType.ENTRANCE).findFirst())
                .orElseThrow(() -> new NotFoundException("This hospital has not published a verified indoor map."));
        boolean exactRoomMatch = exact != null;
        String guidanceEn = exactRoomMatch
                ? "Verified route to the doctor's room."
                : "The doctor's room is not verified on this map yet. The route ends at a verified help desk; hospital staff will guide you from there.";
        String guidanceHi = exactRoomMatch
                ? "डॉक्टर के कमरे तक सत्यापित रास्ता।"
                : "डॉक्टर का कमरा अभी इस मानचित्र पर सत्यापित नहीं है। रास्ता सत्यापित सहायता डेस्क तक जाता है; वहाँ से अस्पताल स्टाफ मार्गदर्शन करेगा।";
        return new AppointmentDestinationResponse(appointmentId, appointment.getHospital().getId(),
                appointment.getHospital().getName(), appointment.getDoctor().getName(), toResponse(destination),
                exactRoomMatch, guidanceEn, guidanceHi);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    public LocationResponse createLocation(UUID hospitalId, LocationRequest request) {
        Hospital hospital = requireHospital(hospitalId);
        String code = normalizedCode(request.code());
        locations.findByHospitalIdAndCodeIgnoreCase(hospitalId, code).ifPresent(found -> {
            throw new ConflictException("This hospital already uses location code " + code + ".");
        });
        HospitalLocation location = locations.save(new HospitalLocation(hospital, code, request.nameEn().trim(),
                request.nameHi().trim(), request.type(), request.building().trim(), request.floorLabel().trim(),
                blankToNull(request.zone()), blankToNull(request.roomNumber()), request.mapX(), request.mapY()));
        audit.record("NAVIGATION_LOCATION_CREATED", "HOSPITAL_LOCATION", location.getId(), hospitalId);
        return toResponse(location);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    public void createPath(UUID hospitalId, PathRequest request) {
        Hospital hospital = requireHospital(hospitalId);
        HospitalLocation from = requireLocation(hospitalId, request.fromCode());
        HospitalLocation to = requireLocation(hospitalId, request.toCode());
        if (from.getId().equals(to.getId())) throw new IllegalArgumentException("A navigation path needs two locations.");
        NavigationPath path = paths.save(new NavigationPath(hospital, from, to, request.instructionEn().trim(),
                request.instructionHi().trim(), request.reverseInstructionEn().trim(),
                request.reverseInstructionHi().trim(), request.distanceMeters(), request.durationSeconds(),
                request.stepFree()));
        audit.record("NAVIGATION_PATH_CREATED", "NAVIGATION_PATH", path.getId(), hospitalId);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    public CheckpointResponse createCheckpoint(UUID hospitalId, CheckpointRequest request) {
        Hospital hospital = requireHospital(hospitalId);
        HospitalLocation location = requireLocation(hospitalId, request.locationCode());
        String code = request.publicCode().trim().toUpperCase(Locale.ROOT);
        if (checkpoints.findByPublicCodeIgnoreCaseAndActiveTrue(code).isPresent()) {
            throw new ConflictException("This QR checkpoint code is already active.");
        }
        QrCheckpoint checkpoint = checkpoints.save(new QrCheckpoint(hospital, location, code,
                request.labelEn().trim(), request.labelHi().trim()));
        audit.record("QR_CHECKPOINT_CREATED", "QR_CHECKPOINT", checkpoint.getId(), hospitalId);
        return toResponse(checkpoint);
    }

    private RouteResponse shortestRoute(Hospital hospital, HospitalLocation source, HospitalLocation destination,
                                        String language, boolean stepFree) {
        if (source.getId().equals(destination.getId())) {
            return routeResponse(true, language, stepFree, source, destination, 0, 0,
                    List.of(source.getCode()), List.of(), language.equals("hi")
                            ? "आप गंतव्य पर हैं।" : "You are already at the destination.");
        }
        List<NavigationPath> allPaths = paths.findAllByHospitalIdAndActiveTrue(hospital.getId()).stream()
                .filter(path -> !stepFree || path.isStepFree()).toList();
        Map<UUID, List<Traversal>> graph = new HashMap<>();
        for (NavigationPath path : allPaths) {
            graph.computeIfAbsent(path.getFromLocation().getId(), ignored -> new ArrayList<>())
                    .add(new Traversal(path.getToLocation(), path, false));
            graph.computeIfAbsent(path.getToLocation().getId(), ignored -> new ArrayList<>())
                    .add(new Traversal(path.getFromLocation(), path, true));
        }
        Map<UUID, Integer> distance = new HashMap<>();
        Map<UUID, Previous> previous = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(Comparator.comparingInt(NodeDistance::distance));
        distance.put(source.getId(), 0);
        queue.add(new NodeDistance(source, 0));
        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            if (current.distance() != distance.getOrDefault(current.location().getId(), Integer.MAX_VALUE)) continue;
            if (current.location().getId().equals(destination.getId())) break;
            for (Traversal traversal : graph.getOrDefault(current.location().getId(), List.of())) {
                int candidate = current.distance() + traversal.path().getDurationSeconds();
                if (candidate < distance.getOrDefault(traversal.next().getId(), Integer.MAX_VALUE)) {
                    distance.put(traversal.next().getId(), candidate);
                    previous.put(traversal.next().getId(), new Previous(current.location(), traversal));
                    queue.add(new NodeDistance(traversal.next(), candidate));
                }
            }
        }
        if (!distance.containsKey(destination.getId())) {
            return routeResponse(false, language, stepFree, source, destination, 0, 0, List.of(), List.of(),
                    language.equals("hi") ? "इस गंतव्य के लिए सत्यापित रास्ता उपलब्ध नहीं है। कृपया सहायता डेस्क से पूछें।"
                            : "A verified route to this destination is unavailable. Please ask the hospital help desk.");
        }
        LinkedList<Previous> traversals = new LinkedList<>();
        HospitalLocation cursor = destination;
        while (!cursor.getId().equals(source.getId())) {
            Previous item = previous.get(cursor.getId());
            traversals.addFirst(item);
            cursor = item.from();
        }
        List<RouteStep> steps = new ArrayList<>();
        List<String> pathCodes = new ArrayList<>();
        pathCodes.add(source.getCode());
        int totalMeters = 0;
        int totalSeconds = 0;
        int order = 1;
        for (Previous item : traversals) {
            NavigationPath path = item.traversal().path();
            HospitalLocation from = item.from();
            HospitalLocation to = item.traversal().next();
            String instruction = instruction(path, item.traversal().reverse(), language);
            steps.add(new RouteStep(order++, instruction, path.getDistanceMeters(), path.getDurationSeconds(),
                    from.getCode(), to.getCode(), from.getFloorLabel(), to.getFloorLabel(),
                    !from.getFloorLabel().equalsIgnoreCase(to.getFloorLabel()), path.isStepFree()));
            totalMeters += path.getDistanceMeters();
            totalSeconds += path.getDurationSeconds();
            pathCodes.add(to.getCode());
        }
        return routeResponse(true, language, stepFree, source, destination, totalMeters,
                Math.max(1, (int) Math.ceil(totalSeconds / 60.0)), pathCodes, steps,
                language.equals("hi") ? "सत्यापित अस्पताल मानचित्र पर रास्ता मिल गया।" : "Route found on the verified hospital map.");
    }

    private RouteResponse routeResponse(boolean available, String language, boolean stepFree,
                                        HospitalLocation source, HospitalLocation destination, int meters, int minutes,
                                        List<String> pathCodes, List<RouteStep> steps, String message) {
        String safety = language.equals("hi")
                ? "इनडोर GPS का उपयोग नहीं किया गया। QR बोर्ड और अस्पताल के संकेत मानें। आपातकाल में तुरंत स्टाफ से सहायता लें।"
                : "Indoor GPS was not used. Follow QR boards and hospital signs; ask staff immediately in an emergency.";
        return new RouteResponse(available, language, stepFree, toResponse(source), toResponse(destination), meters,
                minutes, pathCodes, steps, message, safety);
    }

    private Hospital requireHospital(UUID id) {
        return hospitals.findById(id).filter(Hospital::isActive)
                .orElseThrow(() -> new NotFoundException("Hospital was not found."));
    }

    private HospitalLocation requireLocation(UUID hospitalId, String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("A destination code is required.");
        return locations.findByHospitalIdAndCodeIgnoreCase(hospitalId, code.trim())
                .filter(HospitalLocation::isActive)
                .orElseThrow(() -> new NotFoundException("Verified hospital location was not found."));
    }

    private QrCheckpoint requireCheckpoint(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("A QR checkpoint code is required.");
        return checkpoints.findByPublicCodeIgnoreCaseAndActiveTrue(code.trim())
                .orElseThrow(() -> new NotFoundException("QR checkpoint was not recognized."));
    }

    private static String instruction(NavigationPath path, boolean reverse, String language) {
        if (language.equals("hi")) return reverse ? path.getReverseInstructionHi() : path.getInstructionHi();
        return reverse ? path.getReverseInstructionEn() : path.getInstructionEn();
    }

    private static String normalizedLanguage(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).startsWith("hi") ? "hi" : "en";
    }

    private static String normalizedCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LocationResponse toResponse(HospitalLocation location) {
        return new LocationResponse(location.getCode(), location.getNameEn(), location.getNameHi(), location.getType(),
                location.getBuilding(), location.getFloorLabel(), location.getZone(), location.getRoomNumber(),
                location.getMapX(), location.getMapY());
    }

    private static CheckpointResponse toResponse(QrCheckpoint checkpoint) {
        return new CheckpointResponse(checkpoint.getHospital().getId(), checkpoint.getHospital().getName(),
                checkpoint.getPublicCode(), checkpoint.getLabelEn(), checkpoint.getLabelHi(),
                "/navigate/" + checkpoint.getPublicCode(), toResponse(checkpoint.getLocation()));
    }

    private record Traversal(HospitalLocation next, NavigationPath path, boolean reverse) {
    }

    private record Previous(HospitalLocation from, Traversal traversal) {
    }

    private record NodeDistance(HospitalLocation location, int distance) {
    }
}
