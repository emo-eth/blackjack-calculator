package calculator.classic

import calculator.Action
import calculator.Hand
import calculator.util.LRUDBCache
import calculator.classic.MultiClassicGameState.actionDouble
import calculator.classic.MultiClassicGameState.actionHit
import calculator.classic.MultiClassicGameState.actionInsurance
import calculator.classic.MultiClassicGameState.actionSplit
import calculator.classic.MultiClassicGameState.actionStand
import calculator.classic.MultiClassicGameState.dealer
import calculator.classic.MultiClassicGameState.insurance
import calculator.classic.MultiClassicGameState.player
import calculator.classic.MultiClassicGameState.split
import calculator.classic.MultiClassicGameState.cardsInPlay
import calculator.classic.MultiClassicGameState.numSplits
import calculator.classic.MultiClassicGameState.splitAces

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.ds.PGPoolingDataSource
import java.math.BigDecimal
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger

object MultiClassicGameState : Table() {
    val player = varchar("player", 22)
    val dealer = char("dealer")
    val split = varchar("split", 22)
    val numSplits = char("num_splits")
    val cardsInPlay = varchar("cards_in_play", 22)
    val splitAces = bool("split_aces")
    val insurance = bool("insurance")
    val actionHit = double("action_hit").nullable()
    val actionStand = double("action_stand").nullable()
    val actionSplit = double("action_split").nullable()
    val actionDouble = double("action_double").nullable()
    val actionInsurance = double("action_insurance").nullable()

    init {
//        index(false, player)
//        index(false, dealer)
//        index(false, split)
        index(false, player, dealer, cardsInPlay)
    }
}

object MultiClassicGameStateModel {
    val db = {
        val dataSource = PGPoolingDataSource()
        dataSource.databaseName = System.getenv("POSTGRES_DATABASE")
        dataSource.user = System.getenv("POSTGRES_USER")
        dataSource.portNumber = System.getenv("POSTGRES_PORT").toInt()
        dataSource.serverName = System.getenv("POSTGRES_HOST")
        dataSource.password = System.getenv("POSTGRES_PASSWORD")
        dataSource.maxConnections = 300
        Database.connect(dataSource)
    }()

    data class MultiMapKey(val playerHandString: String, val dealerHand: String, val split: String?, val cardsInPlay: String?, val splitAces: Boolean, val insurance: Boolean, val numSplits: Int)

    val logger: Logger = Logger.getLogger("MultiClassicGameStateModel")
    val multiBatchInsertMap: ConcurrentMap<MultiMapKey, List<Pair<Action, BigDecimal>>> = ConcurrentHashMap()
    val lruCache: LRUDBCache<MultiMapKey, List<Pair<Action, BigDecimal>>> = LRUDBCache(150000)
    val lock = ReentrantLock()
    val MAX_MAP_ENTRIES = 50000


    private fun toUTF8String(hand: Hand?): String {
        if (hand == null) return ""
        return String(hand.toUTF8())
    }

    fun initialize() {
        transaction {
            addLogger(StdOutSqlLogger)

            SchemaUtils.create(MultiClassicGameState)
        }
    }

    fun convertRow(resultRow: ResultRow): List<Pair<Action, BigDecimal>> {
        return listOf(
                Pair(Action.HIT, resultRow[actionHit]), Pair(Action.STAND, resultRow[actionStand]),
                Pair(Action.SPLIT, resultRow[actionSplit]),
                Pair(Action.DOUBLE, resultRow[actionDouble]),
                Pair(Action.INSURANCE, resultRow[actionInsurance]))
                .filter { it.second != null }
                .sortedBy { -(it.second as Double) }
                .map { entry -> Pair(entry.first, BigDecimal(entry.second as Double)) }
    }

    fun makeMultiMapKey(insurance: Boolean,
                   split: Hand?,
                   cardsInPlay: Hand?,
                   splitAces: Boolean,
                   dealer: Hand,
                   player: Hand,
                    numSplits: Int): MultiMapKey {
        return MultiMapKey(toUTF8String(player), toUTF8String(dealer), toUTF8String(split), toUTF8String(cardsInPlay), splitAces, insurance, numSplits)
    }



