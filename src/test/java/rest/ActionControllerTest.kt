package rest

import calculator.Action
import calculator.Card
import calculator.Hand
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek

object ActionControllerTest : Spek({
    group("ActionControllerTest") {
        test("Handler gets blackjack") {
            val controller = ActionController()
            val response = controller.getAction("09", "1", null, false)
            assertThat(response.action).isEqualTo(Action.STAND)
            assertThat(response.utility).isEqualTo(1.5)
        }

        test("fromUtf8") {
            val hand = Hand.fromUtf8("001".toByteArray())
            assertThat(hand).isEqualTo(Hand.fromCards(Card.ACE, Card.ACE, Card.TWO))
        }
    }
})