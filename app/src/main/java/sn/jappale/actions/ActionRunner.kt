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
import sn.jappale.accessibility.UssdPlan
import sn.jappale.intent.Intention
import sn.jappale.intent.Service

/**
 * Exécute une intention déjà confirmée par l'utilisateur.
 * Choisit le moyen (menu USSD ou appli) et dit ce qui se passe.
 *
 * @param say  dit un message ; le second paramètre est un détail à afficher
 *             (par exemple le menu que l'agent n'a pas su lire).
 */
class ActionRunner(
    private val activity: Activity,
    private val say: (String, String?) -> Unit,
) {

    fun run(intention: Intention) {
        when (intention) {
            is Intention.AcheterCredit -> acheterCredit(intention)
            is Intention.EnvoyerArgent -> when (intention.service) {
                Service.WAVE -> envoyerAvecWave(intention)
                Service.ORANGE_MONEY -> envoyerAvecOrangeMoney(intention)
            }
            is Intention.Precision, is Intention.Inconnue -> dire("Je n'ai pas compris.")
        }
    }

    private fun acheterCredit(intention: Intention.AcheterCredit) {
        if (Config.USSD_ACTIF) {
            val service = serviceOuDemande() ?: return
            service.startUssd(
                UssdPlan(etapes = Config.USSD_ETAPES_CREDIT, montant = intention.montant),
                onEnd = say,
            )
            dire("D'accord, je remplis le menu pour toi.")
            composerUssd(Config.USSD_MENU)
            return
        }
        val montant = intention.montant.toString()
        val etapes = mutableListOf(
            Step.ClickText(Config.WAVE_ACCUEIL_CREDIT),
            Step.ClickText(Config.WAVE_MON_NUMERO),
            Step.TypeText(montant, hint = Config.WAVE_CHAMP_MONTANT),
            // On ne va pas plus loin tant que l'écran n'affiche pas exactement ce montant.
            Step.VerifyField(montant),
        )
        if (Config.WAVE_VALIDER_ACHAT_CREDIT) {
            etapes += Step.ClickText(Config.WAVE_BOUTON_ACHETER)
            etapes += Step.StopForUser(
                "J'ai lancé l'achat de $montant francs de crédit sur ton numéro. Regarde l'écran de Wave.",
            )
        } else {
            etapes += Step.StopForUser(
                "J'ai mis $montant francs de crédit pour ton numéro. Appuie sur Acheter pour valider.",
            )
        }
        jouerDansWave(*etapes.toTypedArray())
    }

    private fun envoyerAvecWave(intention: Intention.EnvoyerArgent) {
        // TODO après le relevé de l'écran du montant : choisir la personne et saisir le montant.
        jouerDansWave(
            Step.ClickText(Config.WAVE_ACCUEIL_TRANSFERT),
            Step.TypeText(intention.destinataire, hint = Config.WAVE_CHAMP_DESTINATAIRE),
            Step.StopForUser(
                "Choisis ${intention.destinataire} dans la liste, mets ${intention.montant} francs, " +
                    "vérifie, puis valide.",
            ),
        )
    }

    private fun envoyerAvecOrangeMoney(intention: Intention.EnvoyerArgent) {
        if (!Config.USSD_ACTIF) {
            dire(
                "Je ne sais pas encore utiliser l'appli Orange Money. " +
                    "Fais le mode réglage dans cette appli pour que j'apprenne.",
            )
            return
        }
        val estUnNumero = intention.destinataire.all { it.isDigit() || it == '+' }
        if (!estUnNumero) {
            dire("Pour Orange Money, dis-moi le numéro de téléphone de la personne.")
            return
        }
        val service = serviceOuDemande() ?: return
        service.startUssd(
            UssdPlan(
                etapes = Config.USSD_ETAPES_TRANSFERT,
                montant = intention.montant,
                numero = intention.destinataire,
            ),
            onEnd = say,
        )
        dire("D'accord, je remplis le menu pour toi.")
        composerUssd(Config.USSD_MENU)
    }

    /** Ouvre Wave et joue ces étapes dedans. */
    private fun jouerDansWave(vararg etapes: Step) {
        val lancement = activity.packageManager.getLaunchIntentForPackage(Config.WAVE_PACKAGE)
        if (lancement == null) {
            dire("Wave n'est pas installé sur ce téléphone.")
            return
        }
        val service = serviceOuDemande() ?: return
        service.start(UiScript(Config.WAVE_PACKAGE, etapes.toList()), onEnd = say)
        dire("J'ouvre Wave. Si Wave demande ton code, tape-le, je continue après.")
        activity.startActivity(lancement)
    }

    /** Le service d'accessibilité, ou null après avoir expliqué comment l'activer. */
    private fun serviceOuDemande(): AgentAccessibilityService? {
        val service = AgentAccessibilityService.instance
        if (service == null) {
            dire("Pour que j'agisse à ta place, active Jàppale dans Accessibilité, puis recommence.")
            activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        return service
    }

    private fun composerUssd(code: String) {
        val adresse = Uri.parse("tel:" + Uri.encode(code))
        val peutAppeler = activity.checkSelfPermission(Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        // Sans la permission, on ouvre le clavier avec le code déjà écrit.
        val action = if (peutAppeler) Intent.ACTION_CALL else Intent.ACTION_DIAL
        activity.startActivity(Intent(action, adresse))
    }

    private fun dire(message: String) = say(message, null)
}
