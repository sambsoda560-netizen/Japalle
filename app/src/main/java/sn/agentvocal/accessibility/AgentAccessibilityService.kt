package sn.agentvocal.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import sn.agentvocal.Config

/**
 * Joue un UiScript dans une autre appli (Wave pour l'instant).
 *
 * Garde-fous :
 * - ne fait rien tant que start() n'a pas été appelé, donc tant que
 *   l'utilisateur n'a pas confirmé à voix haute ;
 * - n'agit que dans l'appli visée par le script ;
 * - abandonne si une étape n'aboutit pas dans le délai prévu ;
 * - ne tape jamais de code secret et ne valide jamais un paiement.
 */
class AgentAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var script: UiScript? = null
    private var index = 0
    private var onEnd: ((String) -> Unit)? = null

    private val delaiDepasse = Runnable {
        finish("Je n'ai pas réussi à préparer l'envoi. Continue toi-même dans l'appli.")
    }

    fun start(script: UiScript, onEnd: (String) -> Unit) {
        this.script = script
        this.index = 0
        this.onEnd = onEnd
        armerDelai()
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        handler.removeCallbacks(delaiDepasse)
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val enCours = script ?: return
        if (event?.packageName?.toString() != enCours.targetPackage) return
        val racine = rootInActiveWindow ?: return
        if (Config.DEBUG_ECRAN) journaliser(racine, 0)
        avancer(enCours, racine)
    }

    private fun avancer(enCours: UiScript, racine: AccessibilityNodeInfo) {
        val etape = enCours.steps.getOrNull(index) ?: run {
            finish("C'est prêt.")
            return
        }
        val reussi = when (etape) {
            is Step.ClickText -> cliquer(racine, etape.label)
            is Step.TypeText -> ecrire(racine, etape.value)
            is Step.StopForUser -> {
                finish(etape.message)
                return
            }
        }
        if (!reussi) return // l'écran n'est pas encore prêt : on réessaiera au prochain événement

        index++
        val suivante = enCours.steps.getOrNull(index)
        if (suivante is Step.StopForUser) finish(suivante.message) else armerDelai()
    }

    private fun cliquer(racine: AccessibilityNodeInfo, texte: String): Boolean {
        val cible = racine.findAccessibilityNodeInfosByText(texte)?.firstOrNull() ?: return false
        var noeud: AccessibilityNodeInfo? = cible
        while (noeud != null && !noeud.isClickable) noeud = noeud.parent
        return noeud?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    private fun ecrire(racine: AccessibilityNodeInfo, valeur: String): Boolean {
        val actif = racine.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val champActif = actif?.takeIf { it.isEditable && estVide(it) }
        val champ = champActif ?: premierChampVide(racine) ?: return false

        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                valeur,
            )
        }
        return champ.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun estVide(noeud: AccessibilityNodeInfo): Boolean =
        noeud.isShowingHintText || noeud.text.isNullOrEmpty()

    private fun premierChampVide(noeud: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (noeud.isEditable && estVide(noeud)) return noeud
        for (i in 0 until noeud.childCount) {
            val enfant = noeud.getChild(i) ?: continue
            premierChampVide(enfant)?.let { return it }
        }
        return null
    }

    private fun armerDelai() {
        handler.removeCallbacks(delaiDepasse)
        handler.postDelayed(delaiDepasse, Config.SCRIPT_TIMEOUT_MS)
    }

    private fun finish(message: String) {
        handler.removeCallbacks(delaiDepasse)
        val rappel = onEnd
        script = null
        onEnd = null
        rappel?.invoke(message)
    }

    /** Calibration uniquement (Config.DEBUG_ECRAN) : écrit l'arbre de l'écran dans Logcat. */
    private fun journaliser(noeud: AccessibilityNodeInfo, niveau: Int) {
        val marge = "  ".repeat(niveau)
        Log.d(
            TAG,
            "$marge${noeud.className} texte=${noeud.text} desc=${noeud.contentDescription} " +
                "cliquable=${noeud.isClickable} editable=${noeud.isEditable}",
        )
        for (i in 0 until noeud.childCount) {
            noeud.getChild(i)?.let { journaliser(it, niveau + 1) }
        }
    }

    companion object {
        private const val TAG = "AgentVocal"

        /** Non nul quand l'utilisateur a activé le service dans les paramètres. */
        @Volatile
        var instance: AgentAccessibilityService? = null
            private set
    }
}
