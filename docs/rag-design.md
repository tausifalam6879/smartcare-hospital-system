# Patient-Isolated Grounded Assistant Design

## Delivered Phase 7 pipeline

```text
Authenticated patient
        ↓
Resolve owned patient profile
        ↓
Index immutable visit facts + owned report content
        ↓
Chunk + LOCAL_HASH_V1_192 embedding
        ↓
SELECT chunks WHERE patient_id = authenticated_patient_id
        ↓
Rank only that filtered set
        ↓
Deterministic grounded answer + persisted citations
```

The ownership predicate is part of the repository query before similarity scoring. Application-side filtering after ranking is forbidden because it could expose another patient's content to the ranker or a future model.

## Sources and extraction

Clinician-finalized visits are converted into structured evidence containing the recorded symptoms, diagnosis, notes, discharge summary, follow-up, prescription items, and allergies. Uploaded PDF reports are parsed page by page with Apache PDFBox, limited to 100 pages and 250,000 extracted characters, then split into overlapping 900-character chunks. Each chunk stores patient, hospital, source, page, content hash, embedding model, vector, citation label, and extraction provenance.

JPG/PNG and image-only PDFs are marked `OCR_REQUIRED` or `NO_TEXT`. Encrypted or invalid PDFs receive a non-sensitive error code. A metadata-only chunk can locate the report, but content questions return an explicit unavailable answer. Phase 7 never runs unapproved OCR or fabricates missing report text.

## Answer mode

The delivered default is `PRIVATE_LOCAL_GROUNDED`: retrieval and evidence composition run locally and report text is not sent to an external provider. Answers quote or compact only retrieved evidence and attach source/page cards. An approved on-prem or external LLM can be added later behind a reviewed adapter, but it must receive only the already-authorized retrieved passages and must preserve citations and safety policy.

## Safety and privacy

- Emergency-language questions bypass retrieval and direct the patient to qualified emergency help immediately.
- Requests to diagnose, prescribe, change dose, stop treatment, or declare waiting safe are refused and redirected to qualified clinicians.
- Missing evidence returns “information unavailable”; the assistant never falls back to another patient's data or model memory.
- Patient-uploaded and clinician-finalized provenance remain visibly different.
- Conversation access, retrieval, and message history are patient-owned. Sensitive operations are audited without placing question/answer or report content in audit logs.
- The local per-user rate limiter protects the development deployment. Production requires a shared/distributed limiter.

## Production replacements

Before real patient use, add asynchronous extraction workers, sandboxing, malware scanning/quarantine, managed private object storage, approved OCR, a production vector store with mandatory tenancy policy, consent/retention workflows, clinical evaluation, red-team tests, monitoring, and approved model/data-processing governance.
