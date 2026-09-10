import json
from pathlib import Path
from typing import Literal

import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image, UnidentifiedImageError
from pydantic import BaseModel, Field
from skimage.feature import hog


PROJECT_ROOT = Path(__file__).resolve().parent.parent
MODEL_PATH = PROJECT_ROOT / "ml-models" / "opd_wait_time_model.joblib"
BLOOD_MODEL_PATH = PROJECT_ROOT / "ml-artifacts" / "agglutination_hog_logreg.joblib"
BLOOD_CONFIG_PATH = PROJECT_ROOT / "ml-artifacts" / "feature_config.json"

if not MODEL_PATH.exists():
    raise RuntimeError(f"Model artifact not found: {MODEL_PATH}")

artifact = joblib.load(MODEL_PATH)

wait_time_model = artifact["model"]
config = artifact["hybrid_config"]
validation = artifact["validation"]

if not BLOOD_MODEL_PATH.exists() or not BLOOD_CONFIG_PATH.exists():
    raise RuntimeError(
        "Blood-analysis artifacts are missing. Run the BloodyWell training notebook "
        "and save the model under ml-artifacts/."
    )

blood_model = joblib.load(BLOOD_MODEL_PATH)

with BLOOD_CONFIG_PATH.open("r", encoding="utf-8") as blood_config_file:
    blood_config = json.load(blood_config_file)

