package sn.jappale.actions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import sn.jappale.Config
import sn.jappale.accessibility.AgentAccessibilityService
import sn.jappale.accessibility.Step
import sn.jappale.accessibility.UiScript
import sn.jappale.intent.Intention
import sn.jappale.intent.Service

/**
 * Exécute une intention déjà confirmée par l'utilisateur.
 * Choisit le moyen (code USSD ou script d'accessibilité) et dit ce qui se passe.
 */
class ActionRunner(
    private val activity: Activity,
    private val say: (String) -> Unit,
) {

    fun run(intention: Intention) {
        when (intention) {
            is Intention.AcheterCredit -> acheterCredit(intention)
            is Intention.EnvoyerArgent -> when (intention.service) {
                Service.WAVE -> envoyerAvecWave(intention)
                Service.ORANGE_MONEY -> envoyerAvecOrangeMoney(intention)
            }
            is Intention.Inconnue -> say("Je n'ai pas compris.")
        }
    }

    // TODO étape 2 : composer directement le code d'achat avec le montant,
    // une fois les codes vérifiés par opérateur. Pour l'instant on ouvre le menu.
    private fun acheterCredit(intention: Intention.AcheterCredit) {
        say("J'ouvre le menu. Choisis achat de crédit, ${intention.montant} francs.")
        composerUssd(Config.USSD_MENU)
    }

    private fun envoyerAvecOrangeMoney(intention: Intention.EnvoyerArgent) {
        say(
            "J'ouvre Orange Money. Choisis transfert, ${intention.montant} francs " +
                "pour ${intention.destinataire}.",
        )
        composerUssd(Config.USSD_MENU)
    }

    private fun envoyerAvecWave(intention: Intention.EnvoyerArgent) {
        val lancement = activity.packageManager.getLaunchIntentForPackage(Config.WAVE_PACKAGE)
        if (lancement == null) {
            say("Wave n'est pas installé sur ce téléphone.")
            return
        }

        val service = AgentAccessibilityService.instance
        if (service == null) {
            say("Pour que je prépare l'envoi, active Jàppale dans Accessibilité.")
            activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }

        // TODO étape 3 : retrouver le numéro à partir du prénom dans les contacts.
        val script = UiScript(
            targetPackage = Config.WAVE_PACKAGE,
            steps = listOf(
                Step.ClickText(Config.WAVE_BOUTON_ENVOYER),
                Step.TypeText(intention.destinataire),
                Step.TypeText(intention.montant.toString()),
                Step.StopForUser(
                    "Vérifie le nom et le montant. Si c'est bon, valide et tape ton code secret.",
                ),
            ),
        )
        service.start(script, onEnd = say)
        activity.startActivity(lancement)
    }

    private fun composerUssd(code: String) {
        val adresse = Uri.parse("tel:" + Uri.encode(code))
        val peutAppeler = activity.checkSelfPermission(Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        // Sans la permission, on ouvre le clavier avec le code déjà écrit.
        val action = if (peutAppeler) Intent.ACTION_CALL else Intent.ACTION_DIAL
        activity.startActivity(Intent(action, adresse))
    }
}
