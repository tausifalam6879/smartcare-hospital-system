# Phase 12 Blood-Group Image Decision Support

## Safety position

This optional module is an experimental review workflow, not a medical device or substitute for validated laboratory testing. The development `BloodSlideAnalyzer` adapter deliberately returns `NOT_CONFIGURED`; no visual guess is presented as a blood group. Transfusion compatibility, cross-matching, donor eligibility, specimen identity, and clinical decisions remain with qualified staff and approved laboratory systems.

## Workflow

1. An authenticated patient selects a hospital, explicitly acknowledges the boundary, and submits a real JPG/PNG blood-slide image.
2. The server decodes the image, enforces byte/dimension limits, stores it under an opaque patient-specific key, and creates a `SUBMITTED` analysis with no confirmed group.
3. A lab technician or blood-bank staff member privately reviews the image and records visible Anti-A, Anti-B, and Anti-D reactivity once.
4. The domain maps that reaction tuple deterministically to a preliminary ABO/Rh group and enters `OBSERVATIONS_RECORDED`.
5. A different authorized staff member verifies the same reaction-derived group. Only then does the state become `LAB_VERIFIED`.
6. An unusable image may instead be `REJECTED` with a patient-visible reason.

The maker-checker rule is enforced in the entity and database. A verified review artifact does not automatically overwrite a patient's clinical record.

## Analyzer port

`BloodSlideAnalyzer` isolates any future ONNX/OpenCV implementation. Its result has an explicit attempt status, optional suggestion, and confidence. A future adapter may populate an experimental suggestion only after governed model validation, but the human observation and independent-verification gates remain mandatory.

## Production boundary

Before clinical deployment, require an approved specimen-capture protocol, calibrated imaging, representative training/evaluation data, external validation, bias and failure-mode analysis, versioned model registry, confidence/abstention policy, LIMS specimen identity and confirmed-result integration, encrypted object storage, malware scanning, tenant-scoped reviewer assignment, MFA, maker-checker policy, retention/deletion schedules, incident monitoring, regulatory assessment, and formal laboratory governance.
