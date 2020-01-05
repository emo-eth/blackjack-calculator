package calculator

abstract class AbstractBlackJackDealerHitOnSoft17Game : AbstractBlackJackGame() {
    override fun dealerShouldHit(dealerHand: Hand): Boolean {
        return dealerHand.getPreferredValue() <= 17 && dealerHand.getHardValue() != 17
    }
}