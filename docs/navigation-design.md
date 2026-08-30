# Verified Indoor Navigation Design

## Safety boundary

RaahMediQ Health does not use browser GPS as an indoor-position claim. A route starts only from a hospital-approved QR checkpoint selected or scanned by the patient. The backend searches only active, stored map edges. When no connected route exists it returns an unavailable response and asks the patient to use the help desk; no model or fallback text generator creates directions.

Emergency signage and staff instructions always override the assistant. The UI repeats this boundary beside every route.

## Location hierarchy and graph

Each location belongs to one hospital and stores:

```text
Hospital → Building → Floor → Zone → Room/location
```

The stable location code is used in public URLs and API responses. Schematic `mapX`/`mapY` values help patients understand the sequence but are not architectural coordinates. Typed nodes cover entrances, registration, lifts, stairs, corridors, receptions, doctor rooms, labs, imaging, pharmacy, emergency, and exits.

Each path is a hospital-verified bidirectional edge with:

- forward and reverse instructions in English and Hindi;
- physical distance and an approximate walking duration;
- a `stepFree` flag;
- active/inactive status for temporary closure support later.

## Routing

The service runs Dijkstra’s algorithm using expected traversal seconds. A step-free request removes every non-step-free edge before searching. This makes an accessible lift route eligible even when stairs would be quicker. Route steps contain only public location codes and facility metadata—never patient identity.

## QR and appointment flow

A checkpoint URL uses `/navigate/{publicCode}`. Resolving it establishes the hospital and source node. Patients then tap a large destination card. An authenticated `/navigate?appointment={appointmentId}` flow resolves the doctor room only after service-layer ownership validation and exact building/floor/room matching.

## Production rollout checklist

Before a hospital enables a map, facility and accessibility staff must walk every edge in both directions, approve translated wording, verify lifts and emergency boundaries, print tamper-resistant QR signs, and establish a change/closure review process. QR codes must remain patient-independent and must not encode appointment or medical data.
