package sn.jappale.intent

/**
 * Compréhension par mots-clés, pour tester la chaîne en français.
 *
 * Limites connues :
 * - les montants doivent arriver en chiffres ("5000", "5 000", "5 mille") ;
 *   "cinq mille" en toutes lettres n'est pas compris ;
 * - un seul mot pour le destinataire ("Fatou"), ou un numéro de téléphone.
 */
class RuleBasedParser : IntentParser {

    override fun parse(text: String): Intention {
        val phrase = text.trim()
        val minuscules = phrase.lowercase()

        val parleDeCredit = minuscules.contains("crédit") || minuscules.contains("credit")
        val parleDEnvoi = MOTS_ENVOI.any { minuscules.contains(it) }

        if (parleDeCredit) {
            val montant = montant(phrase) ?: return Intention.Inconnue
            return Intention.AcheterCredit(montant)
        }

        if (parleDEnvoi) {
            val trouve = DESTINATAIRE.findAll(phrase).lastOrNull() ?: return Intention.Inconnue
            val brut = trouve.groupValues[1]
            // On retire le destinataire avant de chercher le montant,
            // pour ne pas prendre un numéro de téléphone pour une somme.
            val reste = phrase.removeRange(trouve.range)
            val montant = montant(reste) ?: return Intention.Inconnue
            val service =
                if (minuscules.contains("orange")) Service.ORANGE_MONEY else Service.WAVE
            return Intention.EnvoyerArgent(montant, nettoyerDestinataire(brut), service)
        }

        return Intention.Inconnue
    }

    override fun parseConfirmation(text: String): Boolean? {
        val mots = text.lowercase().split(Regex("""[^\p{L}]+""")).filter { it.isNotEmpty() }
        if (mots.any { it in MOTS_NON }) return false
        if (mots.any { it in MOTS_OUI }) return true
        // "c'est ça", "c'est bon" : la réponse la plus naturelle à une question
        if (EXPRESSION_OUI.containsMatchIn(text.lowercase())) return true
        return null
    }

    private fun montant(phrase: String): Int? {
        val trouve = MONTANT.find(phrase) ?: return null
        val chiffres = trouve.groupValues[1].filter { it.isDigit() }
        var valeur = chiffres.toLongOrNull() ?: return null
        if (trouve.groupValues[2].isNotBlank()) valeur *= 1000
        return if (valeur in 1..10_000_000) valeur.toInt() else null
    }

    private fun nettoyerDestinataire(brut: String): String {
        val estUnNumero = brut.any { it.isDigit() }
        return if (estUnNumero) {
            brut.filter { it.isDigit() || it == '+' }
        } else {
            brut.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private companion object {
        val MOTS_ENVOI = listOf("envoi", "envoy", "transf", "vire")
        val MOTS_OUI = setOf(
            "oui", "ouais", "ouai", "wi", "yes", "ok", "okay", "accord", "exact", "exactement",
            "voilà", "voila", "confirme", "valide", "waaw", "waw",
        )
        val EXPRESSION_OUI = Regex("""c.?est (ça|ca|bon|cela)""")
        val MOTS_NON = setOf("non", "pas", "annule", "annuler", "arrête", "stop", "déet", "deedeet")

        /** "à Fatou", "a fatou", "au 77 123 45 67" */
        val DESTINATAIRE = Regex(
            """(?:^|\s)(?:à|a|au)\s+(\+?\d[\d ]{7,}\d|\p{L}[\p{L}'-]*)""",
            RegexOption.IGNORE_CASE,
        )

        /** "5000", "5 000", "5.000", "5 mille" */
        val MONTANT = Regex("""(\d+(?:[   .]\d{3})*)(\s*mille)?""", RegexOption.IGNORE_CASE)
    }
}
