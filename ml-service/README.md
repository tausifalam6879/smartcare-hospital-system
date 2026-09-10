# SmartCare OPD wait-time service

This local FastAPI service loads the versioned SmartCare OPD model artifact and combines its department/triage baseline with transparent live queue inputs. It is a project prototype, not a clinical decision system or a guaranteed appointment time.

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

The frontend reads `VITE_ML_API_URL` and defaults to `http://127.0.0.1:8001`. If the service is unavailable, prototype booking remains usable and clearly labels its deterministic fallback estimate.

## Verify

```powershell
Set-Location .\ml-service
python -m unittest -v test_app.py
```

The tests cover artifact loading, supported and unseen departments, increasing load pressure, uncertainty bounds, and emergency escalation messaging.
