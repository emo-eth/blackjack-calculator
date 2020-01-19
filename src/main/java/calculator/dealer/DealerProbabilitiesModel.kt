package calculator.dealer

import calculator.Hand
import calculator.dealer.DealerProbabilities.dealer
import calculator.dealer.DealerProbabilities.player
import calculator.util.LRUDBCache
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.ds.PGPoolingDataSource
import java.lang.RuntimeException
import java.math.BigDecimal
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger


object DealerProbabilities : Table() {
    val player = varchar("player", 40).index()
    val dealer = char("dealer").index()
    val _17 = double("17").nullable()
    val _18 = double("18").nullable()
    val _19 = double("19").nullable()
    val _20 = double("20").nullable()
    val _21 = double("21").nullable()
    val _22 = double("22").nullable()
}

object DealerProbabilitiesModel {
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
    private val batchInsertMap: ConcurrentMap<Pair<String, String>, Map<Int, BigDecimal>> = ConcurrentHashMap()
    private val insertCacheSet: MutableSet<Pair<String, String>> = mutableSetOf()
    private val lruCacheMap: LRUDBCache<Pair<String, String>, Map<Int, BigDecimal>> = LRUDBCache(150000)
    private val MAX_MAP_ENTRIES = 5000




    fun initialize() {
        transaction {
            addLogger(StdOutSqlLogger)

            SchemaUtils.create(DealerProbabilities)
            try {
                exec(SchemaUtils.createIndex(Index(listOf(player, dealer), true))[0])
            } catch (ex: Exception) {
                println(ex.message)
            }
        }

    }

    fun loadInsertCacheSet() {
        transaction {
            DealerProbabilities.selectAll().forEach {
                val player = it[DealerProbabilities.player]
                val dealer = it [DealerProbabilities.dealer]
                val mapKey: Pair<String, String> = Pair(player, dealer.toString())
//                insertCacheSet.add(mapKey)
            }
        }
    }

    fun loadFullCacheMap() {
        transaction {
            DealerProbabilities.selectAll().forEach {
                val player = it[DealerProbabilities.player]
                val dealer = it [DealerProbabilities.dealer]
                val mapKey: Pair<String, String> = Pair(player, dealer.toString())
                lruCacheMap[mapKey] = mapOf(Pair(17, it[DealerProbabilities._17]?.toDouble()),
                        Pair(18, it[DealerProbabilities._18]?.toDouble()),
                        Pair(19, it[DealerProbabilities._19]?.toDouble()),
                        Pair(20, it[DealerProbabilities._20]?.toDouble()),
                        Pair(21, it[DealerProbabilities._21]?.toDouble()),
                        Pair(22, it[DealerProbabilities._22]?.toDouble())).map { entry ->
                    entry.key to BigDecimal(entry.value as Double)
                }.toMap()
            }
        }
    }

    fun checkInsertCache(player: Hand, dealer: Hand): Boolean {
        return makeMapKey(player, dealer) in insertCacheSet
    }

    fun flushCache() {
        try {
            transaction {
                DealerProbabilities.batchInsert(batchInsertMap.entries, true) { entry ->
                    val (playerDealer, dealerProbabilities) = entry
                    val (player, dealerCard) = playerDealer
                    this[DealerProbabilities.player] = player
                    this[DealerProbabilities.dealer] = dealerCard[0]
                    this[DealerProbabilities._17] = dealerProbabilities[17]?.toDouble()
                    this[DealerProbabilities._18] = dealerProbabilities[18]?.toDouble()
                    this[DealerProbabilities._19] = dealerProbabilities[19]?.toDouble()
                    this[DealerProbabilities._20] = dealerProbabilities[20]?.toDouble()
                    this[DealerProbabilities._21] = dealerProbabilities[21]?.toDouble()
                    this[DealerProbabilities._22] = dealerProbabilities[22]?.toDouble()
                }
            }
        } finally {
            batchInsertMap.clear()
        }
    }

    private fun toUTF8String(hand: Hand?): String {
        if (hand == null) return ""
        return String(hand.toUTF8())
    }


    private fun makeMapKey(hand: Hand, dealer: Hand): Pair<String, String> {
        return Pair(toUTF8String(hand), toUTF8String(dealer))
    }

