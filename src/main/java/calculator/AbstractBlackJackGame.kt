package calculator

import java.math.BigDecimal

abstract class AbstractBlackJackGame {
    abstract fun dealerShouldHit(dealerHand: Hand): Boolean
    abstract fun canDouble(playerHand: Hand, double: Boolean, splitAces: Boolean): Boolean
    abstract fun scoreHand(playerHand: Hand, splitHand: Hand?, dealerHand: Hand, dealerStartingCard: Card, shoe: Shoe, blackJack: Boolean, double: Boolean, insurance: Boolean, splitAces: Boolean): BigDecimal
    abstract fun getExpectedUtility(playerHand: Hand, split: Hand?, dealerHand: Hand, dealerStartingCard: Card, blackJack: Boolean, insurance: Boolean, shoe: Shoe): Double

    fun canHit(playerHand: Hand, double: Boolean, splitAces: Boolean): Boolean {
        return (playerHand.size == 1
                || (!double && !splitAces && playerHand.getPreferredValue() < 21))
    }

    fun canSplit(playerHand: Hand, split: Hand?): Boolean {
        return playerHand.size == 2 && split == null && playerHand[0] == playerHand[1]
    }

    fun canInsure(
            playerHand: Hand,
            dealerHand: Hand,
            split: Hand?,
            insurance: Boolean
    ): Boolean {
        // limit insurance to first turn
        return !insurance && playerHand.size == 2 && split == null && dealerHand.isSoft()
    }
}