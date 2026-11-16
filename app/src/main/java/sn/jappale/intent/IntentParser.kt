package sn.jappale.intent

/**
 * Transforme ce que l'utilisateur a dit en intention.
 * - RuleBasedParser : règles simples, sans internet.
 * - LlmParser : modèle de langage par API, avec les règles en secours.
 */
interface IntentParser {
    /**
     * @param echanges la demande, suivie des précisions données ensuite par l'utilisateur
     * @param onResult appelé sur le fil principal
     */
    fun parse(echanges: List<String>, onResult: (Intention) -> Unit)

    /** true = oui, false = non, null = pas compris. */
    fun parseConfirmation(text: String): Boolean?
}
