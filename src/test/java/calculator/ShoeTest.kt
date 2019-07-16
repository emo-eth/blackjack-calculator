package calculator

import calculator.Card
import calculator.fromCounts
import calculator.getNextStatesAndProbabilities
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek

object ShoeTest : Spek({
    group("calculator.ShoeTest") {
        test("shoe filters no card") {
            val shoe = fromCounts(Pair(Card.ACE, 1), Pair(Card.TWO, 1))
            val nextStates = getNextStatesAndProbabilities(shoe)
            assertThat(nextStates.size).isEqualTo(2)
        }
    }
})