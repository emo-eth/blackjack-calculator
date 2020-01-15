package rest

import calculator.HandCalculator
import calculator.classic.ClassicBlackJackGame
import calculator.classic.ClassicGameStateModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
object ClassicActionController : AbstractActionController(HandCalculator(ClassicBlackJackGame, ClassicGameStateModel)) {
    override val logger: Logger = Logger.getLogger("ActionController")

    @GetMapping("/classicAction")
    override fun getAction(@RequestParam(value = "player") player: String, @RequestParam(value = "dealer") dealer: String, @RequestParam(value = "split") split: String, @RequestParam(value = "insurance") insurance: Boolean): ActionResponse {
        return getActionHelper(player, dealer, split, insurance)
    }
}
