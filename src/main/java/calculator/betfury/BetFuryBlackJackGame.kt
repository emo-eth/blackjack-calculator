package calculator.betfury

import calculator.AbstractBlackJackDealerHitOnSoft17Game
import calculator.Shoe

object BetFuryBlackJackGame : AbstractBlackJackDealerHitOnSoft17Game() {
    override val doubleRange = IntRange(10, 11)

    override fun getStartingShoe(): Shoe {
        return Shoe(6)
    }
}