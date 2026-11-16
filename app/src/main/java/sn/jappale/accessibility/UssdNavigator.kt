package sn.jappale.accessibility

/**
 * Ce que l'agent doit faire dans un menu USSD (#144#…) à la place de l'utilisateur.
 *
 * @param etapes  pour chaque écran de menu attendu, les mots à chercher dans les lignes
 *                (ex. d'abord "crédit", puis "moi")
 * @param montant la somme à saisir quand le menu la demande
 * @param numero  le numéro à saisir quand le menu le demande, s'il y en a un
 */
data class UssdPlan(
    val etapes: List<List<String>>,
    val montant: Int,
    val numero: String? = null,
)

sealed class UssdDecision {
    /** Taper cette valeur et l'envoyer. */
    data class Repondre(val valeur: String, val etapeSuivante: Int) : UssdDecision()

    /** Le menu demande le code secret : on s'arrête, c'est à l'utilisateur. */
    object CodeSecret : UssdDecision()

    /** Écran imprévu : on ne touche à rien. */
    data class Abandon(val raison: String) : UssdDecision()
}

/**
 * Lit le texte d'un écran USSD et décide quoi répondre, avec des règles fixes.
 * Aucune réponse n'est envoyée au hasard : si l'écran ne correspond à rien de prévu,
 * la décision est Abandon.
 */
object UssdNavigator {

    private val CODE_SECRET = Regex(
        """code secret|code confidentiel|mot de passe|\bpin\b""",
        RegexOption.IGNORE_CASE,
    )

    /** "1. Transfert", "2 Achat crédit", "3-Paiement", "4) Mon compte" */
    private val LIGNE_MENU = Regex("""^\s*(\d{1,2})\s*[-.:)]?\s*(\p{L}.*)$""")

    private val DEMANDE_MONTANT = Regex("""montant|somme|combien""", RegexOption.IGNORE_CASE)
    private val DEMANDE_NUMERO =
        Regex("""num[ée]ro|destinataire|b[ée]n[ée]ficiaire""", RegexOption.IGNORE_CASE)
    private val MOTS_CONFIRMER = listOf("confirm", "valid")
    private val NOMBRE = Regex("""\d+(?:[ .\u00A0]\d{3})*""")

    /** Vrai si le texte contient exactement ce montant ("500", pas "5000" ni "1500"). */
    private fun afficheLeMontant(texte: String, montant: Int): Boolean =
        NOMBRE.findAll(texte).any { n -> n.value.filter { it.isDigit() }.toLongOrNull() == montant.toLong() }

    fun decider(texte: String, plan: UssdPlan, etape: Int): UssdDecision {
        val lignes = texte.lines()
            .mapNotNull { LIGNE_MENU.find(it) }
            .map { it.groupValues[1] to it.groupValues[2].lowercase() }

        // 1. Un menu à choix : on cherche la ligne voulue.
        if (lignes.size >= 2) {
            for (i in etape until plan.etapes.size) {
                for (mot in plan.etapes[i]) {
                    val ligne = lignes.firstOrNull { it.second.contains(mot) }
                    if (ligne != null) return UssdDecision.Repondre(ligne.first, i + 1)
                }
            }
            // Écran "1. Confirmer / 2. Annuler" : seulement si le bon montant est affiché.
            val confirmer = lignes.firstOrNull { l -> MOTS_CONFIRMER.any { l.second.contains(it) } }
            if (confirmer != null && afficheLeMontant(texte, plan.montant)) {
                return UssdDecision.Repondre(confirmer.first, etape)
            }
            return UssdDecision.Abandon("Je n'ai pas trouvé le bon choix dans le menu.")
        }

        // 2. Une question ouverte.
        if (CODE_SECRET.containsMatchIn(texte)) return UssdDecision.CodeSecret
        if (DEMANDE_MONTANT.containsMatchIn(texte)) {
            return UssdDecision.Repondre(plan.montant.toString(), etape)
        }
        if (DEMANDE_NUMERO.containsMatchIn(texte)) {
            val numero = plan.numero
                ?: return UssdDecision.Abandon("Le menu demande un numéro que je n'ai pas.")
            return UssdDecision.Repondre(numero, etape)
        }
        return UssdDecision.Abandon("Je ne comprends pas ce que le menu demande.")
    }
}
