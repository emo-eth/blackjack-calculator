import java.math.BigDecimal
import java.math.RoundingMode

typealias Shoe = Array<Short>

enum class Card(val num: Byte) {
    ACE(0),
    TWO(1),
    THREE(2),
    FOUR(3),
    FIVE(4),
    SIX(5),
    SEVEN(6),
    EIGHT(7),
    NINE(8),
    TEN(9);

    companion object {
        private val map = Card.values().associateBy(Card::num)
        fun fromByte(type: Byte) = map.getOrDefault(type, Card.ACE)
    }

}

fun getNextStatesAndProbabilities(
        shoe: Shoe
): List<Pair<Card, BigDecimal>> {
    var count = BigDecimal(0)
    val valueProbs: MutableList<Pair<Card, BigDecimal>> = mutableListOf()
    for (i in 0.until(shoe.size)) {
        val numCards = shoe[i].toInt()
        if (numCards == 0) continue
        count = count.plus(BigDecimal(numCards))
        valueProbs.add(Pair(Card.fromByte(i.toByte()), BigDecimal(numCards)))
    }
    return valueProbs.map { entry ->
        Pair(entry.first, entry.second.divide(count, 32, RoundingMode.HALF_UP))
    }
}

fun makeShoe(numDecks: Int): Shoe {
    return Array(10) { x ->
        if (x == 9) {
            (16 * numDecks).toShort()
        } else {
            (4 * numDecks).toShort()
        }
    }
}

fun removeCard(card: Card, shoe: Shoe): Shoe {
    if (shoe[card.num.toInt()] == 0.toShort()) {
        throw Error("Cannot remove empty card");
    }
    val newShoe = shoe.clone()
    newShoe[card.num.toInt()] = (newShoe[card.num.toInt()] - 1).toShort()
    return newShoe
}


fun fromHands(numDecks: Int, vararg hands: Hand): Shoe {
    var shoe = makeShoe(numDecks);
    for (hand in hands) {
        for (card in hand) {
            shoe = removeCard(Card.fromByte(card), shoe);
        }
    }
    return shoe;
}

fun fromCounts(vararg counts: Pair<Card, Int>): Shoe {
    val shoe = makeShoe(0)
    for ((card, count) in counts) {
        shoe[card.num.toInt()] = count.toShort()
    }
    return shoe
}
