package io.github.dianila68.gesturemacro.core.actions

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import io.github.dianila68.gesturemacro.core.serialization.MacroAction
import io.github.dianila68.gesturemacro.core.serialization.PlaySoundAction
import io.github.dianila68.gesturemacro.core.serialization.SoundMode
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class SoundExecutor(private val context: Context) : ActionExecutor {

    companion object {
        val BUNDLED_SOUNDS: Map<String, String> = mapOf(
            "no" to android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString(),
            "alert" to android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI.toString(),
            "chime" to android.provider.Settings.System.DEFAULT_RINGTONE_URI.toString(),
        )
    }

    override suspend fun execute(action: MacroAction): ExecResult {
        val soundAction = action as? PlaySoundAction
            ?: return ExecResult.Failure("SoundExecutor received unexpected action type: ${action::class.simpleName}")

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        val focusResult = audioManager.requestAudioFocus(focusRequest)
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return ExecResult.Failure("Could not acquire audio focus")
        }

        return try {
            when (soundAction.mode) {
                SoundMode.BUNDLED -> playBundled(soundAction.bundledSound, audioManager, focusRequest)
                SoundMode.FILE -> playFileUri(soundAction.fileUri, audioManager, focusRequest)
                SoundMode.TTS -> playTts(soundAction.ttsText, audioManager, focusRequest)
            }
        } catch (e: Exception) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            ExecResult.Failure("Sound playback failed: ${e.message}")
        }
    }

    private suspend fun playBundled(
        bundledSound: String?,
        audioManager: AudioManager,
        focusRequest: AudioFocusRequest,
    ): ExecResult {
        val uriString = BUNDLED_SOUNDS[bundledSound]
            ?: android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString()
        return playMediaUri(Uri.parse(uriString), audioManager, focusRequest)
    }

    private suspend fun playFileUri(
        fileUri: String?,
        audioManager: AudioManager,
        focusRequest: AudioFocusRequest,
    ): ExecResult {
        if (fileUri.isNullOrBlank()) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            return ExecResult.Failure("play_sound FILE mode: file_uri is empty")
        }
        return playMediaUri(Uri.parse(fileUri), audioManager, focusRequest)
    }

    private suspend fun playMediaUri(
        uri: Uri,
        audioManager: AudioManager,
        focusRequest: AudioFocusRequest,
    ): ExecResult = suspendCancellableCoroutine { cont ->
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(context, uri)
            player.setOnCompletionListener {
                player.release()
                audioManager.abandonAudioFocusRequest(focusRequest)
                if (cont.isActive) cont.resume(ExecResult.Success)
            }
            player.setOnErrorListener { _, what, extra ->
                player.release()
                audioManager.abandonAudioFocusRequest(focusRequest)
                if (cont.isActive) cont.resume(ExecResult.Failure("MediaPlayer error: what=$what extra=$extra"))
                true
            }
            player.prepare()
            player.start()
        } catch (e: Exception) {
            player.release()
            audioManager.abandonAudioFocusRequest(focusRequest)
            if (cont.isActive) cont.resume(ExecResult.Failure("MediaPlayer setup failed: ${e.message}"))
        }

        cont.invokeOnCancellation {
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.release() }
            audioManager.abandonAudioFocusRequest(focusRequest)
        }
    }

    private fun playTts(
        ttsText: String?,
        audioManager: AudioManager,
        focusRequest: AudioFocusRequest,
    ): ExecResult {
        if (ttsText.isNullOrBlank()) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            return ExecResult.Failure("play_sound TTS mode: tts_text is empty")
        }

        val latch = CountDownLatch(1)
        var speakError: String? = null
        var tts: TextToSpeech? = null

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val langResult = tts?.setLanguage(Locale.getDefault())
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        latch.countDown()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        speakError = "TTS utterance error"
                        latch.countDown()
                    }
                })
                val params = android.os.Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "macro_tts")
                }
                tts?.speak(ttsText, TextToSpeech.QUEUE_FLUSH, params, "macro_tts")
            } else {
                speakError = "TextToSpeech initialization failed (status=$status)"
                latch.countDown()
            }
        }

        val completed = latch.await(30, TimeUnit.SECONDS)
        tts.shutdown()
        audioManager.abandonAudioFocusRequest(focusRequest)

        return when {
            !completed -> ExecResult.Failure("TTS timed out after 30 s")
            speakError != null -> ExecResult.Failure(speakError!!)
            else -> ExecResult.Success
        }
    }
}
