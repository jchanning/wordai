# ARCH-02 Move Analysis Result Models

## Status

Completed on 2026-03-07.

## Goal

Move analysis result models out of the HTTP layer so the analysis package owns its own result types.

## Problem

- `PlayerAnalyser` imports analysis result DTOs from `server.dto`.
- The analysis layer therefore depends on the server layer.
- This keeps the `analysis -> server` architecture test disabled.

## Scope

- Move `AnalysisGameResult` and `AnalysisResponse` into an analysis-owned package or shared results package.
- Update server endpoints to serialize the moved models without changing response structure.
- Preserve existing analysis behavior and API contracts.

## Acceptance Criteria

- Analysis result types used by the analyser no longer live under `server.dto`.
- `analysis` has no dependency on `server`.
- The disabled `analysis -> server` ArchUnit rule is enabled and passes.
- `POST /api/wordai/analysis` remains backward-compatible.

## Notes

- Complete this alongside ARCH-01 so the architecture fitness test can tighten quickly.