package calculator.classic
import calculator.*
import calculator.dealer.DealerProbabilitiesModel
import java.math.BigDecimal


class MultiDatabasePopulator() {
    val db = MultiClassicGameStateModel
    val game = ClassicBlackJackGame


    fun main(cardsInPlay: Hand) {
        DealerProbabilitiesModel.loadFullCacheMap()
        db.initialize()
//        println(args[0].toInt())
        println(Runtime.getRuntime().availableProcessors())
        val threads = IntArray(12)
        0.until(10).toList().forEach {
            println(it)
            threads.asList().parallelStream().forEach { _ ->
                doForDealerCard(Card.fromByte(it.toByte()), cardsInPlay)
            }
        }
        db.flushCache()
        getHouseEdge(cardsInPlay)
    }

    fun doForDealerCard(dealerCard: Card, cardsInPlay: Hand) {
        var sum = BigDecimal(0)
        val shoe = game.getStartingShoe()
        val dealer = Hand.fromCard(dealerCard)
        val shoeAfterCardsInPlay = shoe.removeHand(cardsInPlay)
        val shoeAfterDealer = shoeAfterCardsInPlay.removeCard(dealerCard)
        val nextStatesAndProbabilities = shoeAfterDealer.getNextStatesAndProbabilities().shuffled()
        val classicHandCalculator = MultiHandCalculator(game)
        for ((playerCard1, prob2) in nextStatesAndProbabilities) {
            val player1Card = Hand.fromCard(playerCard1)
            val shoeAfterPlayerCard1 = shoeAfterDealer.removeCard(playerCard1)
            val nextNextStatesAndProbabilities = shoeAfterPlayerCard1.getNextStatesAndProbabilities().shuffled()
            for ((playerCard2, prob3) in nextNextStatesAndProbabilities) {
                val player2Cards = player1Card.addCard(playerCard2)
                val shoeAfterPlayerCard2 = shoeAfterPlayerCard1.removeCard(playerCard2)
                println("${player2Cards[0]}${player2Cards[1]}, ${dealer[0]}")
                val (action, bigDecimal) = classicHandCalculator.getBestAction(
                        player2Cards,
                        dealer,
                        shoeAfterPlayerCard2,
                        false,
                        null,
                        cardsInPlay,
                        splitAces = false,
                        insurance = false
                )
                sum = sum.plus(bigDecimal.times(prob2 * prob3))
            }
        }
        println(sum)
    }

    fun getHouseEdge(cardsInPlay: Hand) {
        var sum = BigDecimal(0)
        val startingShoe = game.getStartingShoe()
        val shoe = startingShoe.removeHand(cardsInPlay)
        val nextStatesAndProbabilities = shoe.getNextStatesAndProbabilities()
        val classicHandCalculator = MultiHandCalculator(game)
        for ((dealerCard, prob) in nextStatesAndProbabilities) {
            val dealer = Hand.fromCard(dealerCard)
            val shoeAfterDealer = shoe.removeCard(dealerCard)
            val nextStatesAndProbabilities2 = shoeAfterDealer.getNextStatesAndProbabilities().shuffled()
            for ((playerCard1, prob2) in nextStatesAndProbabilities2) {
                val player1Card = Hand.fromCard(playerCard1)
                val shoeAfterPlayerCard1 = shoeAfterDealer.removeCard(playerCard1)
                val nextStatesAndProbabilities3 = shoeAfterPlayerCard1.getNextStatesAndProbabilities().shuffled()
                for ((playerCard2, prob3) in nextStatesAndProbabilities3) {
                    val player2Cards = player1Card.addCard(playerCard2)
                    val shoeAfterPlayerCard2 = shoeAfterPlayerCard1.removeCard(playerCard2)
                    println("${player2Cards[0]}${player2Cards[1]}, ${dealer[0]}")
                    val (action, expectedUtility) = classicHandCalculator.getBestAction(
                            player2Cards,
                            dealer,
                            shoeAfterPlayerCard2,
                            false,
                            null,
                            cardsInPlay,
                            splitAces = false,
                            insurance = false
                    )
                    sum = sum.plus(expectedUtility.times(prob * prob2 * prob3))
                }
            }
        }
        println(sum)
    }
}


fun main() {
    MultiDatabasePopulator().main(Hand.fromCards(Card.SEVEN, Card.EIGHT, Card.NINE, Card.TEN))
}

