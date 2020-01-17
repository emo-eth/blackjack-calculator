package calculator

import calculator.classic.MultiClassicGameStateModel
import java.math.BigDecimal
import java.nio.charset.Charset
import java.sql.SQLException
import java.util.logging.Logger

class MultiHandCalculator(val game: AbstractBlackJackGame) {
    private val logger: Logger = Logger.getLogger("HandCalculator")
    val db = MultiClassicGameStateModel

    protected fun doHit(card: Card, playerHand: Hand, shoe: Shoe): Pair<Hand, Shoe> {
        val newPlayerHand = playerHand.addCard(card)
        val newShoe = shoe.removeCard(card)
        return Pair(newPlayerHand, newShoe)
    }

    protected fun doDouble(card: Card, playerHand: Hand, shoe: Shoe): Pair<Hand, Shoe> {
        return doHit(card, playerHand, shoe)
    }

    fun getActionsAndScores(playerHand: Hand,
                            dealerHand: Hand,
                            shoe: Shoe,
                            double: Boolean,
                            split: Hand?,
                            cardsInPlay: Hand?,
                            splitAces: Boolean,
                            insurance: Boolean): List<Pair<Action, BigDecimal>> {
        val possibleActions: List<Action> = game.getAllPossibleActions(
                playerHand,
                dealerHand,
                split,
                splitAces,
                insurance
        )
        val actions = db.getHand(insurance, split, cardsInPlay, splitAces, dealerHand, playerHand)
        if (actions != null && actions.size != possibleActions.size && !(actions.map{it.first}.contains(Action.SPLIT) && !possibleActions.contains(Action.SPLIT))) {
            throw Exception("Mismatched actions, possible: ${possibleActions.joinToString(",")}, received: ${actions.joinToString(",")}. Player value ${playerHand.getPreferredValue()}")
        }
        if (actions == null || actions.isEmpty()) {
            logger.info("Computing hand ${String(dealerHand.toUTF8())} ${String(playerHand.toUTF8())} ${split?.toUTF8()?.toString(Charset.defaultCharset()) ?: ""} ${cardsInPlay?.toUTF8()?.toString(Charset.defaultCharset()) ?: ""} ins: $insurance splitAce: $splitAces")
            val calculatedActions = possibleActions.map { action ->
                val score = evaluateAction(
                        action,
                        playerHand,
                        dealerHand,
                        shoe,
                        double,
                        split,
                        cardsInPlay,
                        splitAces,
                        insurance)
                Pair(action, score)
            }

            // sort descending
            val sorted = calculatedActions.sortedBy { x -> -x.second }

            try {
                db.insertHand(insurance, split, cardsInPlay, splitAces, dealerHand, playerHand, sorted)
            } catch (ex: SQLException) {
                println(ex.message)
            }
            return sorted
        }
        return actions.map { x -> Pair(x.first, x.second) }
    }

    fun getBestAction(
            playerHand: Hand,
            dealerHand: Hand,
            shoe: Shoe,
            double: Boolean,
            split: Hand?,
            cardsInPlay: Hand?,
            splitAces: Boolean,
            insurance: Boolean
    ): Pair<Action, BigDecimal> {
        if (game.canSplit(playerHand, split) && game.shouldSplit(playerHand, split)) {
            return Pair(Action.SPLIT, BigDecimal(-1))
        }
        val actions = getActionsAndScores(playerHand, dealerHand, shoe, double, split, cardsInPlay, splitAces, insurance)
        val performActions = actions.filter { x ->
            game.canPerform(
                    x.first,
                    playerHand,
                    dealerHand,
                    double,
                    split,
                    splitAces,
                    insurance
            )
        }
        return performActions[0]
    }

    fun evaluateAction(
            action: Action,
            playerHand: Hand,
            dealerHand: Hand,
            shoe: Shoe,
            double: Boolean,
            split: Hand?,
            cardsInPlay: Hand?,
            splitAces: Boolean,
            insurance: Boolean
    ): BigDecimal {
        return when (action) {
            (Action.STAND) -> {
                val combinedHands = if (split != null) (if (cardsInPlay != null) Hand.combineHands(split, cardsInPlay) else split) else null
                val playerValue = playerHand.getPreferredValue()
                game.scoreHand(
                        playerHand,
                        combinedHands,
                        dealerHand,
                        shoe,
                        playerValue == 21 && playerHand.size == 2,
                        double,
                        insurance,
                        splitAces
                )
            }
            (Action.HIT) -> {
                val scores: MutableList<BigDecimal> = mutableListOf()
                for ((card, prob) in shoe.getNextStatesAndProbabilities().shuffled()) {
                    val (newPlayerHand, newShoe) = doHit(card, playerHand, shoe)
                    val (nextAction, score) = getBestAction(
                            newPlayerHand,
                            dealerHand,
                            newShoe,
                            double,
                            split,
                            cardsInPlay,
                            splitAces,
                            insurance
                    )
                    scores.add(prob.times(score))
                }
                scores.reduce { x, y -> x.plus(y) }
            }
            (Action.DOUBLE) -> {
                val scores: MutableList<BigDecimal> = mutableListOf()
                for ((card, prob) in shoe.getNextStatesAndProbabilities().shuffled()) {
                    val (newPlayerHand, newShoe) = doDouble(card, playerHand, shoe)
                    val (nextAction, score) = getBestAction(
                            newPlayerHand,
                            dealerHand,
                            newShoe,
                            true,
                            split,
                            cardsInPlay,
                            splitAces,
                            insurance
                    )
                    scores.add(prob.times(score))
                }
                scores.reduce { x, y -> x.plus(y) }.times(BigDecimal(2))
            }
            (Action.SPLIT) -> {
                val scores: MutableList<BigDecimal> = mutableListOf()
                for ((card, prob) in shoe.getNextStatesAndProbabilities().shuffled()) {
                    val newShoe = shoe.removeCard(card)
                    for ((card2, prob2) in newShoe.getNextStatesAndProbabilities().shuffled()) {
                        val newShoe2 = newShoe.removeCard(card2)
                        val hand1 = Hand.fromCards(Card.fromByte(playerHand[0]), card)
                        val hand2 = Hand.fromCards(Card.fromByte(playerHand[0]), card2)
                        // for n cards: in stand and double, pass resulting hand as the 'split' column for the split hand
                        // TODO: consider passing in cardsInPlay with a faster computer
                        val (nextActionHand1, scoreHand1) = getBestAction(hand1, dealerHand, newShoe2, double, hand2, null, playerHand.isSoft(), insurance)
                        val (nextActionHand2, scoreHand2) = getBestAction(hand2, dealerHand, newShoe2, double, hand1, null, playerHand.isSoft(), insurance)
                        scores.add(scoreHand1.plus(scoreHand2).times(prob).times(prob2))
                    }
                }
                scores.reduce { x, y -> x.plus(y) }
            }
            (Action.INSURANCE) -> {
                // todo: technically not safe but there will never be missing cards on first deal when insurance is an option
                val probDealerHasTen = shoe.getNextStatesAndProbabilities()[9].second
                val probDealerDoesNotHaveBlackjack = BigDecimal(1).minus(probDealerHasTen)
                val (nextAction, score) = getBestAction(
                        playerHand,
                        dealerHand,
                        shoe,
                        double,
                        split,
                        cardsInPlay,
                        splitAces,
                        true
                )
                // subtract out insurance
                score.minus(BigDecimal(0.5)).times(probDealerDoesNotHaveBlackjack)
            }
            // no-op
            else -> BigDecimal(0)
        }
    }
}