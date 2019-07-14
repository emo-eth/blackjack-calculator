fun main(args: Array<String>) {
    initialize()
    println(args[0].toInt())
    println(Runtime.getRuntime().availableProcessors())
    var threads = IntArray(8)
    7.until(10).toList().forEach{
        println(it)
        threads.asList().parallelStream().forEach{ _ ->
            doForDealerCard(Card.fromByte(it.toByte()))
        }
    }
}

fun doForDealerCard(dealerCard: Card) {
    val shoe = makeShoe(6)
    val dealer = fromCard(dealerCard)
    val shoeAfterDealer = removeCard(dealerCard, shoe)
    val nextStatesAndProbabilities = getNextStatesAndProbabilities(shoeAfterDealer).shuffled()
    for ((playerCard1, prob2) in nextStatesAndProbabilities) {
        val player1Card = fromCard(playerCard1)
        val shoeAfterPlayerCard1 = removeCard(playerCard1, shoeAfterDealer)
        val nextNextStatesAndProbabilities = getNextStatesAndProbabilities(shoeAfterPlayerCard1).shuffled()
        for ((playerCard2, prob3) in nextNextStatesAndProbabilities) {
            val player2Cards = addCard(playerCard2, player1Card)
            val shoeAfterPlayerCard2 = removeCard(playerCard2, shoeAfterPlayerCard1)
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
