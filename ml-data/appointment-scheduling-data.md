# Appointment scheduling dataset assessment

Source: [Medical Appointment Scheduling System](https://www.kaggle.com/datasets/carogonzalezgaltier/medical-appointment-scheduling-system), CC BY 4.0.

The raw files are intentionally ignored by Git and remain under `ml-data/raw/appointment-scheduling/` only on the local development machine.

`appointments.csv` contains an explicit `waiting_time` target and appointment timing details. The training evaluation reads only this file and excludes patient ID, name, insurance, sex, age, and age group.

It is not merged with the deployed OPD wait-time model because it does not provide department, clinical triage, patients-ahead, active-doctor, or doctor-delay fields. The model evaluation is retained as a scheduling-data benchmark; the deployed model remains the department-and-triage hybrid model.
