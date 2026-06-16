# ticket-056: Webhook / MQTT publish action

- **Milestone:** M5
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-038

## Description

Add two new outbound action types so a fired macro can signal external systems
(Home Assistant, Node-RED, IFTTT, Zapier, etc.) without the app managing any
devices directly. The app is the **trigger layer**; the external system owns
actuation.

This is Option B of the external-device scope decision: sensor input (tickets
054–055) plus a thin outbound signal. No device registry, no state tracking, no
scenes.

## Actions

### `WebhookAction`
```kotlin
@Serializable
@SerialName("webhook")
data class WebhookAction(
    val url: String,
    val method: String = "POST",            // POST | GET | PUT
    val headers: Map<String, String> = emptyMap(),
    val bodyTemplate: String = "",          // supports {{macro.id}}, {{fired_at}}
) : MacroAction() {
    override val actionType get() = "webhook"
}
```
- Uses `OkHttp` (already on the classpath via Room/Retrofit transitive deps) or
  `java.net.HttpURLConnection` to keep dep count down.
- Fire-and-forget: result is logged but never retries (callers own idempotency).
- Template variables: `{{macro.id}}`, `{{macro.name}}`, `{{fired_at_ms}}`,
  `{{trigger.pattern}}`.

### `MqttPublishAction`
```kotlin
@Serializable
@SerialName("mqtt_publish")
data class MqttPublishAction(
    val brokerUrl: String,                  // e.g. "tcp://192.168.1.10:1883"
    val topic: String,                      // e.g. "home/gesturemacro/fired"
    val payload: String = "",               // supports same template vars
    val qos: Int = 0,
    val retain: Boolean = false,
) : MacroAction() {
    override val actionType get() = "mqtt_publish"
}
```
- Uses `org.eclipse.paho:org.eclipse.paho.client.mqttv3` (lightweight, no broker
  bundled).
- Connect → publish → disconnect per firing (stateless; no persistent session).

## Acceptance criteria

- [ ] `WebhookAction` and `MqttPublishAction` data classes in `core.serialization`
  with `@Serializable` / `@SerialName`.
- [ ] `WebhookExecutor` and `MqttPublishExecutor` in `android.actions` implementing
  `ActionExecutor`; registered in `BuiltinExecutorRegistry`.
- [ ] Template variable substitution util in `core.actions`.
- [ ] `MacroCodec` schema updated; golden fixture added.
- [ ] Unit tests: template expansion, executor happy path (mock HTTP / mock MQTT).
- [ ] UI: both actions available in the action picker (ticket-018 catalog).

## Technical notes

- No retry logic inside the executor — the external system must handle duplicates.
- OkHttp call dispatched on `Dispatchers.IO`; timeout 10 s connect + 15 s read.
- MQTT: connect timeout 5 s; if broker unreachable, log `ExecResult.Failure` and
  continue (non-fatal).
- No TLS pinning required; user-supplied broker URL. Document that plain `tcp://`
  is insecure on untrusted networks.
