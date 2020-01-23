package calculator

import calculator.classic.ClassicBlackJackGame
import calculator.dealer.DealerProbabilitiesModel
import calculator.dealer.calculateAndInsertDealerProbs
import java.math.BigDecimal

abstract class AbstractBlackJackGame {
    abstract val doubleRange: IntRange

    abstract fun getStartingShoe(): Shoe
    abstract fun dealerShouldHit(dealerHand: Hand): Boolean

    fun canDouble(
            playerHand: Hand,
            double: Boolean,
            splitAces: Boolean
    ): Boolean {
        return !double && !splitAces && playerHand.size == 2 && (ClassicBlackJackGame.doubleRange.contains(playerHand.getPreferredValue()) || ClassicBlackJackGame.doubleRange.contains(playerHand.getHardValue()))
    }

    fun canHit(playerHand: Hand, double: Boolean, splitAces: Boolean): Boolean {
        return (playerHand.size == 1
                || (!double && !splitAces && playerHand.getPreferredValue() < 21))
    }

    fun canSplit(playerHand: Hand, split: Hand?, numSplits: Int): Boolean {
        return playerHand.size == 2 && numSplits < 3 && playerHand[0] == playerHand[1] && playerHand[0] != Card.TEN.num
    }

    fun shouldSplit(playerHand: Hand, split: Hand?): Boolean {
        return playerHand.size == 2 && split == null && playerHand[0] == playerHand[1] && (playerHand[0] == Card.ACE.num || playerHand[0] == Card.EIGHT.num)
    }

    // https://boardgames.stackexchange.com/questions/27181/blackjack-if-youre-insured-and-bust-do-you-collect-on-the-insurance
    fun canInsure(
            playerHand: Hand,
            dealerHand: Hand,
            split: Hand?,
            insurance: Boolean
    ): Boolean {
        // limit insurance to first turn
        return !insurance && playerHand.size == 2 && split == null && dealerHand.isSoft()
    }

    fun getAllPossibleActions(
            playerHand: Hand,
            dealerHand: Hand,
            split: Hand?,
            splitAces: Boolean,
            insurance: Boolean,
            numSplits: Int
    ): List<Action> {
        val actions = mutableListOf(Action.STAND)
        if (splitAces) {
            if (playerHand.size == 1) return listOf(Action.HIT)
            if (playerHand.size == 2) return actions
        }

        // we don't care about double and splitAces here because this state is reachable regardless
        if (canHit(playerHand, false, false)) actions.add(Action.HIT)
        if (canDouble(playerHand, false, false)) actions.add(Action.DOUBLE)
        if (canSplit(playerHand, split, numSplits)) actions.add(Action.SPLIT)
//        if (game.canInsure(playerHand, dealerHand, split, insurance)) actions.add(Action.INSURANCE)

        return actions
    }

    fun canPerform(
            action: Action,
            playerHand: Hand,
            dealerHand: Hand,
            double: Boolean,
            split: Hand?,
            splitAces: Boolean,
            insurance: Boolean,
            numSplits: Int
    ): Boolean {
        return when (action) {
            Action.STAND -> true
            Action.HIT ->
                // playerHand value will always be <21 at this stage
                canHit(playerHand, double, splitAces)
            Action.DOUBLE ->
                canDouble(playerHand, double, splitAces)
            Action.INSURANCE -> // todo: consider always passing false
                false
//                game.canInsure(playerHand, dealerHand, split, insurance)
            Action.SPLIT ->
                canSplit(playerHand, split, numSplits)
            Action.SURRENDER ->
                false
        }
    }

    fun scoreHand(
            playerHand: Hand,
            splitHand: Hand?,
            dealerHand: Hand,
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
        val dealerStartingCard = Card.fromByte(dealerHand[0])
        if (blackJack && !splitAces && (dealerStartingCard != Card.ACE && dealerStartingCard != Card.TEN)) {
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


    fun getExpectedUtility(playerHand: Hand, split: Hand?, dealerHand: Hand, dealerStartingCard: Card, blackJack: Boolean, insurance: Boolean, shoe: Shoe): Double {
        // handle player blackjack exceptions
        if (blackJack) {
            // handle case when dealer gets blackjack on player blackjack
            if (dealerStartingCard == Card.TEN) {
                val aceProb = shoe.getProbAce()
                return 1.5 * (1.0 - aceProb.toDouble())
            } else if (!insurance && dealerStartingCard == Card.ACE) {
                val tenProb = shoe.getProbTen()
                return 1.5 * (1.0 - tenProb.toDouble())
            }
        }

        val dealerScoresAndProbabilities = getDealerProbs(playerHand, split, dealerHand)


        val playerValue = playerHand.getPreferredValue()
        val utilities: List<Double> = dealerScoresAndProbabilities.map { entry ->
            val (dealerValue, prob) = entry
            if (playerValue == dealerValue) {
                // equal
                0.0
            } else if (dealerValue > 21) {
                // dealer bust
                prob.toDouble()
            } else if (playerValue > dealerValue) {
                // player beat dealer
                prob.toDouble()
            } else {
                -prob.toDouble() // dealer beat player
            }
        }
        return utilities.sum()
    }

    fun getDealerProbs(playerHand: Hand, split: Hand?, dealerHand: Hand): Map<Int, BigDecimal> {
        val combinedHands = if (split == null) playerHand else Hand.combineHands(playerHand, split)
        val probs = DealerProbabilitiesModel.getProbabilitiesIfExist(combinedHands, dealerHand)
        if (probs != null) return probs
        return calculateAndInsertDealerProbs(Hand.fromHands(playerHand, split), Card.fromByte(dealerHand[0]), getStartingShoe())
    }

}