# OPD wait-time model card

## Artifact

`opd_wait_time_model.joblib` contains the versioned `SmartCare OPD Base Wait Model` pipeline and its hybrid-configuration metadata.

- Artifact version: `1.0.0`
- Status: `prototype-not-clinically-validated`
- Training source: 5,000 synthetic records
- Learned features: department and triage category
- Target: triage-to-provider waiting time in minutes

## Evaluation

Five-fold cross-validation on the synthetic processed dataset produced:

| Metric | Mean | Fold standard deviation |
| --- | ---: | ---: |
| MAE | 27.16 minutes | 0.34 minutes |
| RMSE | 40.92 minutes | 0.35 minutes |
| R² | 0.2186 | 0.0102 |

The model explains only a limited share of synthetic wait-time variation. SmartCare displays an indicative range rather than presenting the point estimate as an exact appointment time.

## Hybrid behaviour

The learned model supplies a historical department/triage baseline. A transparent operational layer then adjusts it using patients ahead, active doctors, average consultation duration, current doctor delay, occupancy, and a triage-specific queue weight. Automated tests assert that higher operational load cannot produce a lower estimate for the same department and triage category.

This artifact must not determine clinical urgency, delay emergency assessment, or be presented as validated hospital guidance. Only artifacts generated and reviewed within this repository should be loaded because Joblib artifacts are executable serialized Python objects.

## Appointment scheduling benchmark

The optional **Medical Appointment Scheduling System** source was evaluated separately using 86,032 attended appointments. Its timing-only model achieved a holdout MAE of 28.26 minutes, RMSE of 37.62 minutes, and R² of 0.1613. It is deliberately not deployed: the source has no department, triage, patients-ahead, active-doctor, or doctor-delay fields. See `appointment_scheduling_evaluation.json` and `ml-data/appointment-scheduling-data.md` for the privacy-safe evaluation details.
