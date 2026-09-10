# Local ML experiment notes

These notebooks and their source data remain local until the project owner explicitly requests a GitHub review and push. Raw datasets are excluded by `.gitignore`.

## 01 — OPD wait-time exploration

Status: selected for the local SmartCare prototype.

The synthetic hospital-wait dataset supports a department-and-triage base estimate. SmartCare combines that estimate with live operational inputs such as patients ahead, active doctors, doctor delay, occupancy, and consultation duration. The model is explicitly labelled prototype-only and not clinically validated.

## 02 — Appointment scheduling EDA

Status: academic analysis only; not deployed.

The appointment scheduling data showed a clear increase in average wait through the day, with appointment hour as the only meaningful booking-time predictor. The selected HistGradientBoosting experiment had five-fold CV MAE 28.63 +/- 0.90 minutes, RMSE 37.64 +/- 1.67 minutes, and R2 0.1459 +/- 0.0050. The dataset has no triage, patients-ahead, active-doctor, or doctor-delay fields.

## 03 — Patient-flow wait-time EDA

Status: rejected for model training.

The patient-flow data contains privacy-sensitive name data in `Merged`, substantial missing department referrals, and weak operational predictors. It is useful for descriptive analysis only.

## 04 — No-show and wait-time EDA

Status: rejected for both wait-time and no-show prediction.

For completed appointments, HistGradientBoosting produced a testing MAE of 46.37 minutes and R2 of -0.1100, worse than the mean baseline. A balanced Random Forest no-show classifier produced ROC-AUC 0.4739, below random performance. Neither model is saved or deployed.

## Future data requirement

Collect governance-approved operational records with department, triage category, patients ahead, active doctors, consultation duration, doctor delay, occupancy rate, and actual wait minutes. Use `ml-data/collection_template.csv` as the collection schema.
