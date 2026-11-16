package sn.jappale

/**
 * Toutes les valeurs qui dépendent du terrain (opérateurs, applis tierces).
 * Elles sont des hypothèses de départ : voir docs/A_VERIFIER.md.
 */
object Config {
    /** Nom de paquet de l'appli Wave. À VÉRIFIER. */
    const val WAVE_PACKAGE = "com.wave.personal"

    // Textes de Wave, lus dans le relevé d'écrans du 16 novembre 2026.
    // À revoir si Wave change son interface.
    val WAVE_ACCUEIL_CREDIT = listOf("Crédit")
    val WAVE_ACCUEIL_TRANSFERT = listOf("Transfert")
    val WAVE_MON_NUMERO = listOf("Mon Numéro")
    val WAVE_BOUTON_ACHETER = listOf("Acheter")
    const val WAVE_CHAMP_MONTANT = "Montant"
    const val WAVE_CHAMP_DESTINATAIRE = "À"

    /**
     * true : pour le crédit sur son propre numéro, l'agent appuie lui-même sur « Acheter »
     * après la confirmation à voix haute et la vérification du montant affiché.
     * false : il s'arrête juste avant et laisse l'utilisateur appuyer.
     */
    const val WAVE_VALIDER_ACHAT_CREDIT = true

    /**
     * Les menus USSD (#144#) sont désactivés pour l'instant : tout passe par les applis.
     * Le code du navigateur USSD est gardé pour plus tard (téléphones sans internet).
     */
    const val USSD_ACTIF = false

    /** Menu USSD ouvert pour le crédit et Orange Money. */
    const val USSD_MENU = "#144#"

    /** Applis qui affichent les fenêtres USSD. Peut changer selon la marque du téléphone. */
    val USSD_PACKAGES = setOf("com.android.phone")

    /** En temps normal, le service n'écoute que ces applis. */
    val PAQUETS_AUTORISES: Set<String>
        get() = USSD_PACKAGES + WAVE_PACKAGE

    /** Mode réglage : nombre maximal d'écrans relevés. */
    const val RELEVE_ECRANS_MAX = 40

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

    /** Temps laissé à l'appli pour s'ouvrir et à l'utilisateur pour taper son code d'entrée. */
    const val SCRIPT_OUVERTURE_MS = 90_000L

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
