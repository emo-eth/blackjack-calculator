package calculator.platipus

import calculator.Card
import calculator.Hand
import calculator.Shoe

fun main(args: Array<String>) {
    initialize()
    println(args[0].toInt())
    println(Runtime.getRuntime().availableProcessors())
    val threads = IntArray(8)
    0.until(10).toList().forEach{
        println(it)
        threads.asList().parallelStream().forEach{ _ ->
            doForDealerCard(Card.fromByte(it.toByte()))
        }
    }
}

fun doForDealerCard(dealerCard: Card) {
    val shoe = Shoe(6)
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
            getBestAction(
                    player2Cards,
                    dealer,
                    shoeAfterPlayerCard2,
                    false,
                    null,
                    false,
                    false
            )
        }
    }
}
