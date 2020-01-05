package calculator.classic

import calculator.Card
import calculator.Hand
import calculator.Shoe
import com.nhaarman.mockitokotlin2.*
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import java.math.BigDecimal


object BlackJackClassicGameTest : Spek({
    group("BlackJackClassicGameTest") {

        test("Agent calculates utility in hands properly") {

            var shoe = Shoe(8)
            val spy = spy(ClassicBlackJackGame)

            var playerHand = Hand.fromCards(Card.ACE, Card.TEN)
            var dealerHand = Hand.fromCard(Card.NINE)
            var utility = spy.scoreHand(
                    playerHand, null,
                    dealerHand,
                    shoe,
                    true,
                    false,
                    false,
                    false
            )
            assertThat(utility.toDouble()).isEqualTo(1.5)

            shoe = Shoe.fromCounts(Pair(Card.ACE, 1), Pair(Card.NINE, 1))
            dealerHand = Hand.fromCard(Card.TEN)
            utility = spy.scoreHand(
                    playerHand, null,
                    dealerHand,
                    shoe,
                    true,
                    false,
                    false,
                    false
            )
            assertThat(utility.toDouble()).isEqualTo(0.75)

            var playerHand2 = Hand.fromCards(Card.NINE, Card.ACE, Card.ACE)
            doReturn(mapOf(Pair(21, BigDecimal(0.5)), Pair(20, BigDecimal(0.5)))).whenever(spy).getDealerProbs(eq(playerHand2), eq(null), eq(dealerHand))
            utility = spy.scoreHand(
                    playerHand2, null,
                    dealerHand,
                    shoe,
                    false,
                    false,
                    false,
                    false
            )
            assertThat(utility.toDouble()).isEqualTo(0.5)
            reset(spy)


            TODO()
            dealerHand = playerHand2
            utility = spy.scoreHand(
                    playerHand, null,
                    dealerHand,
                    shoe,
                    false,
                    false,
                    false,
                    false
            )
            assertThat(utility.toDouble()).isEqualTo(-1.0)
            utility = spy.scoreHand(
                    playerHand, null,
                    dealerHand,
                    shoe,
                    false,
                    false,
                    true,
                    false
            )
            assertThat(utility.toDouble()).isEqualTo(-1.5)

        }

        test("getactions makes sense") {
            var playerHand = Hand.fromCards(Card.ACE, Card.ACE)
            var dealerHand = Hand.fromCards(Card.TEN)
            var actions = ClassicBlackJackGame.getAllPossibleActions(
                    playerHand,
                    dealerHand,
                    null,
                    false,
                    false
            )
            assertThat(actions.size).isEqualTo(3)
        }

        test("calculator.Platipus.BlackJackClassicGame.getUtility returns reasonable values") {
            val bjc = spy(ClassicBlackJackGame)
//            whenever(bjc)
            val dummyShoe = Shoe(8)
            var playerHand = Hand.fromCards(Card.ACE, Card.NINE)
            var splitHand = Hand.fromCards(Card.ACE, Card.ACE)
            var dealerHand = Hand.fromCard(Card.TWO)
            var returnMap = mapOf(Pair(22, BigDecimal(1)))
            doReturn(returnMap).whenever(bjc).getDealerProbs(eq(playerHand), eq(splitHand), eq(dealerHand))
            assertThat(bjc.scoreHand(playerHand, splitHand, dealerHand, dummyShoe, false, false, false, false)).isEqualTo(BigDecimal(1))

            playerHand = Hand.fromCards(Card.TEN, Card.FIVE)
            splitHand = Hand.fromCards(Card.TEN, Card.EIGHT)
            returnMap = mapOf(Pair(21, BigDecimal(1)))
            doReturn(returnMap).whenever(bjc).getDealerProbs(playerHand, splitHand, dealerHand)
            assertThat(bjc.scoreHand(playerHand, splitHand, dealerHand, dummyShoe, false, false, false, false)).isEqualTo(BigDecimal(-1.0))

            playerHand = Hand.fromCards(Card.TEN, Card.ACE)
            dealerHand = Hand.fromCard(Card.NINE)
            returnMap = mapOf(Pair(20, BigDecimal(1)))
            doReturn(returnMap).whenever(bjc).getDealerProbs(playerHand, splitHand, dealerHand)
            assertThat(bjc.scoreHand(playerHand, splitHand, dealerHand, dummyShoe, true, false, false, false)).isEqualTo(BigDecimal(1.5))

            playerHand = Hand.fromCards(Card.ACE, Card.TEN)
            splitHand = Hand.fromCards(Card.ACE, Card.TEN)
            doReturn(returnMap).whenever(bjc).getDealerProbs(playerHand, splitHand, dealerHand)
            assertThat(bjc.scoreHand(playerHand, splitHand, dealerHand, dummyShoe, true, false, true, false)).isEqualTo(BigDecimal(1.5))

            // TODO: test insurance? or nah
            playerHand = Hand.fromCards(Card.ACE, Card.TEN)
            splitHand = Hand.fromCards(Card.ACE, Card.TEN)
            doReturn(returnMap).whenever(bjc).getDealerProbs(playerHand, splitHand, dealerHand)
            assertThat(bjc.scoreHand(playerHand, splitHand, dealerHand, dummyShoe, true, false, true, true)).isEqualTo(BigDecimal(1.0))

            playerHand = Hand.fromCards(Card.TEN, Card.EIGHT)
            returnMap = mapOf(Pair(18, BigDecimal(1)))
            doReturn(returnMap).whenever(bjc).getDealerProbs(playerHand, splitHand, dealerHand)
            assertThat(bjc.scoreHand(playerHand, splitHand, dealerHand, dummyShoe, false, false, true, false)).isEqualTo(BigDecimal(0.0))
        }

        test("calculator.Platipus.BlackJackClassicGame.scoreHand returns reasonable values") {
            var shoe = Shoe(8)
            var playerHand = Hand.fromCards(Card.ACE, Card.NINE)
            var splitHand: Hand? = null
            var dealerHand = Hand.fromCard(Card.TEN)
            assertThat(
                    ClassicBlackJackGame.scoreHand(playerHand, splitHand, dealerHand, shoe, false, false, false, false).toDouble()
            ).isStrictlyBetween(0.0, 1.0)
            playerHand = Hand.fromCards(Card.TWO, Card.TWO)
            assertThat(ClassicBlackJackGame.scoreHand(playerHand, splitHand, dealerHand, shoe, false, false, false, false).toDouble()).isStrictlyBetween(
                    -1.0,
                    0.0
            )
        }
    }
})

