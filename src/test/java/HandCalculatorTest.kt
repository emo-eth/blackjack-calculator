import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import java.math.BigDecimal

object HandCalculatorTest : Spek({
    group("handCalculatorTest") {

        test("Stand on BJ") {
            var playerHand = fromCards(Card.TEN, Card.ACE)
            var dealerHand = fromCard(Card.TWO)
            var shoe = makeShoe(6)
            shoe = removeCard(Card.TEN, shoe)
            shoe = removeCard(Card.ACE, shoe)
            shoe = removeCard(Card.TWO, shoe)
            var (action, score) = getBestAction(
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


//        test("Stand on 20") {
//
//            var playerHand = fromCards(Card.TEN, Card.TEN)
//            var dealerHand = fromCard(Card.TWO)
//            var shoe = makeShoe(6)
//            shoe = removeCard(Card.TEN, shoe)
//            shoe = removeCard(Card.TEN, shoe)
//            shoe = removeCard(Card.TWO, shoe)
//            var (action, score) = getBestAction(
//                    playerHand,
//                    dealerHand,
//                    shoe,
//                    false,
//                    null,
//                    false,
//                    false
//            )
//            assertThat(action).isEqualTo(Action.STAND)
//        }

//        test("Hit (or double) on 10") {
//            var playerHand = fromCards(Card.FIVE, Card.FIVE)
//            var dealerHand = fromCard(Card.TWO)
//            var shoe = makeShoe(6)
//            shoe = removeCard(Card.FIVE, shoe)
//            shoe = removeCard(Card.FIVE, shoe)
//            shoe = removeCard(Card.TWO, shoe)
//
//            var (action, score) = getBestAction(
//                    playerHand,
//                    dealerHand,
//                    shoe,
//                    false,
//                    null,
//                    false,
//                    false
//            )
//            assertThat(action).isEqualTo(Action.DOUBLE)
//        }

//        test("Always split on aces") {
//            var playerHand = fromCards(Card.ACE, Card.ACE) // Hand.newHand(Card.ACE, Card.ACE)
//            var dealerHand = fromCard(Card.TEN)
//            var shoe = makeShoe(6)
//            shoe = removeCard(Card.ACE, shoe)
//            shoe = removeCard(Card.ACE, shoe)
//            shoe = removeCard(Card.TEN, shoe)
//            var (action, score) = getBestAction(
//                    playerHand,
//                    dealerHand,
//                    shoe,
//                    false,
//                    null,
//                    false,
//                    false
//            )
//            assertThat(action).isEqualTo(Action.SPLIT)
//        }

//        test("insurance works") {
//            var playerHand = fromCards(Card.FIVE, Card.NINE)
//            var dealerHand = fromCard(Card.ACE)
//            var shoe = makeShoe(6)
//            shoe = removeCard(Card.ACE, shoe)
//            shoe = removeCard(Card.FIVE, shoe)
//            shoe = removeCard(Card.NINE, shoe)
//            var (action, score) = getBestAction(playerHand, dealerHand, shoe, false, null, false, false)
//
//        }

//        test("split aces") {
//            var playerHand = fromCards(Card.ACE, Card.TEN)
//            var dealerHand = fromCard(Card.THREE)
//            var shoe = makeShoe(6)
//            var (action, score) = getBestAction(playerHand, dealerHand, shoe, false, fromCards(Card.ACE, Card.ACE), true, false)
//            assertThat(score.toDouble()).isStrictlyBetween(0.0, 1.0)
//
//        }

//        test("differences between splits") {
//            var shoe = makeShoe(6)
//            var playerHand = fromCards(Card.ACE, Card.TWO)
//            var dealerHand = fromCard(Card.TEN)
//            shoe = removeCard(Card.ACE, shoe)
//            shoe = removeCard(Card.TEN, shoe)
//            shoe = removeCard(Card.TEN, shoe)
//            var (action1, score1) = getBestAction(playerHand, dealerHand, shoe, false, fromCards(Card.ACE, Card.TWO), true, false)
//            var (action2, score2) = getBestAction(playerHand, dealerHand, shoe, false, fromCards(Card.ACE, Card.TWO), false, false)
//
//            assertThat(score1).isEqualTo(score2)
//        }

//        test("house edge") {
//            var scores: MutableList<BigDecimal> = mutableListOf()
//            var shoe = makeShoe(6)
//            for ((card, prob) in getNextStatesAndProbabilities(shoe)) {
//                var dealer = fromCard(card)
//                var shoeAfterDealer = removeCard(card, shoe)
//                for ((card2, prob2) in getNextStatesAndProbabilities(shoeAfterDealer)) {
//                    var player1Card = fromCard(card2)
//                    var shoeAfterPlayerCard1 = removeCard(card, shoeAfterDealer)
//                    for ((card3, prob3) in getNextStatesAndProbabilities(shoeAfterPlayerCard1)) {
//                        var player2Cards = addCard(card3, player1Card)
//                        var shoeAfterPlayerCard2 = removeCard(card3, shoeAfterPlayerCard1)
//                        var (action, score) = getBestAction(player2Cards, dealer, shoeAfterPlayerCard2, false, null, false, false)
//                        scores.add(score.times(prob).times(prob2).times(prob3))
//                    }
//                }
//            }
//            var overall = scores.reduce { x, y -> x.plus(y) }
//            println("House edge: " + overall)
//            // assertThat(overall).isStrictlyBetween(-1, 0)
//        }

    }
})