encoder = (
    wait_time_model.named_steps["preprocessor"]
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


class AgglutinationResponse(BaseModel):
    agglutinationDetected: bool
    agglutinationProbability: float = Field(ge=0, le=1)
    confidence: float = Field(ge=0, le=1)
    manualReviewRequired: bool
    modelName: str
    modelVersion: str
    safetyNotice: str


class BloodGroupPanelResponse(BaseModel):
    antiA: AgglutinationResponse
    antiB: AgglutinationResponse
    antiD: AgglutinationResponse
    bloodGroup: str | None
    interpretationStatus: Literal[
        "AI-assisted interpretation — clinician verification required",
        "Manual verification required before interpretation",
    ]
    explanation: str
    safetyNotice: str


app = FastAPI(
    title="SmartCare ML Inference API",
    version=artifact["artifact_version"],
    description="Prototype OPD wait-time and blood-reaction inference service.",
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
        "bloodAnalysisModel": blood_config["model_name"],
        "bloodAnalysisStatus": "prototype-not-clinically-validated",
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

        base_wait = float(wait_time_model.predict(model_input)[0])
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
            np.mean(wait_time_model.predict(fallback_inputs))
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


def extract_blood_hog_features(image_bytes: bytes) -> np.ndarray:
    """Create the exact HOG feature vector used during BloodyWell training."""
    if not image_bytes:
        raise ValueError("Blood-reaction image is empty.")
    if len(image_bytes) > 10 * 1024 * 1024:
        raise ValueError("Blood-reaction image must not exceed 10 MB.")

    try:
        from io import BytesIO

        with Image.open(BytesIO(image_bytes)) as image:
            grayscale = image.convert("L").resize(
                tuple(blood_config["image_size"]),
                Image.Resampling.LANCZOS,
            )
            image_array = np.asarray(grayscale, dtype=np.float32) / 255.0
    except (UnidentifiedImageError, OSError) as exception:
        raise ValueError("Upload a readable PNG or JPEG reaction-well image.") from exception

    return hog(
        image_array,
        orientations=blood_config["hog_orientations"],
        pixels_per_cell=tuple(blood_config["pixels_per_cell"]),
        cells_per_block=tuple(blood_config["cells_per_block"]),
        block_norm="L2-Hys",
    ).reshape(1, -1)


def predict_agglutination(image_bytes: bytes) -> AgglutinationResponse:
    features = extract_blood_hog_features(image_bytes)
    probability = float(blood_model.predict_proba(features)[0, 1])
    confidence = max(probability, 1 - probability)
    threshold = float(blood_config["review_threshold"])

    return AgglutinationResponse(
        agglutinationDetected=probability >= 0.5,
        agglutinationProbability=round(probability, 6),
        confidence=round(confidence, 6),
        manualReviewRequired=confidence < threshold,
        modelName=blood_config["model_name"],
        modelVersion="bloodywell-hog-logreg-1.0",
        safetyNotice=(
            "AI-assisted prototype only. A qualified laboratory professional must "
            "verify every reaction and final blood-group interpretation."
        ),
    )


def interpret_abo_panel(
    anti_a: AgglutinationResponse,
    anti_b: AgglutinationResponse,
    anti_d: AgglutinationResponse,
) -> BloodGroupPanelResponse:
    reactions = {"Anti-A": anti_a, "Anti-B": anti_b, "Anti-D": anti_d}
    uncertain = [
        reagent for reagent, result in reactions.items()
        if result.manualReviewRequired
    ]
    safety_notice = (
        "AI-assisted prototype only. A qualified laboratory professional must "
        "verify every reaction and final blood-group interpretation."
    )

    if uncertain:
        return BloodGroupPanelResponse(
            antiA=anti_a,
            antiB=anti_b,
            antiD=anti_d,
            bloodGroup=None,
            interpretationStatus="Manual verification required before interpretation",
            explanation=(
                "No blood-group conclusion generated because these reaction wells "
                "need manual review: " + ", ".join(uncertain) + "."
            ),
            safetyNotice=safety_notice,
        )

    group_map = {
        (False, False, False): "O-",
        (False, False, True): "O+",
        (True, False, False): "A-",
        (True, False, True): "A+",
        (False, True, False): "B-",
        (False, True, True): "B+",
        (True, True, False): "AB-",
        (True, True, True): "AB+",
    }
    reaction_tuple = (
        anti_a.agglutinationDetected,
        anti_b.agglutinationDetected,
        anti_d.agglutinationDetected,
    )
    blood_group = group_map[reaction_tuple]
    positive = [
        reagent for reagent, result in reactions.items()
        if result.agglutinationDetected
    ]
    negative = [
        reagent for reagent, result in reactions.items()
        if not result.agglutinationDetected
    ]

    return BloodGroupPanelResponse(
        antiA=anti_a,
        antiB=anti_b,
        antiD=anti_d,
        bloodGroup=blood_group,
        interpretationStatus="AI-assisted interpretation — clinician verification required",
        explanation=(
            f"Agglutination detected: {', '.join(positive) or 'None'}. "
            f"No significant agglutination: {', '.join(negative) or 'None'}. "
            f"Rule-based prototype interpretation: {blood_group}."
        ),
        safetyNotice=safety_notice,
    )


async def read_reaction_image(file: UploadFile, reagent: str) -> bytes:
    if file.content_type not in {"image/jpeg", "image/png"}:
        raise HTTPException(
            status_code=415,
            detail=f"{reagent} must be a PNG or JPEG reaction-well image.",
        )
    return await file.read()


@app.post("/predict-agglutination", response_model=AgglutinationResponse)
async def predict_agglutination_endpoint(
    file: UploadFile = File(...),
) -> AgglutinationResponse:
    if file.content_type not in {"image/jpeg", "image/png"}:
        raise HTTPException(status_code=415, detail="Only PNG or JPEG reaction-well images are supported.")

    try:
        return predict_agglutination(await file.read())
    except ValueError as exception:
        raise HTTPException(status_code=422, detail=str(exception)) from exception


@app.post("/analyze-abo-panel", response_model=BloodGroupPanelResponse)
async def analyze_abo_panel_endpoint(
    antiAFile: UploadFile = File(...),
    antiBFile: UploadFile = File(...),
    antiDFile: UploadFile = File(...),
) -> BloodGroupPanelResponse:
    """Interpret three staff-labelled well images using a transparent ABO/Rh rule."""
    try:
        anti_a = predict_agglutination(await read_reaction_image(antiAFile, "Anti-A"))
        anti_b = predict_agglutination(await read_reaction_image(antiBFile, "Anti-B"))
        anti_d = predict_agglutination(await read_reaction_image(antiDFile, "Anti-D"))
        return interpret_abo_panel(anti_a, anti_b, anti_d)
    except ValueError as exception:
        raise HTTPException(status_code=422, detail=str(exception)) from exception
