package sn.jappale

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.TextView
import sn.jappale.accessibility.AgentAccessibilityService
import sn.jappale.actions.ActionRunner
import sn.jappale.intent.IntentParser
import sn.jappale.intent.Intention
import sn.jappale.intent.RuleBasedParser
import sn.jappale.voice.AndroidSpeechToText
import sn.jappale.voice.Speaker
import sn.jappale.voice.SpeechToText

/**
 * L'écran unique : un bouton.
 * REPOS → ECOUTE_DEMANDE → PARLE → ECOUTE_CONFIRMATION → EXECUTION → REPOS
 */
class MainActivity : Activity() {

    private enum class Etat { REPOS, ECOUTE_DEMANDE, PARLE, ECOUTE_CONFIRMATION, EXECUTION }

    private lateinit var bouton: View
    private lateinit var halo: View
    private lateinit var texteEtat: TextView
    private lateinit var lienReglage: TextView
    private var releveEnCours = false
    private lateinit var pulsation: ObjectAnimator
    private lateinit var oreille: SpeechToText
    private lateinit var voix: Speaker
    private lateinit var actions: ActionRunner
    private val cerveau: IntentParser = RuleBasedParser()
    private val main = Handler(Looper.getMainLooper())

    private var etat = Etat.REPOS
        set(valeur) {
            field = valeur
            mettreAJourVisuel()
        }
    private var enAttente: Intention? = null
    private var essaisConfirmation = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bouton = findViewById(R.id.bouton)
        halo = findViewById(R.id.halo)
        texteEtat = findViewById(R.id.etat)

        pulsation = ObjectAnimator.ofPropertyValuesHolder(
            halo,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.12f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.12f),
        ).apply {
            duration = 700
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }

        oreille = AndroidSpeechToText(this)
        voix = Speaker(this)
        actions = ActionRunner(this) { message, detail ->
            afficher(message, detail, etiquette = "Le menu disait")
            voix.say(message)
        }

        bouton.setOnClickListener { surAppui() }
        lienReglage = findViewById(R.id.reglage)
        lienReglage.setOnClickListener { basculerReleve() }
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        pulsation.cancel()
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

    // ---- 1. La demande ----

    private fun ecouterDemande() {
        etat = Etat.ECOUTE_DEMANDE
        afficher("Je t'écoute…")
        oreille.listen(
            onResult = ::surDemande,
            onFailure = { message -> retourAuRepos("$message Appuie pour recommencer.") },
        )
    }

    private fun surDemande(phrase: String) {
        val intention = cerveau.parse(phrase)
        if (intention is Intention.Inconnue) {
            retourAuRepos("Je n'ai pas compris. Appuie et répète.", entendu = phrase)
            return
        }
        enAttente = intention
        essaisConfirmation = 0
        poserQuestion(intention.question())
    }

    // ---- 2. La confirmation ----

    private fun poserQuestion(question: String, entendu: String? = null) {
        etat = Etat.PARLE
        afficher(question, entendu)
        voix.say(question) { ecouterConfirmation() }
    }

    private fun ecouterConfirmation() {
        essaisConfirmation++
        // Petite pause : le micro ne doit pas capter la fin de la voix de l'appli.
        main.postDelayed({
            etat = Etat.ECOUTE_CONFIRMATION
            oreille.listen(
                onResult = ::surConfirmation,
                onFailure = { redemanderConfirmation(entendu = null) },
            )
        }, PAUSE_AVANT_ECOUTE_MS)
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
            null -> redemanderConfirmation(entendu = phrase)
        }
    }

    /** Réponse pas claire : on redemande une fois. Dans le doute, on n'agit jamais. */
    private fun redemanderConfirmation(entendu: String?) {
        if (essaisConfirmation >= ESSAIS_MAX) {
            retourAuRepos("Je n'ai pas compris ta réponse. J'annule. Appuie pour recommencer.", entendu)
            return
        }
        poserQuestion("Dis seulement oui, ou non.", entendu)
    }

    // ---- Mode réglage ----

    /**
     * Sert à apprendre les écrans d'une appli (Wave, Orange Money…) pour écrire son script :
     * l'utilisateur fait l'opération à la main, l'appli note les boutons et les champs,
     * puis propose d'envoyer ce relevé. Les chiffres sont masqués.
     */
    private fun basculerReleve() {
        val service = AgentAccessibilityService.instance
        if (service == null) {
            dire("Active d'abord Jàppale dans Accessibilité, puis reviens.")
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        if (!releveEnCours) {
            service.demarrerReleve()
            releveEnCours = true
            lienReglage.setText(R.string.reglage_terminer)
            afficher(getString(R.string.reglage_consigne))
            return
        }
        val texte = service.arreterReleve()
        releveEnCours = false
        lienReglage.setText(R.string.reglage)
        if (texte.isBlank()) {
            afficher("Je n'ai rien pu relever. L'appli protège peut-être ses écrans.")
            return
        }
        afficher("Relevé terminé. Envoie-le pour régler l'agent.")
        val partage = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, texte)
        startActivity(Intent.createChooser(partage, getString(R.string.reglage_envoyer)))
    }

    // ---- Affichage et voix ----

    private fun retourAuRepos(message: String, entendu: String? = null) {
        enAttente = null
        etat = Etat.REPOS
        afficher(message, entendu)
        voix.say(message)
    }

    private fun dire(message: String) {
        afficher(message)
        voix.say(message)
    }

    /** [detail] : ce que le micro a compris ou ce que le menu affichait, pour aider à régler l'appli. */
    private fun afficher(message: String, detail: String? = null, etiquette: String = "J'ai entendu") {
        texteEtat.text = if (detail == null) message else "$message\n\n$etiquette : « $detail »"
    }

    private fun mettreAJourVisuel() {
        val ecoute = etat == Etat.ECOUTE_DEMANDE || etat == Etat.ECOUTE_CONFIRMATION
        bouton.isActivated = ecoute
        if (ecoute) {
            if (!pulsation.isStarted) pulsation.start()
        } else {
            pulsation.cancel()
            halo.scaleX = 1f
            halo.scaleY = 1f
        }
    }

    private companion object {
        const val DEMANDE_PERMISSIONS = 1
        const val ESSAIS_MAX = 2
        const val PAUSE_AVANT_ECOUTE_MS = 400L
        val PERMISSIONS = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
        )
    }
}
