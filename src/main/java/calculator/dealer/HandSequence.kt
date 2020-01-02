package calculator.dealer


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

val memoizedHands = { x: Int, y: Int -> handMaker(x, y) }.memoize()

object Permutations {
    val allPermutations by lazy {
        val hands = memoizedHands(21, 11).toList()
        val sortedHands = hands.map { it.sortedBy { elem -> -elem } }
        val permutedHands = sortedHands.mapIndexed { index, it ->
            sortedHands.subList(index, sortedHands.size).map { it2 ->
                (it + it2).sortedBy { elem ->
                    -elem
                }
            }
        }.flatten()
        hands.union(permutedHands)
    }

    val doublePermutations: Set<List<Int>> by lazy {
        val mutes = allPermutations.toList()
        println(mutes.size)
        val mutset: MutableSet<List<Int>> = mutableSetOf()
        mutes.forEachIndexed { index, it ->
            println(index)
            mutes.subList(index, mutes.size).forEach { it2 ->
                mutset.add((it + it2).sorted())
            }
            println(mutset.size)
        }
        mutset
    }
}

fun main() {
    println(Permutations.doublePermutations.size)
}