# ADR-0001: Pivot to 100% native Kotlin

- **Status:** Accepted (2026-06-11)
- **Context:** The product's core promise is background gesture capture driving system- and app-level actions. Evaluated options: Flutter, React Native, Kotlin Multiplatform, native Kotlin.
- **Decision:** Build 100% native Kotlin with Jetpack Compose.
- **Rationale:**
  1. **Background survivability** — persistent Foreground Services, FGS types (API 34+ `specialUse`), WakeLock discipline, and Doze/OEM-killer countermeasures require direct lifecycle control; cross-platform bridges add failure modes exactly where reliability is the product.
  2. **Sensor latency** — FR-2 (≤ 500 ms) needs native `SensorManager` sampling/batching without bridge jitter.
  3. **Accessibility API** — node introspection and action dispatch have no faithful cross-platform abstraction; this is the action-execution arm (M2).
  4. iOS is not addressable for this product category at all (no equivalent background sensing + accessibility automation), so cross-platform buys nothing.
- **Consequences:** Single-platform codebase; Android API-level churn (FGS policy, permissions) becomes our maintenance burden; team needs Android-specific expertise. Compose-only UI keeps the UI surface small.
