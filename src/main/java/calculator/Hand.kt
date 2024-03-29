package calculator


class Hand : Iterable<Byte> {
    companion object {
        fun fromCards(vararg cards: Card): Hand {
            return Hand(*cards)
        }

        fun fromCard(card: Card): Hand {
            return Hand(card)
        }

        fun fromUtf8(hand: ByteArray): Hand {
            return Hand(hand.map { (it - 0x30).toByte() }.toByteArray())
        }
    }

    private val hand: ByteArray
    val size: Int

    constructor(vararg cards: Card) {
        hand = cards.map { card -> card.num }.sorted().toByteArray()
        size = hand.size
    }

    private constructor(hand: ByteArray) {
        this.hand = hand
        this.hand.sort()
        size = hand.size
    }

    constructor(card: Card) {
        this.hand = byteArrayOf(card.num)
        size = hand.size
    }


    fun addCard(card: Card): Hand {
        val newHand = byteArrayOf(*hand, card.num)
        newHand.sort()
        return Hand(newHand)
    }

    fun isSoft(): Boolean {
        return hand[0] == Card.ACE.num
    }

    fun getHardValue(): Int {
        return hand.map { x -> x.toInt() + 1 }.sum()
    }

    fun getSoftValue(): Int {
        if (isSoft()) return 10 + getHardValue()
        return getHardValue()
    }

    fun getPreferredValue(): Int {
        if (isSoft()) {
            val soft = getSoftValue()
            if (soft > 21) {
                return soft - 10
            }
            return soft
        }
        return getHardValue()
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

}