package calculator

import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import java.math.BigDecimal
import java.math.RoundingMode

object ShoeTest : Spek({
    group("calculator.ShoeTest") {
        test("Size matches cards left") {
            val shoe = Shoe.fromCounts(Pair(Card.ACE, 1), Pair(Card.TWO, 1))
            val nextStates = shoe.getNextStatesAndProbabilities()
            assertThat(nextStates.size).isEqualTo(2)
        }
        test("Probabilities are correct") {
            val shoe = Shoe.fromCounts(Pair(Card.ACE, 2), Pair(Card.TWO, 1))
            val nextStates = shoe.getNextStatesAndProbabilities()
            assertThat(nextStates[0].second).isEqualTo(BigDecimal(2).divide(BigDecimal(3), 32, RoundingMode.HALF_UP))
            assertThat(nextStates[1].second).isEqualTo(BigDecimal(1).divide(BigDecimal(3), 32, RoundingMode.HALF_UP))

        }
        test("Removing returns new object") {
            val shoe = Shoe(8)
            val newShoe = shoe.removeCard(Card.ACE)
            assertThat(shoe).isNotEqualTo(newShoe)
        }
        test("fromHands") {
            val shoe = Shoe.fromHands(1, Hand.fromCards(Card.ACE, Card.ACE, Card.ACE, Card.ACE), Hand.fromCard(Card.TWO))
            val states = shoe.getNextStatesAndProbabilities()
            assertThat(states.size).isEqualTo(9)
            assertThat(states[0].second).isEqualTo(BigDecimal(3).divide(BigDecimal(47), 32, RoundingMode.HALF_UP))
        }
    }
})