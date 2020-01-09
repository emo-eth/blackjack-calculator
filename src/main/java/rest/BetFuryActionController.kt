package rest

import calculator.HandCalculator
import calculator.betfury.BetFuryBlackJackGame
import calculator.betfury.BetFuryGameStateModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
object BetFuryActionController : AbstractActionController(HandCalculator(BetFuryBlackJackGame, BetFuryGameStateModel)) {
    override val logger: Logger = Logger.getLogger("BetFuryActionController")

    @GetMapping("/betFuryAction")
    override fun getAction(@RequestParam(value = "player") player: String, @RequestParam(value = "dealer") dealer: String, @RequestParam(value = "split") split: String?, @RequestParam(value = "insurance") insurance: Boolean): ActionResponse {
        return getActionHelper(player, dealer, split, insurance)
    }
}