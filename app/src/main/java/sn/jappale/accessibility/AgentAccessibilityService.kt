package sn.jappale.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import sn.jappale.Config

/**
 * Agit à la place de l'utilisateur dans deux endroits :
 * - les fenêtres USSD (#144#…), en suivant un UssdPlan ;
 * - une autre appli (Wave), en jouant un UiScript.
 *
 * Garde-fous :
 * - ne fait rien tant que start()/startUssd() n'a pas été appelé, donc tant que
 *   l'utilisateur n'a pas confirmé à voix haute ;
 * - n'agit que dans l'appli visée ;
 * - abandonne si l'écran attendu n'arrive pas, ou s'il est imprévu ;
 * - ne tape jamais de code secret : il s'arrête dès qu'on le demande.
 */
class AgentAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var onEnd: ((String, String?) -> Unit)? = null

    // Mode script (Wave)
    private var script: UiScript? = null
    private var index = 0

    // Mode USSD
    private var plan: UssdPlan? = null
    private var etapeUssd = 0
    private var reponsesUssd = 0
    private var dernierEcranUssd: String? = null

    /** Délai dépassé : on dit où ça bloque et on note l'écran, pour pouvoir régler le script. */
    private val delaiDepasse = Runnable {
        val blocage = when (val etape = script?.steps?.getOrNull(index)) {
            is Step.ClickText -> "Je ne trouve pas le bouton « ${etape.labels.first()} »."
            is Step.TypeText -> "Je ne trouve pas où écrire « ${etape.value} »."
            else -> "Je n'ai pas réussi à aller au bout."
        }
        val ecran = rootInActiveWindow?.let { racine ->
            StringBuilder("appli ${racine.packageName}").also { decrire(racine, it) }.toString().take(1_500)
        }
        finish("$blocage Continue toi-même.", detail = ecran)
    }

    fun start(script: UiScript, onEnd: (String, String?) -> Unit) {
        reinitialiser()
        this.script = script
        this.onEnd = onEnd
        armerDelai(script.premiereAttenteMs)
    }

    fun startUssd(plan: UssdPlan, onEnd: (String, String?) -> Unit) {
        reinitialiser()
        this.plan = plan
        this.onEnd = onEnd
        armerDelai(Config.USSD_TIMEOUT_MS)
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
        if (releveEnCours) {
            relever()
            return
        }
        if (script == null && plan == null) return
        val racine = rootInActiveWindow ?: return
        val paquet = racine.packageName?.toString() ?: return

        val planEnCours = plan
        val scriptEnCours = script
        if (planEnCours != null && paquet in Config.USSD_PACKAGES) {
            avancerUssd(planEnCours, racine)
        } else if (scriptEnCours != null && paquet == scriptEnCours.targetPackage) {
            if (Config.DEBUG_ECRAN) journaliser(racine, 0)
            avancer(scriptEnCours, racine)
        }
    }

    // ---- Mode réglage : relever les écrans d'une appli pour écrire son script ----

    private var releveEnCours = false
    private val releve = LinkedHashSet<String>()

    /** Commence à noter les écrans des autres applis, à la demande de l'utilisateur. */
    fun demarrerReleve() {
        reinitialiser()
        releve.clear()
        releveEnCours = true
        ecouterToutesLesApplis(true)
    }

    /** Arrête le relevé et renvoie le texte à partager. */
    fun arreterReleve(): String {
        releveEnCours = false
        ecouterToutesLesApplis(false)
        val texte = releve.joinToString("\n\n")
        releve.clear()
        return texte.take(60_000)
    }

    private fun compter(noeud: AccessibilityNodeInfo): Int {
        var total = 1
        for (i in 0 until noeud.childCount) {
            noeud.getChild(i)?.let { total += compter(it) }
        }
        return total
    }

    private fun ecouterToutesLesApplis(toutes: Boolean) {
        val info = serviceInfo ?: return
        info.packageNames = if (toutes) null else Config.PAQUETS_AUTORISES.toTypedArray()
        serviceInfo = info
    }

    private fun relever() {
        if (releve.size >= Config.RELEVE_ECRANS_MAX) return
        val racine = rootInActiveWindow ?: return
        val paquet = racine.packageName?.toString() ?: return
        if (paquet == packageName || PAQUETS_IGNORES.any { paquet.contains(it) }) return
        val lignes = StringBuilder()
        decrire(racine, lignes)
        if (lignes.isEmpty()) {
            // À distinguer d'un oubli : l'appli est bien à l'écran, mais ne montre rien.
            lignes.append("\n(rien de lisible : ")
                .append(compter(racine))
                .append(" éléments, sans texte ni bouton)")
        }
        releve.add("=== écran de $paquet ===$lignes".take(4_000))
    }

    /** Une ligne par élément utile : [B] = cliquable, [E] = champ de saisie. Les chiffres sont masqués. */
    private fun decrire(noeud: AccessibilityNodeInfo, sortie: StringBuilder) {
        val texte = (noeud.text ?: noeud.contentDescription)?.toString()?.trim().orEmpty()
        val actionnable = noeud.isEditable || noeud.isClickable
        if (texte.isNotEmpty() || actionnable) {
            val marque = when {
                noeud.isEditable -> "[E] "
                noeud.isClickable -> "[B] "
                else -> ""
            }
            sortie.append('\n').append(marque)
                .append(if (texte.isEmpty()) "(sans texte)" else texte.replace(CHIFFRES, "#"))
            // Le nom technique de l'élément aide à écrire le script quand il n'a pas de texte.
            val nom = noeud.viewIdResourceName?.substringAfterLast('/')
            if (actionnable && nom != null) sortie.append("  {").append(nom).append('}')
        }
        for (i in 0 until noeud.childCount) {
            noeud.getChild(i)?.let { decrire(it, sortie) }
        }
    }

    // ---- Mode USSD ----

    private fun avancerUssd(planEnCours: UssdPlan, racine: AccessibilityNodeInfo) {
        val texte = textes(racine).joinToString("\n").trim()
        if (texte.isEmpty()) return
        if (Config.DEBUG_ECRAN) Log.d(TAG, "USSD : $texte")

        val champ = premierChamp(racine) { true }
        if (champ == null) {
            // Pas de champ de saisie : soit le réseau cherche encore, soit c'est un message final.
            val boutonOk = boutonCliquable(racine, "OK")
            if (boutonOk != null) finish("Le téléphone répond : $texte")
            return
        }

        if (texte == dernierEcranUssd) return // déjà traité, on attend l'écran suivant
        dernierEcranUssd = texte

        when (val decision = UssdNavigator.decider(texte, planEnCours, etapeUssd)) {
            is UssdDecision.CodeSecret ->
                finish("C'est prêt. Tape ton code secret pour valider.")

            is UssdDecision.Abandon ->
                finish(decision.raison + " Continue toi-même.", detail = texte)

            is UssdDecision.Repondre -> {
                if (reponsesUssd >= Config.USSD_REPONSES_MAX) {
                    finish("Le menu est trop long. Continue toi-même.", detail = texte)
                    return
                }
                val envoye = remplir(champ, decision.valeur) &&
                    Config.USSD_BOUTONS_ENVOI.any { cliquer(racine, it) }
                if (!envoye) {
                    finish("Je n'arrive pas à répondre au menu. Continue toi-même.", detail = texte)
                    return
                }
                etapeUssd = decision.etapeSuivante
                reponsesUssd++
                armerDelai(Config.USSD_TIMEOUT_MS)
            }
        }
    }

    /** Les textes affichés, sans les boutons ni les champs de saisie. */
    private fun textes(noeud: AccessibilityNodeInfo, sortie: MutableList<String> = mutableListOf()): List<String> {
        val estBouton = noeud.className?.toString()?.endsWith("Button") == true
        val texte = noeud.text?.toString()
        if (!estBouton && !noeud.isEditable && !texte.isNullOrBlank()) sortie.add(texte)
        for (i in 0 until noeud.childCount) {
            noeud.getChild(i)?.let { textes(it, sortie) }
        }
        return sortie
    }

    // ---- Mode script (Wave) ----

    private fun avancer(enCours: UiScript, racine: AccessibilityNodeInfo) {
        val etape = enCours.steps.getOrNull(index) ?: run {
            finish("C'est prêt.")
            return
        }
        val reussi = when (etape) {
            is Step.ClickText -> etape.labels.any { cliquer(racine, it) }
            is Step.TypeText -> ecrire(racine, etape.value)
            is Step.StopForUser -> {
                finish(etape.message)
                return
            }
        }
        if (!reussi) return // l'écran n'est pas encore prêt : on réessaiera au prochain événement

        index++
        val suivante = enCours.steps.getOrNull(index)
        if (suivante is Step.StopForUser) finish(suivante.message) else armerDelai(Config.SCRIPT_TIMEOUT_MS)
    }

    private fun ecrire(racine: AccessibilityNodeInfo, valeur: String): Boolean {
        val actif = racine.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val champActif = actif?.takeIf { it.isEditable && estVide(it) }
        val champ = champActif ?: premierChamp(racine, ::estVide) ?: return false
        return remplir(champ, valeur)
    }

    // ---- Outils communs ----

    private fun cliquer(racine: AccessibilityNodeInfo, texte: String): Boolean =
        boutonCliquable(racine, texte)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false

    /** Le premier élément cliquable qui porte ce texte (lui-même ou l'un de ses parents). */
    private fun boutonCliquable(racine: AccessibilityNodeInfo, texte: String): AccessibilityNodeInfo? {
        val trouves = racine.findAccessibilityNodeInfosByText(texte) ?: return null
        // D'abord les éléments dont le texte est exactement celui cherché ("Crédit"),
        // avant ceux qui le contiennent seulement ("Achat crédit du 12 mars").
        val (exacts, autres) = trouves.partition { noeud ->
            noeud.text?.toString()?.trim().equals(texte, ignoreCase = true) ||
                noeud.contentDescription?.toString()?.trim().equals(texte, ignoreCase = true)
        }
        for (noeud in exacts + autres) {
            var candidat: AccessibilityNodeInfo? = noeud
            while (candidat != null && !candidat.isClickable) candidat = candidat.parent
            if (candidat != null) return candidat
        }
        return null
    }

    private fun remplir(champ: AccessibilityNodeInfo, valeur: String): Boolean {
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

    private fun premierChamp(
        noeud: AccessibilityNodeInfo,
        condition: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (noeud.isEditable && condition(noeud)) return noeud
        for (i in 0 until noeud.childCount) {
            val enfant = noeud.getChild(i) ?: continue
            premierChamp(enfant, condition)?.let { return it }
        }
        return null
    }

    private fun armerDelai(duree: Long) {
        handler.removeCallbacks(delaiDepasse)
        handler.postDelayed(delaiDepasse, duree)
    }

    private fun reinitialiser() {
        handler.removeCallbacks(delaiDepasse)
        script = null
        index = 0
        plan = null
        etapeUssd = 0
        reponsesUssd = 0
        dernierEcranUssd = null
        onEnd = null
    }

    private fun finish(message: String, detail: String? = null) {
        val rappel = onEnd
        reinitialiser()
        rappel?.invoke(message, detail)
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
        private const val TAG = "Jappale"
        private val CHIFFRES = Regex("""\d""")

        /** Écrans sans intérêt pour un relevé : accueil du téléphone, clavier, barre système. */
        private val PAQUETS_IGNORES = listOf("launcher", "systemui", "inputmethod", "keyboard")

        /** Non nul quand l'utilisateur a activé le service dans les paramètres. */
        @Volatile
        var instance: AgentAccessibilityService? = null
            private set
    }
}