    fun getHand(
            insurance: Boolean,
            split: Hand?,
            cardsInPlay: Hand?,
            splitAces: Boolean,
            dealer: Hand,
            player: Hand,
            numSplits: Int
    ): List<Pair<Action, BigDecimal>>? {
        val mapKey = makeMultiMapKey(insurance, split, cardsInPlay, splitAces, dealer, player, numSplits)
        val fetched = lruCache[mapKey]
        if (fetched != null) {
//            logger.info("Cache map hit")
            return fetched
        }
        return null
//        val resultRow = transaction {
//            MultiClassicGameState.select {
//                (MultiClassicGameState.insurance.eq(insurance) and
//                        MultiClassicGameState.split.eq(split?.toUTF8()?.toString(Charset.defaultCharset()) ?: "") and
//                        MultiClassicGameState.cardsInPlay.eq(cardsInPlay?.toUTF8()?.toString(Charset.defaultCharset()) ?: "") and
//                        MultiClassicGameState.dealer.eq(dealer.toUTF8()[0].toChar()) and
//                        MultiClassicGameState.splitAces.eq(splitAces) and
//                        MultiClassicGameState.numSplits.eq(numSplits.toChar() + 1) and
//                        MultiClassicGameState.player.eq(player.toUTF8().toString(Charset.defaultCharset())))
//            }.firstOrNull()
//        } ?: return null
//
//        val result = convertRow(resultRow)
//        lruCache[mapKey] = result
//        return result

    }

    fun insertHand(
            insurance: Boolean,
            split: Hand?,
            cardsInPlay: Hand?,
            splitAces: Boolean,
            dealer: Hand,
            player: Hand,
            numSplits: Int,
            calculations: List<Pair<Action, BigDecimal>>) {
        val mapKey = makeMultiMapKey(insurance, split, cardsInPlay, splitAces, dealer, player, numSplits)
        lruCache[mapKey] = calculations
        return
//        if (multiBatchInsertMap.size < MAX_MAP_ENTRIES) {
//            lock.lock()
//            multiBatchInsertMap[mapKey] = calculations
//            lock.unlock()
////            logger.info("Inserted into insertMap")
//            return
//        }
//
//        logger.info("Map reached threshold, batch inserting")
//
//
//        lock.lock()
//        try {
//            transaction {
//                MultiClassicGameState.batchInsert(multiBatchInsertMap.entries, true) { entry ->
//                    val (mapKey, rowCalculations) = entry
//                    val calculationMap = rowCalculations.map {
//                        it.first to it.second
//                    }.toMap()
//                    val (playerString, dealerString, splitString, cardsInPlayString, splitAcesRow, insuranceRow, rowNumSplits) = mapKey
//                    this[MultiClassicGameState.insurance] = insuranceRow
//                    this[MultiClassicGameState.split] = splitString ?: ""
//                    this[MultiClassicGameState.cardsInPlay] = cardsInPlayString ?: ""
//                    this[MultiClassicGameState.player] = playerString
//                    this[MultiClassicGameState.dealer] = dealerString[0]
//                    this[MultiClassicGameState.splitAces] = splitAcesRow
//                    this[MultiClassicGameState.numSplits] = rowNumSplits.toChar() + 1
//                    this[actionHit] = calculationMap[Action.HIT]?.toDouble()
//                    this[actionDouble] = calculationMap[Action.DOUBLE]?.toDouble()
//                    this[actionSplit] = calculationMap[Action.SPLIT]?.toDouble()
//                    this[actionInsurance] = calculationMap[Action.INSURANCE]?.toDouble()
//                    this[actionStand] = calculationMap[Action.STAND]?.toDouble()
//                }
//            }
//            multiBatchInsertMap.clear()
//        } finally {
//            lock.unlock()
//        }
//        logger.info("Clearing map")


    }

    fun deleteHand(
            player: Hand
    ) {
        transaction {
            MultiClassicGameState.deleteWhere {
                MultiClassicGameState.player.eq(player.toUTF8().toString(Charset.defaultCharset()))
            }
        }
    }

    fun flushCache() {
        try {
            transaction {
                MultiClassicGameState.batchInsert(multiBatchInsertMap.entries, true) { entry ->
                    val (mapKey, rowCalculations) = entry
                    val calculationMap = rowCalculations.map {
                        it.first to it.second
                    }.toMap()
                    val (playerString, dealerString, splitString, cardsInPlayString, splitAcesRow, insuranceRow, numSplitsRow) = mapKey
                    this[insurance] = insuranceRow
                    this[split] = splitString ?: ""
                    this[player] = playerString
                    this[dealer] = dealerString[0]
                    this[cardsInPlay] = cardsInPlayString ?: ""
                    this[splitAces] = splitAcesRow
                    this[numSplits] = numSplitsRow.toChar() + 1
                    this[actionHit] = calculationMap[Action.HIT]?.toDouble()
                    this[actionDouble] = calculationMap[Action.DOUBLE]?.toDouble()
                    this[actionSplit] = calculationMap[Action.SPLIT]?.toDouble()
                    this[actionInsurance] = calculationMap[Action.INSURANCE]?.toDouble()
                    this[actionStand] = calculationMap[Action.STAND]?.toDouble()
                }
            }
        } finally {
            multiBatchInsertMap.clear()
        }
    }

}