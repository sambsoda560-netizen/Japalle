package sn.jappale.intent

import org.json.JSONObject

/**
 * Le contrat entre l'IA et l'appli : l'IA ne fait que remplir cette fiche,
 * elle ne déclenche rien. Tout ce qui sort d'ici repasse par la confirmation vocale.
 */
object LlmReponse {

    val CONSIGNE = """
        Tu es le module de compréhension d'une appli vocale utilisée au Sénégal.
        L'utilisateur parle en français, parfois mêlé de wolof. Sa phrase vient d'une
        reconnaissance vocale et peut contenir des erreurs (par exemple « KD » ou « crédi »
        pour « crédit », « wave » écrit « wève »).

        Actions possibles :
        - acheter_credit : acheter du crédit téléphonique. Il faut le montant.
        - envoyer_argent : envoyer de l'argent. Il faut le montant, le destinataire
          (prénom ou numéro de téléphone) et le service : "wave" ou "orange_money".
          Si le service n'est pas précisé, mets "wave".

        Réponds uniquement avec un objet JSON, sans aucun autre texte :
        {"action": "acheter_credit" | "envoyer_argent" | "inconnue",
         "montant": entier ou null,
         "destinataire": texte ou null,
         "service": "wave" | "orange_money" | null,
         "question": texte ou null}

        Règles :
        - Les montants sont en francs CFA, en chiffres : « cinq mille » donne 5000,
          « deux mille cinq cents » donne 2500.
        - N'invente jamais un montant ni un destinataire. S'il manque une information,
          mets-la à null et écris dans "question" une question courte, en français,
          en tutoyant (« Combien veux-tu envoyer ? »).
        - Si tout est là, "question" vaut null.
        - Si la demande ne correspond à aucune action, "action" vaut "inconnue".
    """.trimIndent()

    /** Transforme la réponse de l'IA en intention. Lève une exception si elle est illisible. */
    fun versIntention(contenu: String): Intention {
        val debut = contenu.indexOf('{')
        val fin = contenu.lastIndexOf('}')
        require(debut in 0 until fin) { "Réponse de l'IA sans JSON" }
        val fiche = JSONObject(contenu.substring(debut, fin + 1))

        val montant = if (fiche.isNull("montant")) 0 else fiche.optInt("montant", 0)
        val destinataire = texte(fiche, "destinataire")
        val question = texte(fiche, "question")
        val montantValide = montant in 1..10_000_000

        return when (fiche.optString("action")) {
            "acheter_credit" ->
                if (montantValide) {
                    Intention.AcheterCredit(montant)
                } else {
                    Intention.Precision(question ?: "Combien de crédit veux-tu ?")
                }

            "envoyer_argent" -> {
                val service =
                    if (fiche.optString("service") == "orange_money") Service.ORANGE_MONEY else Service.WAVE
                when {
                    !montantValide -> Intention.Precision(question ?: "Combien veux-tu envoyer ?")
                    destinataire == null -> Intention.Precision(question ?: "À qui veux-tu envoyer ?")
                    else -> Intention.EnvoyerArgent(montant, destinataire, service)
                }
            }

            else -> Intention.Inconnue
        }
    }

    private fun texte(fiche: JSONObject, cle: String): String? =
        if (fiche.isNull(cle)) null else fiche.optString(cle).trim().takeIf { it.isNotEmpty() }
}
