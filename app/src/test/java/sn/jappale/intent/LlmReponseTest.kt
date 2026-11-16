package sn.jappale.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmReponseTest {

    @Test
    fun achatDeCreditComplet() {
        val json = """{"action":"acheter_credit","montant":500,"destinataire":null,"service":null,"question":null}"""
        assertEquals(Intention.AcheterCredit(500), LlmReponse.versIntention(json))
    }

    @Test
    fun envoiCompletMemeEntoureDeTexte() {
        val reponse = "Voici :\n```json\n" +
            """{"action":"envoyer_argent","montant":5000,"destinataire":"Fatou","service":"orange_money","question":null}""" +
            "\n```"
        assertEquals(
            Intention.EnvoyerArgent(5000, "Fatou", Service.ORANGE_MONEY),
            LlmReponse.versIntention(reponse),
        )
    }

    @Test
    fun serviceParDefautWave() {
        val json = """{"action":"envoyer_argent","montant":2000,"destinataire":"Moussa","service":null,"question":null}"""
        assertEquals(Intention.EnvoyerArgent(2000, "Moussa", Service.WAVE), LlmReponse.versIntention(json))
    }

    @Test
    fun informationManquanteDonneUneQuestion() {
        val json = """{"action":"envoyer_argent","montant":null,"destinataire":"Fatou","service":"wave","question":"Combien veux-tu envoyer à Fatou ?"}"""
        assertEquals(
            Intention.Precision("Combien veux-tu envoyer à Fatou ?"),
            LlmReponse.versIntention(json),
        )
        val sansQuestion = """{"action":"acheter_credit","montant":null}"""
        assertTrue(LlmReponse.versIntention(sansQuestion) is Intention.Precision)
    }

    @Test
    fun montantAbsurdeRefuse() {
        val json = """{"action":"acheter_credit","montant":-5,"question":null}"""
        assertTrue(LlmReponse.versIntention(json) is Intention.Precision)
    }

    @Test
    fun actionInconnue() {
        val json = """{"action":"inconnue","montant":null,"destinataire":null,"service":null,"question":null}"""
        assertEquals(Intention.Inconnue, LlmReponse.versIntention(json))
    }

    @Test(expected = Exception::class)
    fun reponseSansJsonLeveUneErreur() {
        LlmReponse.versIntention("Désolé, je ne peux pas répondre.")
    }
}
