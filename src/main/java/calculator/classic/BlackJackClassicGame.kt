package calculator.classic

import calculator.Card
import calculator.Hand
import calculator.Shoe
import calculator.dealer.DealerProbabilitiesModel
import calculator.AbstractBlackJackGame
import calculator.util.getProbAce
import calculator.util.getProbTen
import calculator.util.getScaledProbs
import java.math.BigDecimal

object BlackJackClassicGame : AbstractBlackJackGame() {

    val db by lazy {
        val db = DealerProbabilitiesModel()
        db.loadFullCacheMap()
        db
    }
    // https://boardgames.stackexchange.com/questions/27181/blackjack-if-youre-insured-and-bust-do-you-collect-on-the-insurance

    override fun scoreHand(
            playerHand: Hand,
            splitHand: Hand?,
            dealerHand: Hand,
            dealerStartingCard: Card,
            shoe: Shoe,
            blackJack: Boolean,
            double: Boolean,
            insurance: Boolean,
            splitAces: Boolean
    ): BigDecimal {
        // player bust
        if (playerHand.getPreferredValue() > 21) {
            return BigDecimal(-1)
        }
        // unqualified blackjack
        if (blackJack && (dealerStartingCard != Card.ACE && dealerStartingCard != Card.TEN)) {
            return BigDecimal(1.5)
        }

        return BigDecimal(getExpectedUtility(
                playerHand,
                splitHand,
                dealerHand,
                dealerStartingCard,
                blackJack,
                insurance,
                shoe))
    }

    override fun dealerShouldHit(dealerHand: Hand): Boolean {
        return dealerHand.getPreferredValue() <= 17 && dealerHand.getHardValue() != 17
    }

    override fun canDouble(
            playerHand: Hand,
            double: Boolean,
            splitAces: Boolean
    ): Boolean {
        return !double && !splitAces && playerHand.size == 2 && (DOUBLE_RANGE.contains(playerHand.getPreferredValue()) || DOUBLE_RANGE.contains(playerHand.getHardValue()))
    }

    override fun getExpectedUtility(playerHand: Hand, split: Hand?, dealerHand: Hand, dealerStartingCard: Card, blackJack: Boolean, insurance: Boolean, shoe: Shoe): Double {
        // handle player blackjack exceptions
        if (blackJack) {
            // handle case when dealer gets blackjack on player blackjack
            if (dealerStartingCard == Card.TEN) {
                val aceProb = getProbAce(shoe)
                return 1.5 * (1.0 - aceProb.toDouble())
            } else if (!insurance && dealerStartingCard == Card.ACE) {
                val tenProb = getProbTen(shoe)
                return 1.5 * (1.0 - tenProb.toDouble())
            }
        }

        val dealerScoresAndProbabilities = getScaledProbs(playerHand, split, dealerHand)

        val playerValue = playerHand.getPreferredValue()
        val utilities: List<Double> = dealerScoresAndProbabilities.map { entry ->
            val (dealerValue, prob) = entry
            if (playerValue == dealerValue) {
                // equal
                return 0.0
            } else if (dealerValue > 21) {
                // dealer bust
                return prob.toDouble()
            } else if (playerValue > dealerValue) {
                // player beat dealer
                return prob.toDouble()
            }
            return -prob.toDouble() // dealer beat player
        }
        return utilities.sum()
    }

}