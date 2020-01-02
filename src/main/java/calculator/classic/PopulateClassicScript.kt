package calculator.classic

import calculator.Card
import calculator.Hand
import calculator.Shoe
import java.math.BigDecimal


fun main(args: Array<String>) {
    val db = GameStateClassic()
    db.initialize()
    println(args[0].toInt())
    println(Runtime.getRuntime().availableProcessors())
    val threads = IntArray(12)
    0.until(10).toList().forEach{
        println(it)
        threads.asList().parallelStream().forEach{ _ ->
            doForDealerCard(Card.fromByte(it.toByte()))
        }
    }
    db.flushCache()
    getHouseEdge()
}

fun doForDealerCard(dealerCard: Card) {
    var sum = BigDecimal(0)
    val shoe = Shoe(8)
    val dealer = Hand.fromCard(dealerCard)
    val shoeAfterDealer = shoe.removeCard(dealerCard)
    val nextStatesAndProbabilities = shoeAfterDealer.getNextStatesAndProbabilities().shuffled()
    for ((playerCard1, prob2) in nextStatesAndProbabilities) {
        val player1Card = Hand.fromCard(playerCard1)
        val shoeAfterPlayerCard1 = shoeAfterDealer.removeCard(playerCard1)
        val nextNextStatesAndProbabilities = shoeAfterPlayerCard1.getNextStatesAndProbabilities().shuffled()
        for ((playerCard2, prob3) in nextNextStatesAndProbabilities) {
            val player2Cards = player1Card.addCard(playerCard2)
            val shoeAfterPlayerCard2 = shoeAfterPlayerCard1.removeCard(playerCard2)
            println("${player2Cards[0]}${player2Cards[1]}, ${dealer[0]}")
            val (action, bigDecimal) = ClassicHandCalculator.getBestAction(
                    player2Cards,
                    dealer,
                    shoeAfterPlayerCard2,
                    false,
                    null,
                    splitAces = false,
                    insurance = false
            )
            sum = sum.plus(bigDecimal.times(prob2*prob3))
        }
    }
    println(sum)
}

fun getHouseEdge() {
    var sum = BigDecimal(0)
    val shoe = Shoe(8)
    val nextStatesAndProbabilities = shoe.getNextStatesAndProbabilities()
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
                val (action, expectedUtility) = ClassicHandCalculator.getBestAction(
                        player2Cards,
                        dealer,
                        shoeAfterPlayerCard2,
                        false,
                        null,
                        splitAces = false,
                        insurance = false
                )
                sum = sum.plus(expectedUtility.times(prob*prob2 * prob3))
            }
        }
    }
    println(sum)
}
