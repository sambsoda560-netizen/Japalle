package sn.agentvocal

/**
 * Toutes les valeurs qui dépendent du terrain (opérateurs, applis tierces).
 * Elles sont des hypothèses de départ : voir docs/A_VERIFIER.md.
 */
object Config {
    /** Nom de paquet de l'appli Wave. À VÉRIFIER. */
    const val WAVE_PACKAGE = "com.wave.personal"

    /** Texte du bouton d'envoi sur l'accueil de Wave. À CALIBRER sur un vrai téléphone. */
    const val WAVE_BOUTON_ENVOYER = "Envoyer"

    /** Menu USSD ouvert pour le crédit et Orange Money. À VÉRIFIER par opérateur. */
    const val USSD_MENU = "#144#"

    /** Temps laissé à une étape de script pour trouver son bouton, en millisecondes. */
    const val SCRIPT_TIMEOUT_MS = 8_000L

    /**
     * Mettre à true pendant la calibration pour écrire dans Logcat (tag "AgentVocal")
     * ce que le service voit à l'écran. Laisser à false sinon : l'écran de Wave
     * contient des informations privées (solde, contacts).
     */
    const val DEBUG_ECRAN = false
}
