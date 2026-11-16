package sn.jappale

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import sn.jappale.accessibility.AgentAccessibilityService
import sn.jappale.actions.ActionRunner
import sn.jappale.intent.Intention
import sn.jappale.intent.LlmParser
import sn.jappale.voice.AndroidSpeechToText
import sn.jappale.voice.Speaker
import sn.jappale.voice.SpeechToText

/**
 * L'écran unique : un bouton.
 * REPOS → ECOUTE_DEMANDE → REFLECHIT → PARLE → ECOUTE_CONFIRMATION → EXECUTION → REPOS
 * (avec un retour à ECOUTE_DEMANDE quand il manque une information)
 */
class MainActivity : Activity() {

    private enum class Etat { REPOS, PREPARE, ECOUTE_DEMANDE, REFLECHIT, PARLE, ECOUTE_CONFIRMATION, EXECUTION }

    private lateinit var bouton: View
    private lateinit var halo: View
    private lateinit var texteEtat: TextView
    private lateinit var lienReglage: TextView
    private var releveEnCours = false
    private lateinit var pulsation: ObjectAnimator
    private lateinit var oreille: SpeechToText
    private lateinit var voix: Speaker
    private lateinit var actions: ActionRunner
    private lateinit var reglages: Reglages
    private lateinit var cerveau: LlmParser
    private val echanges = mutableListOf<String>()
    private var precisions = 0
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

        reglages = Reglages(this)
        cerveau = LlmParser(reglages)
        oreille = AndroidSpeechToText(this)
        voix = Speaker(this)
        actions = ActionRunner(this) { message, detail ->
            afficher(message, detail, etiquette = "L'écran affichait")
            voix.say(message)
        }

        bouton.setOnClickListener { surAppui() }
        lienReglage = findViewById(R.id.reglage)
        lienReglage.setOnClickListener { basculerReleve() }
        findViewById<View>(R.id.cerveau).setOnClickListener { ouvrirReglagesIA() }
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
        echanges.clear()
        precisions = 0
        ecouter()
    }

    /** [invite] : la question en cours, à laisser affichée pendant l'écoute. */
    private fun ecouter(invite: String? = null) {
        etat = Etat.PREPARE
        afficher(invite ?: "Un instant…")
        oreille.listen(
            onReady = {
                signalerEcoute(Etat.ECOUTE_DEMANDE)
                afficher(invite ?: "Parle maintenant, je t'écoute.")
            },
            onResult = ::surDemande,
            onFailure = { message -> retourAuRepos("$message Appuie pour recommencer.") },
        )
    }

    private fun surDemande(phrase: String) {
        echanges.add(phrase)
        etat = Etat.REFLECHIT
        afficher("Je réfléchis…", phrase)
        cerveau.parse(echanges.toList()) { intention ->
            if (!isDestroyed) surIntention(intention, phrase)
        }
    }

    private fun surIntention(intention: Intention, phrase: String) {
        // Pendant les tests : si l'IA a échoué, on montre pourquoi.
        val detail = cerveau.derniereErreur?.let { "$phrase » — IA indisponible : « $it" } ?: phrase
        when (intention) {
            is Intention.Inconnue ->
                retourAuRepos("Je n'ai pas compris. Appuie et répète.", entendu = detail)

            is Intention.Precision -> {
                if (precisions >= PRECISIONS_MAX) {
                    retourAuRepos("Je n'ai pas tout compris. Appuie et recommence.", entendu = detail)
                    return
                }
                precisions++
                etat = Etat.PARLE
                afficher(intention.question, detail)
                voix.say(intention.question) {
                    main.postDelayed({ ecouter(invite = intention.question) }, PAUSE_AVANT_ECOUTE_MS)
                }
            }

            else -> {
                enAttente = intention
                essaisConfirmation = 0
                poserQuestion(intention.question())
            }
        }
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
            oreille.listen(
                onReady = { signalerEcoute(Etat.ECOUTE_CONFIRMATION) },
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

    // ---- Réglages de l'IA ----

    /** Choix du service d'IA et saisie de la clé, gardés sur le téléphone. */
    private fun ouvrirReglagesIA() {
        val marge = (20 * resources.displayMetrics.density).toInt()
        val fournisseurs = Fournisseur.values()
        val groupe = RadioGroup(this)
        val boutons = fournisseurs.map { fournisseur ->
            RadioButton(this).apply {
                id = View.generateViewId()
                text = fournisseur.nom
                groupe.addView(this)
            }
        }
        val champCle = EditText(this).apply {
            hint = getString(R.string.cerveau_cle)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(reglages.cle)
        }
        val champModele = EditText(this).apply {
            hint = getString(R.string.cerveau_modele)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setText(reglages.modele)
        }
        boutons[reglages.fournisseur.ordinal].isChecked = true
        groupe.setOnCheckedChangeListener { _, id ->
            val choisi = fournisseurs[boutons.indexOfFirst { it.id == id }.coerceAtLeast(0)]
            champModele.setText(choisi.modeleParDefaut)
        }
        val contenu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(marge, marge / 2, marge, 0)
            addView(groupe)
            addView(champCle)
            addView(champModele)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.cerveau_titre)
            .setView(contenu)
            .setPositiveButton(R.string.enregistrer) { _, _ ->
                val choisi = fournisseurs[boutons.indexOfFirst { it.isChecked }.coerceAtLeast(0)]
                reglages.enregistrer(
                    choisi,
                    champCle.text.toString().trim(),
                    champModele.text.toString().trim(),
                )
                afficher(
                    if (reglages.cle.isBlank()) {
                        "Pas de clé : j'utilise la compréhension simple."
                    } else {
                        "IA réglée sur ${choisi.nom}. Appuie sur le bouton pour essayer."
                    },
                )
            }
            .setNegativeButton(R.string.annuler, null)
            .show()
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

    /** Le micro écoute vraiment : bouton doré, halo qui bat, petite vibration. */
    private fun signalerEcoute(nouvelEtat: Etat) {
        etat = nouvelEtat
        bouton.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

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
        const val PRECISIONS_MAX = 3
        const val PAUSE_AVANT_ECOUTE_MS = 400L
        val PERMISSIONS = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
        )
    }
}
