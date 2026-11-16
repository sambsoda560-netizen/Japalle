package sn.jappale.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Voix → texte. Pour passer au wolof, écrire une autre classe qui respecte
 * cette interface (par exemple un appel à un serveur qui fait tourner un modèle wolof).
 */
interface SpeechToText {
    /** @param onReady appelé quand le micro écoute vraiment : avant, les mots sont perdus. */
    fun listen(onReady: () -> Unit, onResult: (String) -> Unit, onFailure: (String) -> Unit)
    fun release()
}

/** Reconnaissance vocale fournie par Android. À appeler depuis le fil principal. */
class AndroidSpeechToText(
    private val context: Context,
    private val locale: Locale = Locale.FRANCE,
) : SpeechToText {

    private var recognizer: SpeechRecognizer? = null

    override fun listen(onReady: () -> Unit, onResult: (String) -> Unit, onFailure: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onFailure("La reconnaissance vocale n'est pas disponible sur ce téléphone.")
            return
        }
        release()
        val nouveau = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = nouveau

        nouveau.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val texte = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (texte.isNullOrBlank()) onFailure("Je n'ai rien entendu.") else onResult(texte)
            }

            override fun onError(error: Int) {
                onFailure("Je n'ai pas bien entendu.")
            }

            override fun onReadyForSpeech(params: Bundle?) = onReady()
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val demande = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Laisser le temps de réfléchir : ne pas couper à la première petite pause.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_800L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1_800L)
        }
        nouveau.startListening(demande)
    }

    override fun release() {
        recognizer?.destroy()
        recognizer = null
    }
}
