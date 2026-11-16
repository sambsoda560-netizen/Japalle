package sn.jappale

import android.content.Context

/** Les services d'IA proposés. Tous parlent le même format d'API (compatible OpenAI). */
enum class Fournisseur(val nom: String, val url: String, val modeleParDefaut: String) {
    GROQ(
        nom = "Groq (modèles open source)",
        url = "https://api.groq.com/openai/v1/chat/completions",
        modeleParDefaut = "openai/gpt-oss-120b",
    ),
    XAI(
        nom = "Grok (xAI)",
        url = "https://api.x.ai/v1/chat/completions",
        modeleParDefaut = "grok-4.7",
    ),
}

/**
 * Réglages gardés sur le téléphone uniquement.
 * La clé d'API ne doit jamais être écrite dans le code : le dépôt est public.
 */
class Reglages(context: Context) {

    private val prefs = context.getSharedPreferences("jappale", Context.MODE_PRIVATE)

    val fournisseur: Fournisseur
        get() = Fournisseur.values().firstOrNull { it.name == prefs.getString(FOURNISSEUR, null) }
            ?: Fournisseur.GROQ

    val cle: String
        get() = prefs.getString(CLE, "").orEmpty()

    val modele: String
        get() = prefs.getString(MODELE, null)?.takeIf { it.isNotBlank() }
            ?: fournisseur.modeleParDefaut

    fun enregistrer(fournisseur: Fournisseur, cle: String, modele: String) {
        prefs.edit()
            .putString(FOURNISSEUR, fournisseur.name)
            .putString(CLE, cle)
            .putString(MODELE, modele)
            .apply()
    }

    private companion object {
        const val FOURNISSEUR = "fournisseur"
        const val CLE = "cle"
        const val MODELE = "modele"
    }
}
