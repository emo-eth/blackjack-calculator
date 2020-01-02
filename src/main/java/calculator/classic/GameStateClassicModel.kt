package calculator.classic

import calculator.Action
import calculator.classic.GameStateClassic
import calculator.Hand
import calculator.classic.GameStateClassic.actionDouble
import calculator.classic.GameStateClassic.actionHit
import calculator.classic.GameStateClassic.actionInsurance
import calculator.classic.GameStateClassic.actionSplit
import calculator.classic.GameStateClassic.actionStand
import calculator.classic.GameStateClassic.dealer
import calculator.classic.GameStateClassic.insurance
import calculator.classic.GameStateClassic.player
import calculator.classic.GameStateClassic.split
import calculator.classic.GameStateClassic.splitAces

import calculator.dealer.DealerProbabilities
import calculator.dealer.DealerProbabilitiesModel
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.ds.PGPoolingDataSource
import java.math.BigDecimal
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger


data class MapKey(val playerHandString: String, val dealerHand: String, val split: String?, val splitAces: Boolean, val insurance: Boolean)


object GameStateClassic : Table() {
    val player = varchar("player", 22).index()
    val dealer = char("dealer").index()
    val split = varchar("split", 22)
    val splitAces = bool("split_aces")
    val insurance = bool("insurance")
    val actionHit = double("action_hit").nullable()
    val actionStand = double("action_stand").nullable()
    val actionSplit = double("action_split").nullable()
    val actionDouble = double("action_double").nullable()
    val actionInsurance = double("action_insurance").nullable()
}

class GameStateClassicModel {
    companion object {
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
        private val lock = ReentrantLock()
        private val logger: Logger = Logger.getLogger("CalculationDatabase")
        private val batchInsertMap: ConcurrentMap<MapKey, List<Pair<Action, BigDecimal>>> = ConcurrentHashMap()
        private val cacheMap: MutableMap<MapKey, List<Pair<Action, BigDecimal>>> = mutableMapOf()
        private val MAX_MAP_ENTRIES = 10000


        private fun toUTF8String(hand: Hand?): String {
            if (hand == null) return ""
            return String(hand.toUTF8())
        }


        fun makeMapKey(insurance: Boolean,
                       split: Hand?,
                       splitAces: Boolean,
                       dealer: Hand,
                       player: Hand): MapKey {
            return MapKey(toUTF8String(player), toUTF8String(dealer), toUTF8String(split), splitAces, insurance)
        }
    }


    fun initialize() {

        transaction {
            addLogger(StdOutSqlLogger)

            SchemaUtils.create(GameStateClassic)
            try {
                exec(SchemaUtils.createIndex(Index(listOf(player, dealer, split), false))[0])
            } catch (ex: Exception) {
                println(ex.message)
            }
        }

    }


    fun checkInsertCache(insurance: Boolean, split: Hand?, splitAces: Boolean, dealer: Hand, player: Hand): Boolean {
        val mapKey = makeMapKey(insurance, split, splitAces, dealer, player)
        return mapKey in cacheMap
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


    fun getHand(
            insurance: Boolean,
            split: Hand?,
            splitAces: Boolean,
            dealer: Hand,
            player: Hand
    ): List<Pair<Action, BigDecimal>>? {
        val fetched = cacheMap[makeMapKey(insurance, split, splitAces, dealer, player)]
        if (fetched != null) {
            logger.info("Cache map hit")
            return fetched
        }
        val resultRow = transaction {
            GameStateClassic.select {
                (GameStateClassic.insurance.eq(insurance) and
                        GameStateClassic.split.eq(if (split == null) "" else split.toUTF8().toString(Charset.defaultCharset())) and
                        GameStateClassic.dealer.eq(dealer.toUTF8()[0].toChar()) and
                        GameStateClassic.splitAces.eq(splitAces) and
                        GameStateClassic.player.eq(player.toUTF8().toString(Charset.defaultCharset())))
            }.firstOrNull()
        } ?: return null

        return convertRow(resultRow)

    }

    fun insertHand(
            insurance: Boolean,
            split: Hand?,
            splitAces: Boolean,
            dealer: Hand,
            player: Hand,
            calculations: List<Pair<Action, BigDecimal>>) {

        if (batchInsertMap.size < MAX_MAP_ENTRIES) {
            logger.info("Inserting into map")
            lock.lock()
            val mapKey = makeMapKey(insurance, split, splitAces, dealer, player)
            batchInsertMap[mapKey] = calculations
            cacheMap[mapKey] = calculations
            lock.unlock()
            return
        }

        logger.info("Map reached threshold, batch inserting")


        lock.lock()
        try {
            transaction {
                GameStateClassic.batchInsert(batchInsertMap.entries, true) { entry ->
                    val (mapKey, rowCalculations) = entry
                    val calculationMap = rowCalculations.map {
                        it.first to it.second
                    }.toMap()
                    val (playerString, dealerString, splitString, splitAcesRow, insuranceRow) = mapKey
                    this[GameStateClassic.insurance] = insuranceRow
                    this[GameStateClassic.split] = splitString ?: ""
                    this[GameStateClassic.player] = playerString
                    this[GameStateClassic.dealer] = dealerString[0]
                    this[GameStateClassic.splitAces] = splitAcesRow
                    this[actionHit] = calculationMap[Action.HIT]?.toDouble()
                    this[actionDouble] = calculationMap[Action.DOUBLE]?.toDouble()
                    this[actionSplit] = calculationMap[Action.SPLIT]?.toDouble()
                    this[actionInsurance] = calculationMap[Action.INSURANCE]?.toDouble()
                    this[actionStand] = calculationMap[Action.STAND]?.toDouble()
                }
            }
            batchInsertMap.clear()
        } finally {
            lock.unlock()
        }
        logger.info("Clearing map")


    }

    fun deleteHand(
            player: Hand
    ) {
        transaction {
            GameStateClassic.deleteWhere {
                GameStateClassic.player.eq(player.toUTF8().toString(Charset.defaultCharset()))
            }
        }
    }

    fun flushCache() {
        try {
            transaction {
                GameStateClassic.batchInsert(batchInsertMap.entries, true) { entry ->
                    val (mapKey, rowCalculations) = entry
                    val calculationMap = rowCalculations.map {
                        it.first to it.second
                    }.toMap()
                    val (playerString, dealerString, splitString, splitAcesRow, insuranceRow) = mapKey
                    this[insurance] = insuranceRow
                    this[split] = splitString ?: ""
                    this[player] = playerString
                    this[dealer] = dealerString[0]
                    this[splitAces] = splitAcesRow
                    this[actionHit] = calculationMap[Action.HIT]?.toDouble()
                    this[actionDouble] = calculationMap[Action.DOUBLE]?.toDouble()
                    this[actionSplit] = calculationMap[Action.SPLIT]?.toDouble()
                    this[actionInsurance] = calculationMap[Action.INSURANCE]?.toDouble()
                    this[actionStand] = calculationMap[Action.STAND]?.toDouble()
                }
            }
        } finally {
            batchInsertMap.clear()
            cacheMap.clear()
        }
    }

}