    fun getProbabilities(
            player: Hand,
            dealer: Hand
    ): Map<Int, BigDecimal> {
        val fetched = lruCacheMap[makeMapKey(player, dealer)]
        if (fetched != null) {
            logger.info("Cache map hit")
            return fetched
        }
        val resultRow = transaction {
            DealerProbabilities.select {
                (DealerProbabilities.dealer.eq(dealer.toUTF8()[0].toChar()) and
                        DealerProbabilities.player.eq(player.toUTF8().toString(Charset.defaultCharset())))
            }.firstOrNull()
        } ?: throw RuntimeException()

        return mapOf(Pair(17, resultRow[DealerProbabilities._17]?.toDouble()),
                Pair(18, resultRow[DealerProbabilities._18]?.toDouble()),
                Pair(19, resultRow[DealerProbabilities._19]?.toDouble()),
                Pair(20, resultRow[DealerProbabilities._20]?.toDouble()),
                Pair(21, resultRow[DealerProbabilities._21]?.toDouble()),
                Pair(22, resultRow[DealerProbabilities._22]?.toDouble())).map { entry ->
            entry.key to BigDecimal(entry.value as Double)
        }.toMap()
    }

    fun getProbabilitiesIfExist(
            player: Hand,
            dealer: Hand
    ): Map<Int, BigDecimal>? {
        val mapKey = makeMapKey(player, dealer)
        val fetched = lruCacheMap[mapKey]
        if (fetched != null) {
            logger.info("Cache map hit")
            return fetched
        }
        val resultRow = transaction {
            DealerProbabilities.select {
                (DealerProbabilities.dealer.eq(dealer.toUTF8()[0].toChar()) and
                        DealerProbabilities.player.eq(player.toUTF8().toString(Charset.defaultCharset())))
            }.firstOrNull()
        } ?: return null

        val returnValue = mapOf(Pair(17, resultRow[DealerProbabilities._17]?.toDouble()),
                Pair(18, resultRow[DealerProbabilities._18]?.toDouble()),
                Pair(19, resultRow[DealerProbabilities._19]?.toDouble()),
                Pair(20, resultRow[DealerProbabilities._20]?.toDouble()),
                Pair(21, resultRow[DealerProbabilities._21]?.toDouble()),
                Pair(22, resultRow[DealerProbabilities._22]?.toDouble())).map { entry ->
            entry.key to BigDecimal(entry.value as Double)
        }.toMap()
        lruCacheMap[mapKey] = returnValue

        return returnValue
    }

    fun insertHand(
            player: Hand,
            dealer: Hand,
            calculations: Map<Int, BigDecimal>) {
        val mapKey = makeMapKey(player, dealer)
        lruCacheMap[makeMapKey(player, dealer)] = calculations
        if (batchInsertMap.size < MAX_MAP_ENTRIES && MAX_MAP_ENTRIES != 0) {
            logger.info("Inserting into map size ${batchInsertMap.size}")
            lock.lock()
            batchInsertMap[mapKey] = calculations
            lock.unlock()
            return
        }

        logger.info("Map reached threshold, batch inserting")

        lock.lock()

        try {
            transaction {
                logger.info("begin transaction")

                DealerProbabilities.batchInsert(batchInsertMap.entries, true) { entry ->
                    val (playerDealer, dealerProbabilities) = entry
                    val (playerString, dealerCard) = playerDealer
                    this[DealerProbabilities.player] = playerString
                    this[DealerProbabilities.dealer] = dealerCard[0]
                    this[DealerProbabilities._17] = dealerProbabilities[17]?.toDouble()
                    this[DealerProbabilities._18] = dealerProbabilities[18]?.toDouble()
                    this[DealerProbabilities._19] = dealerProbabilities[19]?.toDouble()
                    this[DealerProbabilities._20] = dealerProbabilities[20]?.toDouble()
                    this[DealerProbabilities._21] = dealerProbabilities[21]?.toDouble()
                    this[DealerProbabilities._22] = dealerProbabilities[22]?.toDouble()
                }
            }
            batchInsertMap.clear()
        } finally {
            lock.unlock()
        }

        logger.info("Cleared map")
    }

    fun deleteHand(
            player: Hand
    ) {
        transaction {
            DealerProbabilities.deleteWhere {
                DealerProbabilities.player.eq(player.toUTF8().toString(Charset.defaultCharset()))
            }
        }
    }


}

