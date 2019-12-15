package calculator

import java.math.BigDecimal
import java.math.RoundingMode

//typealias Shoe = Array<Short>

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
        private val map = values().associateBy(Card::num)
        fun fromByte(type: Byte) = map.getOrDefault(type, ACE)
    }

}

class Shoe {
    companion object {

        fun fromHands(numDecks: Int, vararg hands: Hand): Shoe {
            var shoe = Shoe(numDecks)
            for (hand in hands) {
                for (card in hand) {
                    shoe = shoe.removeCard(Card.fromByte(card))
                }
            }
            return shoe
        }

        fun fromCounts(vararg counts: Pair<Card, Int>): Shoe {
            val shoe = Shoe(0)
            for ((card, count) in counts) {
                shoe[card.num.toInt()] = count.toShort()
            }
            return shoe
        }
    }

    val size = 10

    private val shoe: Array<Short>

    constructor(shoe: Array<Short>) {
        this.shoe = shoe
    }

    constructor(numDecks: Int) {
        this.shoe = Array(10) { x ->
            if (x == 9) {
                (16 * numDecks).toShort()
            } else {
                (4 * numDecks).toShort()
            }
        }
    }


    operator fun get(i: Int): Short {
        return shoe[i]
    }

    private operator fun set(i: Int, v: Short) {
        shoe[i] = v
    }

    fun clone(): Shoe {
        return Shoe(shoe.clone())
    }

    fun getNextStatesAndProbabilities(): List<Pair<Card, BigDecimal>> {
        var count = BigDecimal(0)
        val valueProbs: MutableList<Pair<Card, BigDecimal>> = mutableListOf()
        for (i in 0.until(size)) {
            val numCards = shoe[i].toInt()
            if (numCards == 0) continue
            count = count.plus(BigDecimal(numCards))
            valueProbs.add(Pair(Card.fromByte(i.toByte()), BigDecimal(numCards)))
        }
        return valueProbs.map { entry ->
            Pair(entry.first, entry.second.divide(count, 32, RoundingMode.HALF_UP))
        }
    }

    fun removeCard(card: Card): Shoe {
        if (shoe[card.num.toInt()] == 0.toShort()) {
            throw Error("Cannot remove empty card")
        }
        val newShoe = this.clone()
        newShoe[card.num.toInt()] = (newShoe[card.num.toInt()] - 1).toShort()
        return newShoe
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Shoe

        if (!shoe.contentEquals(other.shoe)) return false

        return true
    }

    override fun hashCode(): Int {
        return shoe.contentHashCode()
    }


}
