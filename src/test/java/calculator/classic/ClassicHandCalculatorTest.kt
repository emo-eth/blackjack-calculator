package calculator.classic

import calculator.*
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.dsl.Skip
import java.math.BigDecimal

object ClassicHandCalculatorTest : Spek({
    group("handCalculatorTest") {
        val calc = HandCalculator(ClassicBlackJackGame, ClassicGameStateModel)

        test("Stand on BJ") {
            val playerHand = Hand.fromCards(Card.TEN, Card.ACE)
            val dealerHand = Hand.fromCard(Card.TWO)
            val shoe = Shoe.fromHands(8, playerHand, dealerHand)
            val (action, score) = calc.getBestAction(
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

            0.until(10).forEach {
                val playerHand = Hand.fromCards(Card.TEN, Card.TEN)
                val dealerHand = Hand.fromCard(Card.fromByte(it.toByte()))
                val shoe = Shoe.fromHands(8, playerHand, dealerHand)
                val (action, score) = calc.getBestAction(
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
        }

        test("Hit (or double) on 10", Skip.No, 60 * 1000) {

            val playerHand = Hand.fromCards(Card.FIVE, Card.FIVE)
            val dealerHand = Hand.fromCard(Card.TWO)
            val shoe = Shoe.fromHands(8, playerHand, dealerHand)

            val (action, score) = calc.getBestAction(
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
            val playerHand = Hand.fromCards(Card.ACE, Card.ACE) // calculator.Hand.newHand(calculator.Card.ACE, calculator.Card.ACE)
            val dealerHand = Hand.fromCard(Card.TEN)
            val shoe = Shoe.fromHands(8, playerHand, dealerHand)

            val (action, score) = calc.getBestAction(
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
            val playerHand = Hand.fromCards(Card.FIVE, Card.NINE)
            val dealerHand = Hand.fromCard(Card.ACE)
            val shoe = Shoe.fromHands(8, playerHand, dealerHand)
            val (action, score) = calc.getBestAction(playerHand, dealerHand, shoe, false, null, false, false)

        }

        test("split aces") {
            val playerHand = Hand.fromCards(Card.ACE, Card.TEN)
            val dealerHand = Hand.fromCard(Card.THREE)
            val splitHand = Hand.fromCards(Card.ACE, Card.ACE)
            val shoe = Shoe.fromHands(8, playerHand, dealerHand, splitHand)

            val (action, score) = calc.getBestAction(playerHand, dealerHand, shoe, false, splitHand, true, false)
            assertThat(score.toDouble()).isStrictlyBetween(0.0, 1.0)

        }

        test("differences between splits") {
            val playerHand = Hand.fromCards(Card.ACE, Card.TWO)
            val dealerHand = Hand.fromCard(Card.TEN)
            val shoe = Shoe.fromHands(8, playerHand, dealerHand)


            val (action1, score1) = calc.getBestAction(playerHand, dealerHand, shoe, false, Hand.fromCards(Card.ACE, Card.TWO), true, false)
            val (action2, score2) = calc.getBestAction(playerHand, dealerHand, shoe, false, Hand.fromCards(Card.ACE, Card.TWO), false, false)

            assertThat(score1).isNotEqualTo(score2)
        }

        test("house edge") {
            val scores: MutableList<BigDecimal> = mutableListOf()
            val shoe = Shoe(8)
            for ((card, prob) in shoe.getNextStatesAndProbabilities()) {
                val dealer = Hand.fromCard(card)
                val shoeAfterDealer = shoe.removeCard(card)
                for ((card2, prob2) in shoeAfterDealer.getNextStatesAndProbabilities()) {
                    val player1Card = Hand.fromCard(card2)
                    val shoeAfterPlayerCard1 = shoe.removeCard(card)
                    for ((card3, prob3) in shoeAfterPlayerCard1.getNextStatesAndProbabilities()) {
                        val player2Cards = player1Card.addCard(card3)
                        val shoeAfterPlayerCard2 = shoe.removeCard(card3)
                        val (action, score) = calc.getBestAction(player2Cards, dealer, shoeAfterPlayerCard2, false, null, false, false)
                        scores.add(score.times(prob).times(prob2).times(prob3))
                    }
                }
            }
            val overall = scores.reduce { x, y -> x.plus(y) }
            println("House edge: " + overall)
            assertThat(overall.toDouble()).isStrictlyBetween(-1.0, 0.0)
        }

        test("Batching") {
            val playerHand = Hand.fromCards(Card.FIVE, Card.ACE)
            val playerSplit = Hand.fromCards(Card.FIVE, Card.FIVE, Card.FIVE, Card.FIVE)
            val dealerHand = Hand.fromCard(Card.FIVE)
            val shoe = Shoe.fromHands(8, playerHand, dealerHand, playerSplit)
            val (action, score) = calc.getBestAction(playerHand, dealerHand, shoe, false, playerSplit, false, false)
            println(action)
            println(score)
        }

    }
})