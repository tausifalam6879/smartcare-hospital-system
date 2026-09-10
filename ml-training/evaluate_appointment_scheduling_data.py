"""Evaluate the optional appointment-scheduling dataset without using patient PII.

The source dataset stays in ml-data/raw/ and is ignored by Git. This script reads
only appointments.csv, drops patient identifiers and demographics, and writes a
small JSON evaluation summary suitable for the model card.
"""

from __future__ import annotations

import json
from pathlib import Path

import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import HistGradientBoostingRegressor
from sklearn.impute import SimpleImputer
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder


PROJECT_ROOT = Path(__file__).resolve().parent.parent
SOURCE = PROJECT_ROOT / "ml-data" / "raw" / "appointment-scheduling" / "appointments.csv"
OUTPUT = PROJECT_ROOT / "ml-models" / "appointment_scheduling_evaluation.json"


def build_features() -> pd.DataFrame:
    appointments = pd.read_csv(SOURCE)
    attended = appointments.loc[appointments["status"].eq("attended")].copy()

    attended["appointment_date"] = pd.to_datetime(attended["appointment_date"])
    appointment_time = pd.to_datetime(
        attended["appointment_time"], format="%H:%M:%S"
    )

    attended["appointment_hour"] = appointment_time.dt.hour
    attended["appointment_weekday"] = attended["appointment_date"].dt.day_name()
    attended["appointment_month"] = attended["appointment_date"].dt.month
    attended["arrival_offset_minutes"] = (
        pd.to_timedelta(attended["check_in_time"]).dt.total_seconds() / 60
        - pd.to_timedelta(attended["appointment_time"]).dt.total_seconds() / 60
    )

    return attended[
        [
            "appointment_hour",
            "appointment_weekday",
            "appointment_month",
            "scheduling_interval",
            "appointment_duration",
            "arrival_offset_minutes",
            "waiting_time",
        ]
    ].dropna()


def main() -> None:
    dataset = build_features()
    target = "waiting_time"
    features = [column for column in dataset.columns if column != target]
    categorical = ["appointment_weekday"]
    numeric = [column for column in features if column not in categorical]

    preprocessor = ColumnTransformer(
        [
            ("numeric", SimpleImputer(strategy="median"), numeric),
            (
                "categorical",
                Pipeline(
                    [
                        ("impute", SimpleImputer(strategy="most_frequent")),
                        ("encode", OneHotEncoder(handle_unknown="ignore")),
                    ]
                ),
                categorical,
            ),
        ]
    )

    pipeline = Pipeline(
        [
            ("preprocessor", preprocessor),
            (
                "model",
                HistGradientBoostingRegressor(
                    max_iter=250,
                    learning_rate=0.06,
                    max_leaf_nodes=31,
                    random_state=42,
                ),
            ),
        ]
    )

    X_train, X_test, y_train, y_test = train_test_split(
        dataset[features], dataset[target], test_size=0.20, random_state=42
    )
    pipeline.fit(X_train, y_train)
    predictions = pipeline.predict(X_test)

    summary = {
        "source": "Medical Appointment Scheduling System (Kaggle, CC BY 4.0)",
        "status": "evaluated-not-deployed",
        "records_used": int(len(dataset)),
        "target": "waiting_time",
        "features": features,
        "excluded_fields": ["patient_id", "sex", "age", "age_group", "name", "insurance"],
        "metrics": {
            "testing_mae_minutes": round(float(mean_absolute_error(y_test, predictions)), 2),
            "testing_rmse_minutes": round(
                float(mean_squared_error(y_test, predictions) ** 0.5), 2
            ),
            "testing_r2": round(float(r2_score(y_test, predictions)), 4),
        },
        "decision": (
            "Do not replace the deployed department-and-triage model. "
            "This source has no department, triage, patient-ahead, doctor-count, "
            "or doctor-delay fields; it is retained for scheduling analysis only."
        ),
    }

    OUTPUT.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
