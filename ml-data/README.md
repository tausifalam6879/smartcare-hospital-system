# OPD wait-time data

## Source and scope

The exploratory training input was the synthetic [Hospital Wait Time Data dataset](https://www.kaggle.com/datasets/bharathreddybollu/hospital-wait-time-data). It contains simulated records and must not be represented as real patient or hospital data.

- `raw/` is intentionally ignored by Git and must be downloaded separately from the source.
- `processed/opd_wait_time.csv` is the non-identifying 5,000-row training table produced in the notebook.
- `collection_template.csv` defines fields that a future governed hospital-data collection process would need. It contains headers only.

The model target is `ActualWaitMinutes`, derived from the source field `TriageToProviderStartTime`. The prototype model uses only `Department` and `TriageCategory` for its learned historical baseline because the source dataset did not contain credible relationships between wait time and occupancy/staffing fields. Live operational pressure is therefore handled separately with an explicit rule-based adjustment.

Do not add real patient identifiers, diagnoses, contact details, or unapproved operational exports to this directory.
