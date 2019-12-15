package calculator

import junit.framework.Assert.assertTrue
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek

object HandTest : Spek({
    group("calculator.HandTest") {
        test("get values work") {
            var hand = Hand.fromCards(Card.TEN, Card.TEN)
            assertThat(hand.getPreferredValue()).isEqualTo(20)
            hand = Hand.fromCards(Card.FIVE, Card.ACE)
            assertThat(hand.getSoftValue()).isEqualTo(16)
            assertThat(hand.getHardValue()).isEqualTo(6)
            assertThat(hand.getPreferredValue()).isEqualTo(16)
            hand = Hand.fromCards(Card.ACE, Card.ACE)
            assertThat(hand.getPreferredValue()).isEqualTo(12)
        }

        test("calculator.isSoft") {
            val hand = Hand.fromCards(Card.FIVE, Card.ACE)
            assertThat(hand.isSoft()).isTrue()
        }

        test("add card sorts") {
            var hand = Hand.fromCards(Card.FIVE, Card.ACE)
            assertTrue(hand.isSoft())
            hand = hand.addCard(Card.TWO)
            assertThat(hand[1]).isEqualTo(Card.TWO.num)
        }
    }
})