package calculator.classic

import calculator.*
import calculator.dealer.DealerProbabilitiesModel
import calculator.util.getScaledProbs
import java.math.BigDecimal

object ClassicBlackJackGame : AbstractBlackJackDealerHitOnSoft17Game() {
    override val doubleRange = IntRange(9, 11)

    override fun getStartingShoe(): Shoe {
        return Shoe(8)
    }

}