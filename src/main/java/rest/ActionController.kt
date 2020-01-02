package rest

import calculator.Hand
import calculator.Shoe
import calculator.platipus.getBestAction
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
class ActionController {
    val logger: Logger = Logger.getLogger("ActionController")

    @GetMapping("/action")
    fun getAction(@RequestParam(value = "player") player: String, @RequestParam(value = "dealer") dealer: String, @RequestParam(value = "split") split: String?, @RequestParam(value = "insurance") insurance: Boolean): ActionResponse {
        logger.info("GET $player, $split, $dealer, $insurance")
        val playerHand = Hand.fromUtf8(player.toByteArray())
        val dealerHand = Hand.fromUtf8(dealer.toByteArray())
        val splitHand = if (split == null) null else Hand.fromUtf8(split.toByteArray())
        val shoe = if (splitHand == null) Shoe.fromHands(6, playerHand, dealerHand) else Shoe.fromHands(6, playerHand, dealerHand, splitHand)
        val (action, score) = getBestAction(playerHand, dealerHand, shoe, false, splitHand, false, insurance)
        return ActionResponse(action, score.toDouble())

    }

}
