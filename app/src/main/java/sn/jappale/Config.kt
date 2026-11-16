package sn.jappale

/**
 * Toutes les valeurs qui dépendent du terrain (opérateurs, applis tierces).
 * Elles sont des hypothèses de départ : voir docs/A_VERIFIER.md.
 */
object Config {
    /** Nom de paquet de l'appli Wave. À VÉRIFIER. */
    const val WAVE_PACKAGE = "com.wave.personal"

    /** Texte du bouton d'envoi sur l'accueil de Wave. À CALIBRER sur un vrai téléphone. */
    const val WAVE_BOUTON_ENVOYER = "Envoyer"

    /** Menu USSD ouvert pour le crédit et Orange Money. */
    const val USSD_MENU = "#144#"

    /** Applis qui affichent les fenêtres USSD. Peut changer selon la marque du téléphone. */
    val USSD_PACKAGES = setOf("com.android.phone")

    /** Boutons possibles pour envoyer une réponse dans une fenêtre USSD. */
    val USSD_BOUTONS_ENVOI = listOf("Envoyer", "Send", "Répondre", "Reply", "OK")

    /**
     * Achat de crédit : mots cherchés dans les lignes du menu, écran après écran.
     * À CALIBRER avec le vrai menu #144#.
     */
    val USSD_ETAPES_CREDIT = listOf(
        listOf("crédit", "credit", "recharge"),
        listOf("moi", "mon numéro", "mon numero", "mon compte"),
    )

    /** Transfert Orange Money : mots cherchés dans le menu. À CALIBRER. */
    val USSD_ETAPES_TRANSFERT = listOf(
        listOf("transfert", "envoi", "envoyer"),
    )

    /** Temps laissé à une étape de script pour trouver son bouton, en millisecondes. */
    const val SCRIPT_TIMEOUT_MS = 8_000L

    /** Temps laissé au réseau pour afficher l'écran USSD suivant. */
    const val USSD_TIMEOUT_MS = 30_000L

    /** Nombre maximal de réponses qu'un plan USSD peut envoyer. */
    const val USSD_REPONSES_MAX = 8

    /**
     * Mettre à true pendant la calibration pour écrire dans Logcat (tag "Jappale")
     * ce que le service voit à l'écran. Laisser à false sinon : ces écrans
     * contiennent des informations privées (solde, contacts).
     */
    const val DEBUG_ECRAN = false
}
