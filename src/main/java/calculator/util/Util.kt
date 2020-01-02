package calculator.util

import calculator.Card
import calculator.Hand
import calculator.Shoe
import calculator.classic.BlackJackClassicGame
import java.math.BigDecimal
import java.math.RoundingMode


fun getProbAce(shoe: Shoe): BigDecimal {
    val stateProbs = shoe.getNextStatesAndProbabilities()
    val firstCardAndProb = stateProbs[0]
    if (firstCardAndProb.first == Card.ACE) {
        return firstCardAndProb.second
    }
    return BigDecimal(0)
}

fun getProbTen(shoe: Shoe): BigDecimal {
    val stateProbs = shoe.getNextStatesAndProbabilities()
    val lastCardAndProb = stateProbs[stateProbs.size - 1]
    if (lastCardAndProb.first == Card.TEN) {
        return lastCardAndProb.second
    }
    return BigDecimal(0)
}

fun getScaledProbs(playerHand: Hand, split: Hand?, dealerHand: Hand/*, insurance: Boolean, shoe: Shoe*/): Map<Int, BigDecimal> {
    val combinedHands = if (split == null) playerHand else Hand.combineHands(playerHand, split)
    val dealerScoresAndProbabilities = BlackJackClassicGame.db.getProbabilities(combinedHands, dealerHand)
//    if (insurance) {
//        // if hand is insured, it means dealer does not have blackjack
//        // remove probability that next card is an ace
//        val aceProb = getProbAce(shoe)
//        val prob21 = dealerScoresAndProbabilities[21]
//        if (prob21 != null && (aceProb.toDouble() != 0.0)) {
//            val mutableScoresAndProbabilities = dealerScoresAndProbabilities.toMutableMap()
//            val newProb21 = prob21 - aceProb
//            mutableScoresAndProbabilities[21] = newProb21
//            return rescaleProbs(mutableScoresAndProbabilities, BigDecimal(1) - aceProb)
//        }
//    }
    return dealerScoresAndProbabilities
}


fun rescaleProbs(map: Map<Int, BigDecimal>, newScale: BigDecimal): Map<Int, BigDecimal> {
    val newMap = map.toMutableMap()
    return newMap.map {
        it.key to it.value.divide(newScale, 32, RoundingMode.HALF_UP)
    }.toMap()
}