package calculator


class Hand : Iterable<Byte> {
    companion object {
        fun fromCards(vararg cards: Card): Hand {
            return Hand(*cards)
        }

        fun fromCard(card: Card): Hand {
            return Hand(card)
        }

        fun combineHands(hand: Hand, otherHand: Hand): Hand {
            return Hand(byteArrayOf(*hand.getBytes(), *otherHand.getBytes()))
        }

        fun fromHands(vararg hands: Hand?): Hand {
            if (hands.isEmpty()) return Hand()
            return hands.filterNotNull().reduce { acc, hand -> combineHands(acc, hand)}
        }

        fun fromUtf8(hand: ByteArray): Hand {
            return Hand(hand.map { (it - 0x30).toByte() }.toByteArray())
        }

        fun fromListOfValues(hand: List<Int>): Hand {
            return Hand(hand.map { if (it == 11) 0 else it - 1}.map{it.toByte()}.toByteArray())
        }
    }

    private val hand: ByteArray
    val size: Int
    var isSoftVal: Boolean? = null
    var preferredVal: Int? = null
    var hardVal: Int? = null
    var softVal: Int? = null

    constructor(vararg cards: Card) {
        hand = cards.map { card -> card.num }.toByteArray().sortedArray()
        size = hand.size
    }

    constructor(hand: ByteArray) {
        this.hand = hand
        this.hand.sort()
        size = hand.size
        if (size == 0) {
            throw IllegalArgumentException("Empty bytearray passed to constructor")
        }
    }

    constructor(card: Card) {
        this.hand = byteArrayOf(card.num)
        size = hand.size
    }

    fun addCard(card: Card): Hand {
        val newHand = byteArrayOf(*hand, card.num)
        return Hand(newHand)
    }

    fun isSoft(): Boolean {
        if (isSoftVal == null) {
            isSoftVal = hand[0] == Card.ACE.num
        }
        return isSoftVal as Boolean
    }

    fun getHardValue(): Int {
        if (hardVal == null) {
            hardVal = hand.map { x -> x.toInt() + 1 }.sum()
        }
        return hardVal as Int
    }

    fun getSoftValue(): Int {
        if (softVal == null) {
            if (isSoft()) {
                softVal = 10 + getHardValue()
            } else {
                softVal = getHardValue()
            }
        }
        return softVal as Int
    }

    fun getPreferredValue(): Int {
        if (preferredVal == null) {
            if (isSoft()) {
                val soft = getSoftValue()
                if (soft > 21) {
                    preferredVal = soft - 10
                } else {
                    preferredVal = soft
                }
            } else {
                preferredVal = getHardValue()
            }
        }
        return preferredVal as Int
    }

    override operator fun iterator(): Iterator<Byte> {
        return hand.iterator()
    }

    operator fun get(i: Int): Byte {
        return hand[i]
    }

    fun toUTF8(): ByteArray {
        return hand.map { (it + 0x30).toByte() }.toByteArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hand

        if (!hand.contentEquals(other.hand)) return false

        return true
    }

    override fun hashCode(): Int {
        return hand.contentHashCode()
    }

    private fun getBytes(): ByteArray {
        return hand
    }

}