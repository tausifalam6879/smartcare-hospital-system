from pathlib import Path
from typing import Literal

import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field


PROJECT_ROOT = Path(__file__).resolve().parent.parent
MODEL_PATH = PROJECT_ROOT / "ml-models" / "opd_wait_time_model.joblib"

if not MODEL_PATH.exists():
    raise RuntimeError(f"Model artifact not found: {MODEL_PATH}")

artifact = joblib.load(MODEL_PATH)

model = artifact["model"]
config = artifact["hybrid_config"]
validation = artifact["validation"]

encoder = (
    model.named_steps["preprocessor"]
    .named_transformers_["categorical"]
)

supported_departments = {
    str(value) for value in encoder.categories_[0]
}


class WaitTimeRequest(BaseModel):
    department: str = Field(min_length=1, max_length=100)

    triageCategory: Literal[
        "Immediate",
        "Emergency",
        "Urgent",
        "Semi-urgent",
        "Non-urgent",
    ]

    patientsAhead: int = Field(ge=0, le=200)
    activeDoctors: int = Field(ge=1, le=50)

    averageConsultationMinutes: float = Field(
        default=12,
        gt=0,
        le=60,
    )

    currentDoctorDelayMinutes: float = Field(
        default=0,
        ge=0,
        le=240,
    )

    occupancyRate: float = Field(
        default=0.50,
        ge=0,
        le=1,
    )


class WaitTimeRange(BaseModel):
    minimum: float
    maximum: float


class WaitTimeResponse(BaseModel):
    estimatedWaitMinutes: float
    estimatedRangeMinutes: WaitTimeRange
    mlBaseWaitMinutes: float
    liveQueueWaitMinutes: float
    departmentSupported: bool
    baseEstimateSource: Literal[
        "department-specific",
        "generic-department-average",
    ]
    modelVersion: str
    modelStatus: str
    urgentNotice: str | None


app = FastAPI(
    title="SmartCare OPD Wait-Time API",
    version=artifact["artifact_version"],
    description="Prototype OPD waiting-time estimation service.",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://127.0.0.1:5173",
        "http://localhost:5173",
        "http://127.0.0.1:5174",
        "http://localhost:5174",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {
        "status": "ok",
        "model": artifact["model_name"],
        "version": artifact["artifact_version"],
        "modelStatus": artifact["status"],
    }


def predict_base_wait(
    department: str,
    triage_category: str,
):
    department_supported = department in supported_departments

    if department_supported:
        model_input = pd.DataFrame(
            [{
                "Department": department,
                "TriageCategory": triage_category,
            }]
        )

        base_wait = float(model.predict(model_input)[0])
        source = "department-specific"

    else:
        fallback_inputs = pd.DataFrame(
            [
                {
                    "Department": known_department,
                    "TriageCategory": triage_category,
                }
                for known_department in sorted(supported_departments)
            ]
        )

        base_wait = float(
            np.mean(model.predict(fallback_inputs))
        )
        source = "generic-department-average"

    return base_wait, department_supported, source


@app.post("/predict-wait", response_model=WaitTimeResponse)
def predict_wait(payload: WaitTimeRequest) -> WaitTimeResponse:
    base_wait, department_supported, base_source = (
        predict_base_wait(
            payload.department,
            payload.triageCategory,
        )
    )

    raw_queue_wait = (
        payload.patientsAhead
        * payload.averageConsultationMinutes
        / payload.activeDoctors
    ) + payload.currentDoctorDelayMinutes

    occupancy_multiplier = (
        config["occupancy_base"]
        + config["occupancy_slope"]
        * payload.occupancyRate
    )

    priority_weight = config["triage_queue_weight"][
        payload.triageCategory
    ]

    live_queue_wait = (
        raw_queue_wait
        * occupancy_multiplier
        * priority_weight
    )

    final_wait = max(
        1,
        config["ml_base_weight"] * base_wait
        + config["live_queue_weight"] * live_queue_wait,
    )

    uncertainty = validation["mae_mean_minutes"]

    minimum_wait = max(1, final_wait - uncertainty)
    maximum_wait = final_wait + uncertainty

    urgent_notice = None

    if payload.triageCategory in {"Immediate", "Emergency"}:
        urgent_notice = (
            "Emergency cases must be assessed immediately. "
            "Do not delay care based on this estimate."
        )

    return WaitTimeResponse(
        estimatedWaitMinutes=round(final_wait, 1),
        estimatedRangeMinutes=WaitTimeRange(
            minimum=round(minimum_wait, 1),
            maximum=round(maximum_wait, 1),
        ),
        mlBaseWaitMinutes=round(base_wait, 1),
        liveQueueWaitMinutes=round(live_queue_wait, 1),
        departmentSupported=department_supported,
        baseEstimateSource=base_source,
        modelVersion=artifact["artifact_version"],
        modelStatus=artifact["status"],
        urgentNotice=urgent_notice,
    )
