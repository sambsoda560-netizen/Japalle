package sn.agentvocal.intent

enum class Service(val label: String) {
    WAVE("Wave"),
    ORANGE_MONEY("Orange Money"),
}

/** Une demande comprise et complète. Ajouter une action = ajouter un cas ici. */
sealed class Intention {

    data class AcheterCredit(val montant: Int) : Intention()

    data class EnvoyerArgent(
        val montant: Int,
        val destinataire: String,
        val service: Service,
    ) : Intention()

    object Inconnue : Intention()

    /** La phrase dite à voix haute avant d'agir. */
    fun question(): String = when (this) {
        is AcheterCredit ->
            "Tu veux acheter $montant francs de crédit, c'est ça ?"
        is EnvoyerArgent ->
            "Tu veux envoyer $montant francs à $destinataire avec ${service.label}, c'est ça ?"
        is Inconnue -> ""
    }
}
