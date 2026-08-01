# TODO - Fusion ds-free-api -> api_bridge

## Phase 1 : Analyse approfondie
- [x] Lire tout LocalApiServer.kt
- [x] Lire WebInterface.kt en entier
- [x] Lire AutomationCoordinator.kt en entier
- [x] Lire FloatingWindowService.kt
- [x] Lire CalibrationOverlayManager.kt
- [x] Lire utils restants

## Phase 2 : Refonte API (LocalApiServer.kt)
- [x] Ajouter endpoint POST /v1/chat/completions
- [x] Ajouter endpoint GET /v1/models
- [x] Conserver /ask, /result pour backward compat
- [ ] Implémenter streaming SSE
- [x] Implémenter tool calling 3 niveaux (ToolParser.kt)

## Phase 3 : Refonte UI Web (WebInterface.kt)
- [x] Cloner le design du panneau admin ds-free-api
- [x] Dashboard : statut, stats, uptime
- [x] Page Models
- [x] Page Config/Calibration
- [x] Page Logs
- [x] Sidebar navigation + theme sombre
- [x] Page Chat integree

## Phase 4 : GitHub Actions
- [x] Creer .github/workflows/build.yml
- [x] Configurer build APK automatique
- [x] Push avec token sur main

## Phase 5 : Tests & Livraison
- [ ] Verifier compilation (GitHub Actions en cours)
- [x] Commit + push final
