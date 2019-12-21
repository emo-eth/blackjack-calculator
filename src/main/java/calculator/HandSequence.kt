package calculator


fun handMaker(left: Int, last: Int): Sequence<List<Int>> = sequence {
    if (left == 0) {
        yield(listOf())
    } else {
        for (x in last.downTo(0)) {
            if (x <= left && x != 0) {
                val remaining = handMaker(left - x, x).toList()
                yieldAll(remaining.map { listOf(x) + it })
            } else if (x == 0) {
                yield(listOf())
            }
        }
    }
}


class Memoize<in T, in R, out S>(val seq: (T, R) -> Sequence<S>): (T, R) -> Sequence<S> {

    private val memo = mutableMapOf<Pair<T, R>, List<S>>()

    override fun invoke (x: T, y: R): Sequence<S> = sequence {
        val get = memo[Pair(x, y)]
        if (get != null) {
            yieldAll(get.asSequence())
        } else {
            val compute = seq(x, y)
            memo[Pair(x, y)] = compute.toList()
            yieldAll(compute)
        }
    }
}

fun <T, R, S> ((T, R) -> Sequence<S>).memoize(): (T, R) -> Sequence<S> = Memoize(this)

val memoizedHands = { x: Int, y: Int -> handMaker(x, y)}.memoize()

fun main() {
    val hands = memoizedHands(21, 11).toList()
    val sortedHands = hands.map { it.sortedBy { elem -> -elem }}
    val permutedHands = sortedHands.mapIndexed { index, it ->
        sortedHands.subList(index, sortedHands.size).map {it2 ->
             (it + it2).sortedBy {elem -> -elem
            }
        }
    }.flatten()
    println(permutedHands.size)
    val union: Set<List<Int>> = hands.union(permutedHands)
    println(union.size)
}