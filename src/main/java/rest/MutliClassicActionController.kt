package rest

import calculator.Hand
import calculator.classic.ClassicBlackJackGame
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
class MutliClassicActionController {
    val logger: Logger = Logger.getLogger("MultiClassicActionController")
    val handCalc = calculator.MultiHandCalculator(ClassicBlackJackGame)

    fun getActionHelper(player: String, dealer: String, insurance: Boolean, split: String?, cardsInPlay: String?): ActionResponse {
        logger.info("GET $dealer, $player, $split, $cardsInPlay")
        val playerHand = Hand.fromUtf8(player.toByteArray())
        val dealerHand = Hand.fromUtf8(dealer.toByteArray())
        val splitHand = if (split.isNullOrEmpty()) null else Hand.fromUtf8(split.toByteArray())
        val cardsInPlayHand = if (cardsInPlay.isNullOrEmpty()) null else Hand.fromUtf8(cardsInPlay.toByteArray())
        val startingShoe = handCalc.game.getStartingShoe()
        var shoe = startingShoe.removeHand(playerHand)
        shoe = shoe.removeHand(dealerHand)
        if (splitHand != null) {
            shoe = shoe.removeHand(splitHand)
        }
        if (cardsInPlayHand != null) {
            shoe = shoe.removeHand(cardsInPlayHand)
        }
        val (action, score) = handCalc.getBestAction(playerHand, dealerHand, shoe, false, splitHand, cardsInPlayHand, false, false)
        return ActionResponse(action, score.toDouble())
    }

    @GetMapping("/classicAction")
    fun getAction(@RequestParam(value = "player") player: String, @RequestParam(value = "dealer") dealer: String, @RequestParam(value = "split") split: String?, @RequestParam(value = "cardsInPlay") cardsInPlay: String?, @RequestParam(value="insurance") insurance: Boolean): ActionResponse {

        return this.getActionHelper(player, dealer, insurance, split, cardsInPlay)
    }
}