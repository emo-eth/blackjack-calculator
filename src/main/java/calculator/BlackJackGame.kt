package calculator

import java.math.BigDecimal

fun scoreHand(
        playerValue: Int, // TODO: consider taking a hand here anyway
        dealerHand: Hand,
        dealerStartingCard: Card,
        shoe: Shoe,
        blackJack: Boolean,
        double: Boolean,
        insurance: Boolean,
        splitAces: Boolean
): BigDecimal {
    var blackJackVar = blackJack
    if (splitAces) blackJackVar = false
    if (playerValue > 21) {
        if (!insurance) return BigDecimal(-1)
        else return BigDecimal(-1.5)
    }
    if (blackJackVar && dealerStartingCard != Card.ACE) {
        return BigDecimal(1.5)
    }
    if (dealerShouldHit(dealerHand)) {
        val scores: MutableList<BigDecimal> = mutableListOf()
        for ((card, prob) in shoe.getNextStatesAndProbabilities()) {
            val newShoe = shoe.removeCard(card)
            val newDealerHand = dealerHand.addCard(card)
            val score = scoreHand(
                    playerValue,
                    newDealerHand,
                    dealerStartingCard,
                    newShoe,
                    blackJackVar,
                    double,
                    insurance,
                    splitAces
            )
            scores.add(prob.times(score))
        }
        return scores.reduce { x, y -> x.plus(y) }
    }
    return BigDecimal(
            getUtility(
                    playerValue,
                    dealerHand.getPreferredValue(),
                    dealerHand.size,
                    dealerStartingCard,
                    blackJackVar,
                    double,
                    insurance,
                    splitAces
            )
    )
}

fun dealerShouldHit(dealerHand: Hand): Boolean {
    return dealerHand.getPreferredValue() < 17
}

fun shouldHitAfterSplit(playerHand: Hand, split: Boolean): Boolean {
    return split && playerHand.size == 1
}

fun canHit(playerHand: Hand, double: Boolean, splitAces: Boolean): Boolean {
    return (
            (!double && !splitAces && playerHand.getPreferredValue() < 21) ||
                    playerHand.size == 1
            )
}

fun canDouble(
        playerHand: Hand,
        double: Boolean,
        splitAces: Boolean
): Boolean {
    return !double && !splitAces && playerHand.size == 2
}

fun canSplit(playerHand: Hand, split: Hand?): Boolean {
    if (split != null || playerHand.size != 2) return false
    return playerHand[0] == playerHand[1]
}

fun canInsure(
        playerHand: Hand,
        dealerHand: Hand,
        double: Boolean,
        insurance: Boolean
): Boolean {
    // limit insurance to first turn
    return !insurance && !double && dealerHand.isSoft() && playerHand.size == 2
}

fun getUtility(
        playerValue: Int,
        dealerValue: Int,
        dealerLength: Int,
        dealerStartingCard: Card,
        blackJack: Boolean,
        double: Boolean,
        insurance: Boolean,
        splitAces: Boolean
): Double {
    var blackJackVar = blackJack
    if (splitAces) blackJackVar = false
    var insurancePayout = 0.0
    var doubleMultiplier = 1.0 // this is for calculating loss, calculations must still multiply by 2
    if (double && dealerValue == 21 && dealerLength == 2 && dealerStartingCard == Card.TEN) {
        doubleMultiplier = 0.5 // only lose 1/2 of bet if dealer bj on starting 10
    }
    if (insurance) {
        if (dealerValue == 21 && dealerLength == 2) {
            insurancePayout = 1.0
        } else {
            insurancePayout = -0.5
        }
    }
    if (!blackJackVar && dealerValue == 21 && dealerLength == 2) {
        return doubleMultiplier * -1 + insurancePayout
    }

    if (playerValue > 21) {
        // bust
        return doubleMultiplier * -1 + insurancePayout
    } else if (playerValue == dealerValue) {
        // equal
        return doubleMultiplier * 0 + insurancePayout
    } else if (blackJackVar) {
        // bj
        return 1.5 // this will never be doubled
    } else if (dealerValue > 21) {
        // dealer bust
        return doubleMultiplier * 1 + insurancePayout
    }
    if (playerValue > dealerValue) {
        // player beat dealer
        return doubleMultiplier * 1 + insurancePayout
    }
    return doubleMultiplier * -1 + insurancePayout // dealer beat player
}
