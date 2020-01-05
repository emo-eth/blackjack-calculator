package calculator.dealer

import calculator.Card
import calculator.Shoe
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import java.math.BigDecimal
import java.math.RoundingMode

object DealerCalculatorTest : Spek({
    test("getDealerProbs") {
        var shoe = Shoe.fromCounts(Pair(Card.TEN, 2), Pair(Card.FIVE, 1))
        var dealerResultProbs = getDealerResultProbs(Card.TEN, shoe)
        assertThat(dealerResultProbs.size).isEqualTo(2)
        assertThat(dealerResultProbs[20]?.toDouble()).isEqualTo(BigDecimal(2).divide(BigDecimal(3), 32, RoundingMode.HALF_UP).toDouble())
        assertThat(dealerResultProbs[22]?.toDouble()).isEqualTo(BigDecimal(1).divide(BigDecimal(3), 32, RoundingMode.HALF_UP).toDouble())
        // 10 -> 10. 10 -> 10. 10 -> 5 -> 5. 10 -> 5 -> 10. 10 -> 5 -> 10.

        shoe = Shoe.fromCounts(Pair(Card.TEN, 2), Pair(Card.FIVE, 2))
        dealerResultProbs = getDealerResultProbs(Card.TEN, shoe)
        assertThat(dealerResultProbs.size).isEqualTo(2)
        assertThat(dealerResultProbs[20]?.toDouble()).isEqualTo(BigDecimal(2).divide(BigDecimal(3), 32, RoundingMode.HALF_UP).toDouble())
        assertThat(dealerResultProbs[22]?.toDouble()).isEqualTo(BigDecimal(1).divide(BigDecimal(3), 32, RoundingMode.HALF_UP).toDouble())

        shoe = Shoe.fromCounts(Pair(Card.TEN, 2), Pair(Card.FIVE, 2), Pair(Card.ACE, 1))
        dealerResultProbs = getDealerResultProbs(Card.TEN, shoe)
        assertThat(dealerResultProbs.size).isEqualTo(3)
        assertThat(dealerResultProbs[20]?.toDouble()).isEqualTo(0.5)
        assertThat(dealerResultProbs[21]).isEqualTo(
                BigDecimal(1).divide(BigDecimal(5), 32, RoundingMode.HALF_UP)
                .plus(
                        BigDecimal(2).divide(BigDecimal(5), 32, RoundingMode.HALF_UP).times(
                        BigDecimal(1).divide(BigDecimal(4), 32, RoundingMode.HALF_UP)
                        .times(
                                BigDecimal(1).divide(BigDecimal(3), 32, RoundingMode.HALF_UP)))))


    }

})