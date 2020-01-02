package calculator

import calculator.Card
import calculator.Hand
import calculator.Shoe
import java.math.BigDecimal

abstract class AbstractBlackJackGame {
    abstract fun dealerShouldHit(dealerHand: Hand): Boolean
    abstract fun canDouble(playerHand: Hand, double: Boolean, splitAces: Boolean): Boolean
    // TODO: consider taking a hand here anyway
    abstract fun scoreHand(playerHand: Hand, splitHand: Hand?, dealerHand: Hand, dealerStartingCard: Card, shoe: Shoe, blackJack: Boolean, double: Boolean, insurance: Boolean, splitAces: Boolean): BigDecimal

    fun canHit(playerHand: Hand, double: Boolean, splitAces: Boolean): Boolean {
        return (playerHand.size == 1
                || (!double && !splitAces && playerHand.getPreferredValue() < 21))
    }

    fun canSplit(playerHand: Hand, split: Hand?): Boolean {
        if (split != null || playerHand.size != 2) return false
        return playerHand[0] == playerHand[1]
    }

    // todo: can just check if dealer is soft and no split
    // TODO: if insured we know dealer doesn't have 10, calculate probs accordingly??????
    fun canInsure(
            playerHand: Hand,
            dealerHand: Hand,
            split: Hand?,
            insurance: Boolean
    ): Boolean {
        // limit insurance to first turn
        return !insurance && split == null && dealerHand.isSoft() && playerHand.size == 2
    }
}