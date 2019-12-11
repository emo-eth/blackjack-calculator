package calculator

import java.math.BigDecimal

fun calculateSingleHands(playerHand: Hand, dealerCard: Card, shoe: Shoe) {
    // todo: remember to remove dealer card from shoe
    for (next in shoe.getNextStatesAndProbabilities().shuffled()) {
        val card = next.first
        val newHand = playerHand.addCard(card)
        val newShoe = shoe.removeCard(card)
        if (handFinished(newHand)) {
            calculateAndInsertDealerProbs(newHand, dealerCard, newShoe)
        }
        // handUnfinished is not the opposite of handFinished,
        // plus we want to hit no matter what if < 21
        if (handUnfinished(newHand)) {
            calculateSingleHands(newHand, dealerCard, newShoe)
        }
    }
}

fun calculateSplitHands(playerHand: Hand, splitHand: Hand, dealerCard: Card, shoe: Shoe) {
    for (next in shoe.getNextStatesAndProbabilities().shuffled()) {
        val card = next.first
        val newHand = playerHand.addCard(card)
        val newShoe = shoe.removeCard(card)
        if (handFinished(newHand)) {
            if (handFinished(splitHand)) {
                // insert
                calculateAndInsertDealerProbs(Hand.combineHands(playerHand, splitHand), dealerCard, newShoe)
            } else {
                calculateSplitHands(splitHand, newHand, dealerCard, newShoe)
            }
        }
        if (handUnfinished(newHand)){
            calculateSplitHands(newHand, splitHand, dealerCard, newShoe)
        }
    }
}

fun handFinished(hand: Hand): Boolean {
    val preferredVal = hand.getPreferredValue()
    return IntRange(11, 21).contains(preferredVal)
}

fun handUnfinished(hand: Hand): Boolean {
    return hand.getPreferredValue() < 21
}

fun calculateAndInsertDealerProbs(playerHand: Hand, dealerCard: Card, shoe: Shoe) {
    val dealerProbs = getDealerResultProbs(dealerCard, shoe)
    insertDealerProbs(playerHand, dealerCard, dealerProbs)
}

/**
 * Get dealer results from dealerResultProbsSeq and collect them by score
 */
fun getDealerResultProbs(dealerCard: Card, shoe: Shoe): List<Pair<Int, BigDecimal>> {
    val dealerHand = Hand.fromCard(dealerCard)
    return dealerResultProbsSeq(dealerHand, shoe, BigDecimal(1))
            .toList()
            .groupByTo(hashMapOf(), { pair -> pair.first }, { pair -> pair.second })
            .entries.map {
        Pair(it.key, it.value.reduce { acc, next -> acc.plus(next) })
    }

}

/**
 * Generator sequence of dealer Pair(Score, Probability) to be rolled up by Score, eg [(17, 0.25), (18, 0.03)]
 */
fun dealerResultProbsSeq(hand: Hand, shoe: Shoe, prob: BigDecimal): Sequence<Pair<Int, BigDecimal>> = sequence {
    for (next in shoe.getNextStatesAndProbabilities().shuffled()) {
        val card = next.first
        val newProb = next.second
        val newHand = hand.addCard(card)
        val newShoe = shoe.removeCard(card)
        var preferredVal = newHand.getPreferredValue()
        if (dealerFinished(newHand, preferredVal)) {
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
fun dealerFinished(hand: Hand, preferredVal: Int): Boolean {
    return preferredVal >= 17
}

/**
 * Insert player, dealer, and probability values into the database.
 */
fun insertDealerProbs(playerHand: Hand, dealerCard: Card, dealerProbs: List<Pair<Int, BigDecimal>>) {

}