package sn.jappale

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import sn.jappale.actions.ActionRunner
import sn.jappale.intent.IntentParser
import sn.jappale.intent.Intention
import sn.jappale.intent.RuleBasedParser
import sn.jappale.voice.AndroidSpeechToText
import sn.jappale.voice.Speaker
import sn.jappale.voice.SpeechToText

/**
 * L'écran unique : un bouton.
 * REPOS → ECOUTE_DEMANDE → ECOUTE_CONFIRMATION → EXECUTION → REPOS
 */
class MainActivity : Activity() {

    private enum class Etat { REPOS, ECOUTE_DEMANDE, ECOUTE_CONFIRMATION, EXECUTION }

    private lateinit var bouton: Button
    private lateinit var texteEtat: TextView
    private lateinit var oreille: SpeechToText
    private lateinit var voix: Speaker
    private lateinit var actions: ActionRunner
    private val cerveau: IntentParser = RuleBasedParser()

    private var etat = Etat.REPOS
    private var enAttente: Intention? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bouton = findViewById(R.id.bouton)
        texteEtat = findViewById(R.id.etat)

        oreille = AndroidSpeechToText(this)
        voix = Speaker(this)
        actions = ActionRunner(this) { message -> dire(message) }

        bouton.setOnClickListener { surAppui() }
    }

    override fun onDestroy() {
        oreille.release()
        voix.release()
        super.onDestroy()
    }

    private fun surAppui() {
        if (etat != Etat.REPOS) return
        val manquantes = PERMISSIONS.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (Manifest.permission.RECORD_AUDIO in manquantes) {
            requestPermissions(manquantes.toTypedArray(), DEMANDE_PERMISSIONS)
            return
        }
        ecouterDemande()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val microAccorde = checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (requestCode == DEMANDE_PERMISSIONS && microAccorde) {
            ecouterDemande()
        } else {
            dire("J'ai besoin du micro pour t'écouter.")
        }
    }

    private fun ecouterDemande() {
        etat = Etat.ECOUTE_DEMANDE
        afficher("Je t'écoute…")
        oreille.listen(onResult = ::surDemande, onFailure = ::surEchec)
    }

    private fun surDemande(phrase: String) {
        val intention = cerveau.parse(phrase)
        if (intention is Intention.Inconnue) {
            retourAuRepos("Je n'ai pas compris. Appuie et répète.")
            return
        }
        enAttente = intention
        val question = intention.question()
        afficher(question)
        voix.say(question) {
            etat = Etat.ECOUTE_CONFIRMATION
            oreille.listen(onResult = ::surConfirmation, onFailure = ::surEchec)
        }
    }

    private fun surConfirmation(phrase: String) {
        val intention = enAttente
        when (cerveau.parseConfirmation(phrase)) {
            true -> if (intention != null) {
                etat = Etat.EXECUTION
                enAttente = null
                actions.run(intention)
                etat = Etat.REPOS
            } else {
                retourAuRepos("Appuie et répète ta demande.")
            }
            false -> retourAuRepos("D'accord, j'annule.")
            // Dans le doute, on n'agit pas.
            null -> retourAuRepos("Je n'ai pas compris ta réponse. J'annule. Appuie pour recommencer.")
        }
    }

    private fun surEchec(message: String) {
        retourAuRepos("$message Appuie pour recommencer.")
    }

    private fun retourAuRepos(message: String) {
        enAttente = null
        etat = Etat.REPOS
        dire(message)
    }

    private fun dire(message: String) {
        afficher(message)
        voix.say(message)
    }

    private fun afficher(message: String) {
        texteEtat.text = message
    }

    private companion object {
        const val DEMANDE_PERMISSIONS = 1
        val PERMISSIONS = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
        )
    }
}
