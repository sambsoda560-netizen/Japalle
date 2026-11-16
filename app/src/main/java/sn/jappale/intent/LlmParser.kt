package sn.jappale.intent

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import sn.jappale.Reglages
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Compréhension par un modèle de langage, via une API compatible OpenAI (Groq, xAI…).
 *
 * - Sans clé d'API, ou si l'appel échoue, on retombe sur les règles simples.
 * - L'IA ne fait que remplir une fiche (voir LlmReponse) : elle ne pilote pas le téléphone.
 * - Le « oui / non » de confirmation reste traité par des règles, sans IA.
 */
class LlmParser(
    private val reglages: Reglages,
    private val secours: RuleBasedParser = RuleBasedParser(),
) : IntentParser {

    private val fil = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /** Dernier problème rencontré avec l'IA, pour l'afficher pendant les tests. */
    @Volatile
    var derniereErreur: String? = null
        private set

    override fun parse(echanges: List<String>, onResult: (Intention) -> Unit) {
        derniereErreur = null
        val cle = reglages.cle
        if (cle.isBlank()) {
            secours.parse(echanges, onResult)
            return
        }
        val url = reglages.fournisseur.url
        val modele = reglages.modele
        fil.execute {
            val intention = try {
                LlmReponse.versIntention(appeler(url, cle, modele, echanges))
            } catch (e: Exception) {
                derniereErreur = e.message ?: e.javaClass.simpleName
                secours.comprendre(echanges.joinToString(" "))
            }
            main.post { onResult(intention) }
        }
    }

    override fun parseConfirmation(text: String): Boolean? = secours.parseConfirmation(text)

    private fun appeler(url: String, cle: String, modele: String, echanges: List<String>): String {
        val demande = echanges.mapIndexed { i, phrase ->
            if (i == 0) "Demande : $phrase" else "Précision donnée ensuite : $phrase"
        }.joinToString("\n")

        val corps = JSONObject()
            .put("model", modele)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", LlmReponse.CONSIGNE))
                    .put(JSONObject().put("role", "user").put("content", demande)),
            )
            .toString()

        val connexion = URL(url).openConnection() as HttpURLConnection
        try {
            connexion.requestMethod = "POST"
            connexion.connectTimeout = 8_000
            connexion.readTimeout = 20_000
            connexion.doOutput = true
            connexion.setRequestProperty("Content-Type", "application/json")
            connexion.setRequestProperty("Authorization", "Bearer $cle")
            connexion.outputStream.use { it.write(corps.toByteArray(Charsets.UTF_8)) }

            val code = connexion.responseCode
            if (code !in 200..299) {
                val erreur = connexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("Erreur $code de l'IA : ${erreur.take(200)}")
            }
            val reponse = connexion.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(reponse)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } finally {
            connexion.disconnect()
        }
    }
}
