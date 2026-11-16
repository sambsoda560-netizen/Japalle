package sn.jappale.accessibility

import sn.jappale.Config

/**
 * Une suite d'étapes fixes à jouer dans une autre appli.
 * Les scripts sont écrits à la main : aucune IA ne décide des étapes.
 * Un script doit toujours finir par StopForUser.
 *
 * @param premiereAttenteMs temps laissé à la première étape : l'appli doit s'ouvrir
 *                          et l'utilisateur doit souvent taper son code d'entrée.
 */
data class UiScript(
    val targetPackage: String,
    val steps: List<Step>,
    val premiereAttenteMs: Long = Config.SCRIPT_OUVERTURE_MS,
)

sealed class Step {
    /** Appuyer sur le premier élément qui porte l'un de ces textes. */
    data class ClickText(val labels: List<String>) : Step()

    /** Écrire dans le champ actif, ou dans le premier champ vide. */
    data class TypeText(val value: String) : Step()

    /** Fin du script : on rend la main à l'utilisateur et on lui dit quoi faire. */
    data class StopForUser(val message: String) : Step()
}
