import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.dsl.Skip
import java.math.BigDecimal

object HandCalculatorTest : Spek({
    group("handCalculatorTest") {

        test("Stand on BJ") {
            val playerHand = fromCards(Card.TEN, Card.ACE)
            val dealerHand = fromCard(Card.TWO)
            val shoe = fromHands(6, playerHand, dealerHand)
            val (action, score) = getBestAction(
                    playerHand,
                    dealerHand,
                    shoe,
                    false,
                    null,
                    false,
                    false
            )
            assertThat(action).isEqualTo(Action.STAND)
        }


        test("Stand on 20") {

            val playerHand = fromCards(Card.TEN, Card.TEN)
            val dealerHand = fromCard(Card.TWO)
            val shoe = fromHands(6, playerHand, dealerHand)
            val (action, score) = getBestAction(
                    playerHand,
                    dealerHand,
                    shoe,
                    false,
                    null,
                    false,
                    false
            )
            assertThat(action).isEqualTo(Action.STAND)
        }

        test("Hit (or double) on 10", Skip.No, 60 * 1000) {

            val playerHand = fromCards(Card.FIVE, Card.FIVE)
            val dealerHand = fromCard(Card.TWO)
            val shoe = fromHands(6, playerHand, dealerHand)

            val (action, score) = getBestAction(
                    playerHand,
                    dealerHand,
                    shoe,
                    false,
                    null,
                    false,
                    false
            )
            assertThat(action).isEqualTo(Action.DOUBLE)
        }

        test("Always split on aces", Skip.No, 60 * 1000) {
            val playerHand = fromCards(Card.ACE, Card.ACE) // Hand.newHand(Card.ACE, Card.ACE)
            val dealerHand = fromCard(Card.TEN)
            val shoe = fromHands(6, playerHand, dealerHand)

            val (action, score) = getBestAction(
                    playerHand,
                    dealerHand,
                    shoe,
                    false,
                    null,
                    false,
                    false
            )
            assertThat(action).isEqualTo(Action.SPLIT)
        }

        test("insurance works") {
            val playerHand = fromCards(Card.FIVE, Card.NINE)
            val dealerHand = fromCard(Card.ACE)
            val shoe = fromHands(6, playerHand, dealerHand)
            val (action, score) = getBestAction(playerHand, dealerHand, shoe, false, null, false, false)

        }

        test("split aces") {
            val playerHand = fromCards(Card.ACE, Card.TEN)
            val dealerHand = fromCard(Card.THREE)
            val splitHand = fromCards(Card.ACE, Card.ACE)
            val shoe = fromHands(6, playerHand, dealerHand, splitHand)

            val (action, score) = getBestAction(playerHand, dealerHand, shoe, false, splitHand, true, false)
            assertThat(score.toDouble()).isStrictlyBetween(0.0, 1.0)

        }

        test("differences between splits") {
            val playerHand = fromCards(Card.ACE, Card.TWO)
            val dealerHand = fromCard(Card.TEN)
            val shoe = fromHands(6, playerHand, dealerHand)


            val (action1, score1) = getBestAction(playerHand, dealerHand, shoe, false, fromCards(Card.ACE, Card.TWO), true, false)
            val (action2, score2) = getBestAction(playerHand, dealerHand, shoe, false, fromCards(Card.ACE, Card.TWO), false, false)

            assertThat(score1).isEqualTo(score2)
        }

        test("house edge") {
            val scores: MutableList<BigDecimal> = mutableListOf()
            val shoe = makeShoe(6)
            for ((card, prob) in getNextStatesAndProbabilities(shoe)) {
                val dealer = fromCard(card)
                val shoeAfterDealer = removeCard(card, shoe)
                for ((card2, prob2) in getNextStatesAndProbabilities(shoeAfterDealer)) {
                    val player1Card = fromCard(card2)
                    val shoeAfterPlayerCard1 = removeCard(card, shoeAfterDealer)
                    for ((card3, prob3) in getNextStatesAndProbabilities(shoeAfterPlayerCard1)) {
                        val player2Cards = addCard(card3, player1Card)
                        val shoeAfterPlayerCard2 = removeCard(card3, shoeAfterPlayerCard1)
                        val (action, score) = getBestAction(player2Cards, dealer, shoeAfterPlayerCard2, false, null, false, false)
                        scores.add(score.times(prob).times(prob2).times(prob3))
                    }
                }
            }
            val overall = scores.reduce { x, y -> x.plus(y) }
            println("House edge: " + overall)
            assertThat(overall.toDouble()).isStrictlyBetween(-1.0, 0.0)
        }

    }
})