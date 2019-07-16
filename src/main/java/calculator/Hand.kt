package calculator

typealias Hand = ByteArray

fun addCard(card: Card, hand: Hand): Hand {
    val newHand = byteArrayOf(*hand, card.num)
    newHand.sort()
    return newHand
}

fun fromCard(card: Card): Hand {
    return byteArrayOf(card.num)
}

fun fromCards(vararg cards: Card): Hand {
    return cards.map { card -> card.num }.sorted().toByteArray()
}


fun isSoft(hand: Hand): Boolean {
    return hand[0] == Card.ACE.num
}

fun getHardValue(hand: Hand): Int {
    return hand.map { x -> x.toInt() + 1 }.sum()
}

fun getSoftValue(hand: Hand): Int {
    if (isSoft(hand)) return 10 + getHardValue(hand)
    return getHardValue(hand)
}

fun getPreferredValue(hand: Hand): Int {
    if (isSoft(hand)) {
        val soft = getSoftValue(hand)
        if (soft > 21) {
            return soft - 10
        }
        return soft
    }
    return getHardValue(hand)
}