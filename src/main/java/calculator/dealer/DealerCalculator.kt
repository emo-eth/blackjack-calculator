package calculator.dealer

import calculator.Card
import calculator.Hand
import calculator.Shoe
import java.math.BigDecimal

fun calculateAndInsertDealerProbs(playerHand: Hand, dealerCard: Card, shoe: Shoe): Map<Int, BigDecimal> {
    val dealerProbs = getDealerResultProbs(dealerCard, shoe)
    insertDealerProbs(playerHand, dealerCard, dealerProbs)
    return dealerProbs
}

/**
 * Get dealer results from dealerResultProbsSeq and collect them by score
 */
fun getDealerResultProbs(dealerCard: Card, shoe: Shoe): Map<Int, BigDecimal> {
    val dealerHand = Hand.fromCard(dealerCard)
    val seqList = dealerResultProbsSeq(dealerHand, shoe, BigDecimal(1))
            .toList()
    return seqList
            .groupByTo(hashMapOf(), { pair -> pair.first }, { pair -> pair.second })
            .entries.map {
        Pair(it.key, it.value.reduce { acc, next -> acc.plus(next) })
    }.toMap()
}

/**
 * Generator sequence of dealer Pair(Score, Probability) to be rolled up by Score, eg [(17, 0.25), (18, 0.03)]
 */
fun dealerResultProbsSeq(hand: Hand, shoe: Shoe, prob: BigDecimal): Sequence<Pair<Int, BigDecimal>> = sequence {
    for ((card, newProb) in shoe.getNextStatesAndProbabilities().shuffled()) {
        val newHand = hand.addCard(card)
        val newShoe = shoe.removeCard(card)
        var preferredVal = newHand.getPreferredValue()
        if (dealerFinished(newHand)) {
            if (preferredVal > 21) {
                preferredVal = 22
            }
            yield(Pair(preferredVal, prob.times(newProb)))
        } else {
            yieldAll(dealerResultProbsSeq(newHand, newShoe, prob.times(newProb)))
        }
    }
}

/**
 * Function that determines if the dealer should hit again or not.
 * The logic will depend on the rules of a particular table.
 */
fun dealerFinished(hand: Hand): Boolean {
    return hand.getPreferredValue() > 17 || hand.getHardValue() == 17
}

/**
 * Insert player, dealer, and probability values into the database.
 */
fun insertDealerProbs(playerHand: Hand, dealerCard: Card, dealerProbs: Map<Int, BigDecimal>) {
    DealerProbabilitiesModel.insertHand(playerHand, Hand.fromCard(dealerCard), dealerProbs)
}