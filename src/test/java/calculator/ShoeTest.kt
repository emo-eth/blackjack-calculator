package calculator

import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek

object ShoeTest : Spek({
    group("calculator.ShoeTest") {
        test("shoe filters no card") {
            val shoe = Shoe.fromCounts(Pair(Card.ACE, 1), Pair(Card.TWO, 1))
            val nextStates = shoe.getNextStatesAndProbabilities()
            assertThat(nextStates.size).isEqualTo(2)
        }
        test("getNextStatesAndProbabilities") {
            throw NotImplementedError()
        }
    }
})