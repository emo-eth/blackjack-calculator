package calculator

import java.math.BigDecimal
import java.sql.SQLException
import java.util.logging.Logger
import java.util.stream.Collectors

private val logger: Logger = Logger.getLogger("HandCalculator")

fun getAllPossibleActions(
        playerHand: Hand,
        dealerHand: Hand,
        split: Hand?,
        splitAces: Boolean,
        insurance: Boolean
): List<Action> {
    val actions = mutableListOf(Action.STAND)
    if (splitAces) {
        if (playerHand.size == 1) return listOf(Action.HIT)
        if (playerHand.size == 2) return actions
    }
    if (playerHand.getPreferredValue() < 21) {
        actions.add(Action.HIT)
        if (playerHand.size == 2) {
            if (playerHand.getPreferredValue() < 21) actions.add(Action.DOUBLE)
            if (split == null && playerHand.size == 2 && playerHand[0] == playerHand[1])
                actions.add(Action.SPLIT)
        }
        if (!insurance && dealerHand.isSoft() && playerHand.size == 2)
            actions.add(Action.INSURANCE)
    }
    return actions
}

fun doHit(card: Card, playerHand: Hand, shoe: Shoe): Pair<Hand, Shoe> {
    val newPlayerHand = playerHand.addCard(card)
    val newShoe = shoe.removeCard(card)
    return Pair(newPlayerHand, newShoe)
}

fun doDouble(card: Card, playerHand: Hand, shoe: Shoe): Pair<Hand, Shoe> {
    return doHit(card, playerHand, shoe)
}

fun getActionsAndScores(playerHand: Hand,
                        dealerHand: Hand,
                        shoe: Shoe,
                        double: Boolean,
                        split: Hand?,
                        splitAces: Boolean,
                        insurance: Boolean): List<Pair<Action, BigDecimal>> {
    val possibleActions: List<Action> = getAllPossibleActions(
            playerHand,
            dealerHand,
            split,
            splitAces,
            insurance
    )
    val actions = getHand(insurance, split, splitAces, dealerHand, playerHand)
//    val actions: List<Pair<calculator.Action, Double>>? = emptyList()
    if (actions != null && actions.size != possibleActions.size) {
//        println(playerHand, dealerHand)
        // todo: string interpolation
        throw Exception("Mismatched actions, possible: ${possibleActions.joinToString(",")}, received: ${actions.joinToString(",")}")
    }
    if (
            actions == null ||
            actions.isEmpty()
    ) {
        logger.info("Computing hand ${String(playerHand.toUTF8())} ${if (split == null) "" else String(split.toUTF8())} ${String(dealerHand.toUTF8())} $insurance $splitAces")
        val calculatedActions = possibleActions.parallelStream().map {
            action ->
            val score = evaluateAction(
                    action,
                    playerHand,
                    dealerHand,
                    shoe,
                    double,
                    split,
                    splitAces,
                    insurance)
            Pair(action, score)
        }.collect(Collectors.toList())

        // sort descending
        calculatedActions.sortBy { x -> -x.second }

        try {
            insertHand(insurance, split, splitAces, dealerHand, playerHand, calculatedActions)
        } catch (ex: SQLException) {
            println(ex.message)
        }
        return calculatedActions
    }
    return actions.map { x -> Pair(x.first, x.second) }
}

fun getBestAction(
        playerHand: Hand,
        dealerHand: Hand,
        shoe: Shoe,
        double: Boolean,
        split: Hand?,
        splitAces: Boolean,
        insurance: Boolean
): Pair<Action, BigDecimal> {
    val actions = getActionsAndScores(playerHand, dealerHand, shoe, double, split, splitAces, insurance)
    return actions.filter { x ->
        canPerform(
                x.first,
                playerHand,
                dealerHand,
                double,
                split,
                splitAces,
                insurance
        )
    }[0]
}

fun canPerform(
        action: Action,
        playerHand: Hand,
        dealerHand: Hand,
        double: Boolean,
        split: Hand?,
        splitAces: Boolean,
        insurance: Boolean
): Boolean {
    return when (action) {
        Action.STAND -> true
        Action.HIT ->
            // playerHand value will always be <21 at this stage
            canHit(playerHand, double, splitAces)
        Action.DOUBLE ->
            canDouble(playerHand, double, splitAces)
        Action.INSURANCE ->
            canInsure(playerHand, dealerHand, double, insurance)
        Action.SPLIT ->
            canSplit(playerHand, split)
        Action.SURRENDER ->
            false
    }
}

fun evaluateAction(
        action: Action,
        playerHand: Hand,
        dealerHand: Hand,
        shoe: Shoe,
        double: Boolean,
        split: Hand?,
        splitAces: Boolean,
        insurance: Boolean
): BigDecimal {
    if (action == Action.STAND) {
        val playerValue = playerHand.getPreferredValue()
        return scoreHand(
                playerHand.getPreferredValue(),
                dealerHand,
                Card.fromByte(dealerHand[0]),
                shoe,
                playerValue == 21 && playerHand.size == 2,
                double,
                insurance,
                splitAces
        )
    } else if (action == Action.HIT) {
        val scores: MutableList<BigDecimal> = mutableListOf()
        for ((card, prob) in shoe.getNextStatesAndProbabilities().shuffled()) {
            val (newPlayerHand, newShoe) = doHit(card, playerHand, shoe)
            val (nextAction, score) = getBestAction(
                    newPlayerHand,
                    dealerHand,
                    newShoe,
                    double,
                    split,
                    splitAces,
                    insurance
            )
            scores.add(prob.times(score))
        }
        return scores.reduce { x, y -> x.plus(y) }
    } else if (action == Action.DOUBLE) {
        val scores: MutableList<BigDecimal> = mutableListOf()
        for ((card, prob) in shoe.getNextStatesAndProbabilities().shuffled()) {
            val (newPlayerHand, newShoe) = doDouble(card, playerHand, shoe)
            val (nextAction, score) = getBestAction(
                    newPlayerHand,
                    dealerHand,
                    newShoe,
                    true,
                    split,
                    splitAces,
                    insurance
            )
            scores.add(prob.times(score).times(BigDecimal(2)))
        }
        return scores.reduce { x, y -> x.plus(y) }
    } else if (action == Action.SPLIT) {
        val scores: MutableList<BigDecimal> = mutableListOf()
        for ((card, prob) in shoe.getNextStatesAndProbabilities().shuffled()) {
            val newShoe = shoe.removeCard(card)
            for ((card2, prob2) in newShoe.getNextStatesAndProbabilities().shuffled()) {
                val newShoe2 = newShoe.removeCard(card2)
                val hand1 = Hand.fromCards(Card.fromByte(playerHand[0]), card)
                val hand2 = Hand.fromCards(Card.fromByte(playerHand[0]), card2)
                // for n cards: in stand and double, pass resulting hand as the 'split' column for the split hand
                val (nextActionHand1, scoreHand1) = getBestAction(hand1, dealerHand, newShoe2, double, hand2, playerHand.isSoft(), insurance)
                val (nextActionHand2, scoreHand2) = getBestAction(hand2, dealerHand, newShoe2, double, hand1, playerHand.isSoft(), insurance)
                scores.add(scoreHand1.plus(scoreHand2).times(prob).times(prob2))
            }
        }
        return scores.reduce { x, y -> x.plus(y) }
    } else if (action == Action.INSURANCE) {
        val (nextAction, score) = getBestAction(
                playerHand,
                dealerHand,
                shoe,
                double,
                split,
                splitAces,
                true
        )
        return score
    }
    // no-op
    return BigDecimal(0)
}
