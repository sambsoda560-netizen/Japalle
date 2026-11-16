package sn.jappale.accessibility

/**
 * Une suite d'étapes fixes à jouer dans une autre appli.
 * Les scripts sont écrits à la main : aucune IA ne décide des étapes.
 * Un script doit toujours finir par StopForUser.
 */
data class UiScript(
    val targetPackage: String,
    val steps: List<Step>,
)

sealed class Step {
    /** Appuyer sur l'élément qui porte ce texte. */
    data class ClickText(val label: String) : Step()

    /** Écrire dans le champ actif, ou dans le premier champ vide. */
    data class TypeText(val value: String) : Step()

    /** Fin du script : on rend la main à l'utilisateur et on lui dit quoi faire. */
    data class StopForUser(val message: String) : Step()
}
