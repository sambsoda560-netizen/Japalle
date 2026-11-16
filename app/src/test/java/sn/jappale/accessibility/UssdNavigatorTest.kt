package sn.jappale.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UssdNavigatorTest {

    private val plan = UssdPlan(
        etapes = listOf(listOf("crédit", "credit"), listOf("moi", "mon numéro")),
        montant = 500,
    )

    @Test
    fun choisitLaLigneCreditDansLeMenu() {
        val menu = "Orange Money\n1. Transfert d'argent\n2. Paiement\n3. Achat crédit\n4. Mon compte"
        assertEquals(UssdDecision.Repondre("3", 1), UssdNavigator.decider(menu, plan, 0))
    }

    @Test
    fun choisitPourMoiAuDeuxiemeEcran() {
        val menu = "Achat crédit\n1 Pour moi-même\n2 Pour un autre numéro"
        assertEquals(UssdDecision.Repondre("1", 2), UssdNavigator.decider(menu, plan, 1))
    }

    @Test
    fun saisitLeMontant() {
        assertEquals(
            UssdDecision.Repondre("500", 2),
            UssdNavigator.decider("Entrez le montant (100 à 100000 F)", plan, 2),
        )
    }

    @Test
    fun sArreteAuCodeSecret() {
        val ecran = "Achat de 500 F de crédit.\nEntrez votre code secret pour valider"
        assertEquals(UssdDecision.CodeSecret, UssdNavigator.decider(ecran, plan, 2))
    }

    @Test
    fun uneLigneCodeSecretDansUnMenuNEstPasUneDemandeDeCode() {
        val menu = "1. Achat crédit\n2. Changer code secret"
        assertEquals(UssdDecision.Repondre("1", 1), UssdNavigator.decider(menu, plan, 0))
    }

    @Test
    fun confirmeSeulementSiLeMontantEstLeBon() {
        val bon = "Achat de 500 F de crédit\n1. Confirmer\n2. Annuler"
        val mauvais = "Achat de 5000 F de crédit\n1. Confirmer\n2. Annuler"
        assertEquals(UssdDecision.Repondre("1", 2), UssdNavigator.decider(bon, plan, 2))
        assertTrue(UssdNavigator.decider(mauvais, plan, 2) is UssdDecision.Abandon)
    }

    @Test
    fun abandonneSurUnMenuInconnu() {
        val menu = "1. Forfaits internet\n2. Jeux\n3. Musique"
        assertTrue(UssdNavigator.decider(menu, plan, 0) is UssdDecision.Abandon)
    }

    @Test
    fun abandonneSiLeMenuDemandeUnNumeroInconnu() {
        assertTrue(UssdNavigator.decider("Entrez le numéro du bénéficiaire", plan, 1) is UssdDecision.Abandon)
    }
}
