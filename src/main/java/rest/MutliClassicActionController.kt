package rest

import calculator.Hand
import calculator.classic.ClassicBlackJackGame
import calculator.classic.MultiClassicGameStateModel
import calculator.dealer.DealerProbabilitiesModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
class MutliClassicActionController {
    val logger: Logger = Logger.getLogger("MultiClassicActionController")
    val handCalc = calculator.MultiHandCalculator(ClassicBlackJackGame)

    init {
        DealerProbabilitiesModel.initialize()
        MultiClassicGameStateModel.initialize()
    }

    fun getActionHelper(player: String, dealer: String, insurance: Boolean, split: String?, cardsInPlay: String?, numSplits: Int): ActionResponse {
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
        val (action, score) = handCalc.getBestAction(playerHand, dealerHand, shoe, false, splitHand, cardsInPlayHand, false, false, numSplits)
        return ActionResponse(action, score.toDouble())
    }

    @GetMapping("/multiClassicAction")
    fun getAction(@RequestParam(value = "player") player: String, @RequestParam(value = "dealer") dealer: String, @RequestParam(value = "split") split: String?, @RequestParam(value = "cardsInPlay") cardsInPlay: String?, @RequestParam(value="insurance") insurance: Boolean): ActionResponse {

        val returnVal = this.getActionHelper(player, dealer, insurance, split, cardsInPlay, 0)
//        handCalc.db.flushCache()
//        DealerProbabilitiesModel.flushCache()
        return returnVal

    }
}