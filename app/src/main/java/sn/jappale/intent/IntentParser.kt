package sn.jappale.intent

/**
 * Transforme une phrase en intention.
 * Version actuelle : RuleBasedParser (français, règles simples).
 * Plus tard : un LLM qui renvoie la même structure, en français et en wolof.
 */
interface IntentParser {
    fun parse(text: String): Intention

    /** true = oui, false = non, null = pas compris. */
    fun parseConfirmation(text: String): Boolean?
}
