package calculator.util

import calculator.Hand
import calculator.Shoe
import calculator.dealer.DealerProbabilitiesModel
import java.math.BigDecimal
import java.math.RoundingMode


fun getScaledProbs(playerHand: Hand, split: Hand?, dealerHand: Hand, insurance: Boolean, shoe: Shoe): Map<Int, BigDecimal> {
    val combinedHands = if (split == null) playerHand else Hand.combineHands(playerHand, split)
    val dealerScoresAndProbabilities = DealerProbabilitiesModel.getProbabilities(combinedHands, dealerHand)
    if (insurance) {
        // if hand is insured, it means dealer does not have blackjack
        // remove probability that next card is an ace
        val aceProb = shoe.getProbAce()
        val prob21 = dealerScoresAndProbabilities[21]
        if (prob21 != null && (aceProb.toDouble() != 0.0)) {
            val mutableScoresAndProbabilities = dealerScoresAndProbabilities.toMutableMap()
            val newProb21 = prob21 - aceProb
            mutableScoresAndProbabilities[21] = newProb21
            return rescaleProbs(mutableScoresAndProbabilities, BigDecimal(1) - aceProb)
        }
    }
    return dealerScoresAndProbabilities
}


fun rescaleProbs(map: Map<Int, BigDecimal>, newScale: BigDecimal): Map<Int, BigDecimal> {
    val newMap = map.toMutableMap()
    return newMap.map {
        it.key to it.value.divide(newScale, 32, RoundingMode.HALF_UP)
    }.toMap()
}