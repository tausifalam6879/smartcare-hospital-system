# SmartCare ML inference service

This local FastAPI service exposes two separate prototype models:

- `/predict-wait` loads the versioned OPD wait-time artifact and combines a department/triage baseline with transparent live queue inputs.
- `/predict-agglutination` loads the BloodyWell HOG + Logistic Regression artifact and evaluates one PNG/JPEG reaction-well image for agglutination.

Both are project prototypes. The blood-reaction endpoint is **not** a blood-group result: a qualified laboratory professional must verify each reaction, and staff-confirmed Anti-A, Anti-B, and Anti-D results are required before the application may apply its transparent ABO/Rh rule engine.

## Start locally

From the repository root on Windows PowerShell:

```powershell
py -3.13 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r .\ml-service\requirements.txt
Set-Location .\ml-service
python -m uvicorn app:app --host 127.0.0.1 --port 8001
```

Health and interactive API documentation are available at:

- `http://127.0.0.1:8001/health`
- `http://127.0.0.1:8001/docs`

## Blood-reaction endpoint

`POST /predict-agglutination` accepts one multipart form field named `file` containing a PNG or JPEG reaction-well image (maximum 10 MB). It returns the predicted reaction, agglutination probability, confidence, and a manual-review flag using the validation-selected 0.90 confidence threshold.

Example PowerShell request:

```powershell
curl.exe -X POST http://127.0.0.1:8001/predict-agglutination `
  -F "file=@C:\path\to\reaction-well.png"
```

The service contains only the trained `.joblib` and feature configuration. The 1.7 GB BloodyWell training images are not part of deployment.

## Three-well ABO/Rh prototype endpoint

`POST /analyze-abo-panel` accepts three separately labelled multipart images:

- `antiAFile`
- `antiBFile`
- `antiDFile`

It calls the reaction model for each well and applies a transparent ABO/Rh rule only when all three reaction predictions meet the 0.90 confidence threshold. It returns no blood-group conclusion if any well requires manual review.

The caller is responsible for ensuring the images are from the same sample and correctly assigned to Anti-A, Anti-B, and Anti-D. The model cannot infer reagent identity from an arbitrary image.

The frontend reads `VITE_ML_API_URL` and defaults to `http://127.0.0.1:8001`. If the service is unavailable, prototype booking remains usable and clearly labels its deterministic fallback estimate.

## Verify

```powershell
Set-Location .\ml-service
python -m unittest -v test_app.py
```

The tests cover artifact loading, supported and unseen departments, increasing load pressure, uncertainty bounds, and emergency escalation messaging.
