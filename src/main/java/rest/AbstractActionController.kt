package rest

import calculator.Hand
import calculator.HandCalculator
import java.util.logging.Logger

abstract class AbstractActionController(private val calculator: HandCalculator) {
    abstract val logger: Logger
    abstract fun getAction(player: String,dealer: String, split: String?, insurance: Boolean): ActionResponse

    fun getActionHelper(player: String,dealer: String, split: String?, insurance: Boolean): ActionResponse {
        logger.info("GET $player, $split, $dealer, $insurance")
        val playerHand = Hand.fromUtf8(player.toByteArray())
        val dealerHand = Hand.fromUtf8(dealer.toByteArray())
        val splitHand = if (split == null) null else Hand.fromUtf8(split.toByteArray())
        val startingShoe = calculator.game.getStartingShoe()
        val shoe = if (splitHand == null) startingShoe.removeHand(playerHand) else startingShoe.removeHands(playerHand, splitHand)
        val (action, score) = calculator.getBestAction(playerHand, dealerHand, shoe, false, splitHand, false, insurance)
        return ActionResponse(action, score.toDouble())
    }
}