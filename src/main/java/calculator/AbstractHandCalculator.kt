package calculator

import java.math.BigDecimal

abstract class AbstractHandCalculator(val game: AbstractBlackJackGame) {

    abstract fun evaluateAction(
            action: Action,
            playerHand: Hand,
            dealerHand: Hand,
            shoe: Shoe,
            double: Boolean,
            split: Hand?,
            splitAces: Boolean,
            insurance: Boolean
    ): BigDecimal
}