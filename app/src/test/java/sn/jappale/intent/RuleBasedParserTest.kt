package sn.jappale.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleBasedParserTest {

    private val parser = RuleBasedParser()

    @Test
    fun achatDeCredit() {
        assertEquals(Intention.AcheterCredit(1000), parser.comprendre("Achète-moi 1000 francs de crédit"))
        assertEquals(Intention.AcheterCredit(500), parser.comprendre("je veux du credit 500"))
        assertEquals(Intention.AcheterCredit(5000), parser.comprendre("achète-moi du crédit 5000"))
    }

    @Test
    fun envoiAvecPrenom() {
        assertEquals(
            Intention.EnvoyerArgent(5000, "Fatou", Service.WAVE),
            parser.comprendre("envoie 5000 francs à Fatou"),
        )
        assertEquals(
            Intention.EnvoyerArgent(5000, "Fatou", Service.WAVE),
            parser.comprendre("Envoie 5 000 à fatou avec Wave"),
        )
    }

    @Test
    fun envoiAvecMilleEtOrangeMoney() {
        assertEquals(
            Intention.EnvoyerArgent(10000, "Moussa", Service.ORANGE_MONEY),
            parser.comprendre("transfère 10 mille à Moussa par Orange Money"),
        )
    }

    @Test
    fun envoiAvecNumero() {
        assertEquals(
            Intention.EnvoyerArgent(2000, "771234567", Service.WAVE),
            parser.comprendre("envoie 2000 au 77 123 45 67"),
        )
    }

    @Test
    fun demandeIncompleteOuHorsSujet() {
        assertEquals(Intention.Inconnue, parser.comprendre("envoie de l'argent à Fatou"))
        assertEquals(Intention.Inconnue, parser.comprendre("envoie 5000"))
        assertEquals(Intention.Inconnue, parser.comprendre("quel temps fait-il"))
    }

    @Test
    fun confirmation() {
        assertEquals(true, parser.parseConfirmation("oui c'est ça"))
        assertEquals(true, parser.parseConfirmation("Waaw"))
        assertEquals(true, parser.parseConfirmation("c'est ça"))
        assertEquals(true, parser.parseConfirmation("voilà exactement"))
        assertEquals(false, parser.parseConfirmation("non"))
        assertEquals(false, parser.parseConfirmation("c'est pas ça"))
        assertNull(parser.parseConfirmation("euh"))
    }
}
