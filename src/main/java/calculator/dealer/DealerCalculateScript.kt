package calculator.dealer

import calculator.Card
import calculator.Hand
import calculator.Shoe

fun main(args: Array<String>) {
    val db = DealerProbabilitiesModel
//    db.initialize()
    println(Permutations.allPermutations.size)

    println(Runtime.getRuntime().availableProcessors())
    val threads = IntArray(8)
    db.loadInsertCacheSet()
    0.until(10).forEach { dealerVal ->
        val dealerCard = Card.fromByte(dealerVal.toByte())
        val dealerHand = Hand.fromCard(dealerCard)
        Permutations.allPermutations.parallelStream().forEach permute@{
            val hand = Hand.fromListOfValues(it)
            if (db.checkInsertCache(hand, dealerHand)) {
//                println("match")
                return@permute
            }
            val shoe: Shoe
            try {
                shoe = Shoe.fromHands(8, hand, dealerHand)
            } catch (ex: Error) {
                return@permute
            }
            calculateAndInsertDealerProbs(hand, dealerCard, shoe)
        }

    }
    db.flushCache()
}