//
//        test("score hands works") {
//            assertThat(
//                    BlackJackClassicGame.scoreHand(
//                            21,
//                            Hand.fromCard(Card.fromByte(1)),
//                            Card.fromByte(1),
//                            Shoe.fromHands(6,
//                                    Hand.fromCards(*listOf(0, 9).map { Card.fromByte(it.toByte()) }.toTypedArray()),
//                                    Hand.fromCard(Card.fromByte(1))),
//                            true,
//                            false,
//                            false,
//                            false
//                    )).isEqualTo(BigDecimal(1.5))
//
//
//            assertThat(BlackJackClassicGame.scoreHand(
//                    21,
//                    HandHelper.of(1),
//                    Card.fromByte(0),
//                    Shoe.fromHands(6, HandHelper.of(0, 9), HandHelper.of(1)),
//                    true,
//                    false,
//                    false,
//                    false
//            ).toDouble()).isStrictlyBetween(0.0, 1.5)
//            assertThat(
//                    BlackJackClassicGame.scoreHand(
//                            21,
//                            HandHelper.of(0),
//                            Card.fromByte(0),
//                            Shoe.fromHands(6, HandHelper.of(0, 9), HandHelper.of(1)),
//                            true,
//                            false,
//                            true,
//                            false
//                    ).toDouble()).isStrictlyBetween(0.0, 1.5)
//        }
//    }
//
//    test("score states") {
//
//        // bj, regular dealer
//        var score: BigDecimal
//        var player = Hand.fromCards(Card.ACE, Card.TEN)
//        var dealer = Hand.fromCard(Card.FIVE)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.FIVE,
//                Shoe.fromHands(6, player, dealer),
//                true,
//                false,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(1.5)
//
//        // bj, ace hole, no insurance
//        player = Hand.fromCards(Card.ACE, Card.TEN)
//        dealer = Hand.fromCard(Card.ACE)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, player, dealer),
//                true,
//                false,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isStrictlyBetween(0.0, 1.5)
//
//        // bj, ace hole, insurance
//        player = Hand.fromCards(Card.ACE, Card.TEN)
//        dealer = Hand.fromCard(Card.ACE)
//        var insScore = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, player, dealer),
//                true,
//                false,
//                true,
//                false
//        )
//        assertThat(insScore.toDouble()).isGreaterThan(score.toDouble())
//
//        // both bj, no insurance
//        player = Hand.fromCards(Card.ACE, Card.TEN)
//        dealer = Hand.fromCards(Card.ACE, Card.TEN)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                true,
//                false,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(0.0)
//
//        // both bj, insurance
//        player = Hand.fromCards(Card.ACE, Card.TEN)
//        dealer = Hand.fromCards(Card.ACE, Card.TEN)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                true,
//                false,
//                true,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(1.0)
//
//        // 21, no ace hole
//        player = Hand.fromCards(Card.ACE, Card.EIGHT, Card.TWO)
//        dealer = Hand.fromCard(Card.FIVE)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.FIVE,
//                Shoe.fromHands(6, player, dealer),
//                false,
//                false,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isStrictlyBetween(0.0, 1.0)
//
//        // 21, ace hole, no insurance
//        player = Hand.fromCards(Card.ACE, Card.EIGHT, Card.TWO)
//        dealer = Hand.fromCard(Card.ACE)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, player, dealer),
//                false,
//                false,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isStrictlyBetween(0.0, 1.0)
//
//        // 21, ace hole, insurance
//        player = Hand.fromCards(Card.ACE, Card.EIGHT, Card.TWO)
//        dealer = Hand.fromCard(Card.ACE)
//        insScore = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, player, dealer),
//                false,
//                false,
//                true,
//                false
//        )
//        assertThat(insScore.toDouble()).isStrictlyBetween(0.0, 1.0)
//
//        // bust, ace hole, no insurance
//        player = Hand.fromCards(Card.TEN, Card.NINE, Card.TWO, Card.TWO)
//        dealer = Hand.fromCard(Card.ACE)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(-1.0)
//
//        // bust, ace hole, insurance
//        player = Hand.fromCards(Card.TEN, Card.NINE, Card.TWO, Card.TWO)
//        dealer = Hand.fromCard(Card.ACE)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                true,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(-1.5)
//
//        // player > dealer, no insurance
//        player = Hand.fromCards(Card.ACE, Card.NINE)
//        dealer = Hand.fromCards(Card.ACE, Card.SIX)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(1.0)
//
//        // player > dealer, insurance
//        player = Hand.fromCards(Card.ACE, Card.NINE)
//        dealer = Hand.fromCards(Card.ACE, Card.SIX)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                true,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(0.5)
//
//        // player < dealer, no insurance
//        player = Hand.fromCards(Card.ACE, Card.SIX)
//        dealer = Hand.fromCards(Card.ACE, Card.NINE)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(-1.0)
//
//        // player < dealer, insurance
//        player = Hand.fromCards(Card.ACE, Card.SIX)
//        dealer = Hand.fromCards(Card.ACE, Card.NINE)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                true,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(-1.5)
//
//        // dealer bust, no insurance
//        player = Hand.fromCards(Card.ACE, Card.SIX)
//        dealer = Hand.fromCards(Card.ACE, Card.NINE, Card.TEN, Card.TEN)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(1.0)
//
//        // dealer bust, insurance
//        player = Hand.fromCards(Card.ACE, Card.SIX)
//        dealer = Hand.fromCards(Card.ACE, Card.NINE, Card.TEN, Card.TEN)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                true,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(0.5)
//
//        // equal, no insurance
//        player = Hand.fromCards(Card.ACE, Card.SIX)
//        dealer = Hand.fromCards(Card.ACE, Card.SIX)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(0.0)
//
//        // equal, insurance
//        player = Hand.fromCards(Card.ACE, Card.SIX)
//        dealer = Hand.fromCards(Card.ACE, Card.SIX)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                true,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(-0.5)
//
//        // dealer 21, no ace hole
//        player = Hand.fromCards(Card.ACE, Card.SIX)
//        dealer = Hand.fromCards(Card.ACE, Card.EIGHT, Card.TWO)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.EIGHT,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(-1.0)
//
//        // dealer 21, ace hole, insurance
//        player = Hand.fromCards(Card.ACE, Card.SIX)
//        dealer = Hand.fromCards(Card.ACE, Card.EIGHT, Card.TWO)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                true,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(-1.5)
//
//        // dealer 21, ace hole, equal, no ins
//        player = Hand.fromCards(Card.ACE, Card.EIGHT, Card.TWO)
//        dealer = Hand.fromCards(Card.ACE, Card.EIGHT, Card.TWO)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(0.0)
//
//        // dealer 21, ace hole, equal, ins
//        player = Hand.fromCards(Card.ACE, Card.EIGHT, Card.TWO)
//        dealer = Hand.fromCards(Card.ACE, Card.EIGHT, Card.TWO)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                true,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(-0.5)
//
//        // dealer blackjack, player 21, no ins
//        player = Hand.fromCards(Card.ACE, Card.EIGHT, Card.TWO)
//        dealer = Hand.fromCards(Card.ACE, Card.TEN)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                false,
//                false
//        )
//        // TODO: does dealer BJ beat 21??
//        assertThat(score.toDouble()).isEqualTo(-1.0)
//
//        // dealer blackjack, player 21, ins
//
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                true,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(0.0)
//
//        // dealer blackjack, face ace, player 21, double, no ins
//        player = Hand.fromCards(Card.ACE, Card.EIGHT, Card.TWO)
//        dealer = Hand.fromCards(Card.ACE, Card.TEN)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                true,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(-1.0)
//
//        // dealer blackjack, face ten, player 21, double, no ins
//        player = Hand.fromCards(Card.ACE, Card.EIGHT, Card.TWO)
//        dealer = Hand.fromCards(Card.ACE, Card.TEN)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.TEN,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                true,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(-0.5)
//
//        // dealer blackjack, face ace, player 21, double, ins
//        player = Hand.fromCards(Card.ACE, Card.EIGHT, Card.TWO)
//        dealer = Hand.fromCards(Card.ACE, Card.TEN)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.ACE,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                true,
//                true,
//                false
//        )
//        // TODO: idk man
//        // assertThat(score.toDouble()).isEqualTo(-1.0)
//
//        // dealer win, double
//        player = Hand.fromCards(Card.SEVEN, Card.EIGHT, Card.TWO)
//        dealer = Hand.fromCards(Card.EIGHT, Card.TEN)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.TEN,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                true,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(-1.0)
//
//        // player win, double
//        player = Hand.fromCards(Card.SEVEN, Card.EIGHT, Card.THREE)
//        dealer = Hand.fromCards(Card.SEVEN, Card.TEN)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.TEN,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                true,
//                false,
//                false
//        )
//        assertThat(score.toDouble()).isEqualTo(1.0)
//
//        // player win, double
//        player = Hand.fromCards(Card.ACE, Card.TEN)
//        dealer = Hand.fromCards(Card.SEVEN, Card.TEN)
//        score = BlackJackClassicGame.scoreHand(
//                player.getPreferredValue(),
//                dealer,
//                Card.TEN,
//                Shoe.fromHands(6, dealer, player),
//                false,
//                false,
//                false,
//                true
//        )
//        assertThat(score.toDouble()).isEqualTo(1.0)
//    }
//})
//
//object HandHelper {
//    fun of(vararg ints: Int): Hand {
//        return Hand.fromCards(*ints.map { Card.fromByte(it.toByte()) }.toTypedArray())
//    }
//}
