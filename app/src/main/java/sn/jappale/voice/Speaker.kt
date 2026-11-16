package sn.jappale.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Texte → voix, avec la synthèse vocale d'Android.
 * À appeler depuis le fil principal. Pour le wolof : remplacer par une voix wolof
 * ou par des audios préenregistrés.
 */
class Speaker(context: Context, private val locale: Locale = Locale.FRANCE) {

    private val main = Handler(Looper.getMainLooper())
    private val rappels = HashMap<String, () -> Unit>()
    private var compteur = 0
    private var pret = false
    private var enPanne = false
    private var enAttente: Pair<String, (() -> Unit)?>? = null
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { statut ->
            main.post {
                if (statut == TextToSpeech.SUCCESS) {
                    tts.language = locale
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) = termine(utteranceId)

                        @Deprecated("Ancienne signature, encore exigée par la classe")
                        override fun onError(utteranceId: String?) = termine(utteranceId)
                    })
                    pret = true
                } else {
                    enPanne = true
                }
                enAttente?.let { (texte, apres) -> say(texte, apres) }
                enAttente = null
            }
        }
    }

    /** Dit le texte, puis appelle [apres] sur le fil principal. */
    fun say(texte: String, apres: (() -> Unit)? = null) {
        when {
            enPanne -> apres?.invoke()
            !pret -> enAttente = texte to apres
            else -> {
                val id = "phrase-${compteur++}"
                if (apres != null) rappels[id] = apres
                tts.speak(texte, TextToSpeech.QUEUE_FLUSH, null, id)
            }
        }
    }

    fun release() {
        rappels.clear()
        tts.stop()
        tts.shutdown()
    }

    private fun termine(id: String?) {
        main.post { rappels.remove(id)?.invoke() }
    }
}
