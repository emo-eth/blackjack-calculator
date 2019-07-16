package calculator

import calculator.*
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek

object HandTest : Spek({
    group("calculator.HandTest") {
        test("get values work") {
            var hand = fromCards(Card.TEN, Card.TEN)
            assertThat(getPreferredValue(hand)).isEqualTo(20)
            hand = fromCards(Card.FIVE, Card.ACE)
            assertThat(getSoftValue(hand)).isEqualTo(16)
            assertThat(getHardValue(hand)).isEqualTo(6)
            assertThat(getPreferredValue(hand)).isEqualTo(16)
            hand = fromCards(Card.ACE, Card.ACE)
            assertThat(getPreferredValue(hand)).isEqualTo(12)
        }

        test("calculator.isSoft") {
            val hand = fromCards(Card.FIVE, Card.ACE)
            assertThat(isSoft(hand)).isTrue()
        }

        test("add card sorts") {
            var hand = fromCards(Card.FIVE, Card.ACE)
            assertThat(isSoft(hand)).isTrue()
            hand = addCard(Card.TWO, hand)
            assertThat(hand[1]).isEqualTo(Card.TWO.num)
        }
    }
})