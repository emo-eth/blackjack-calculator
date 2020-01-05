package calculator

import org.jetbrains.exposed.sql.*
import org.postgresql.ds.PGPoolingDataSource
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger

abstract class AbstractGameStateModel {
    data class MapKey(val playerHandString: String, val dealerHand: String, val split: String?, val splitAces: Boolean, val insurance: Boolean)

    abstract val logger: Logger

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
    val lock = ReentrantLock()
    val batchInsertMap: ConcurrentMap<MapKey, List<Pair<Action, BigDecimal>>> = ConcurrentHashMap()
    val cacheMap: MutableMap<MapKey, List<Pair<Action, BigDecimal>>> = mutableMapOf()
    val MAX_MAP_ENTRIES = 50000

    protected fun toUTF8String(hand: Hand?): String {
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


    abstract fun initialize()


    fun checkInsertCache(insurance: Boolean, split: Hand?, splitAces: Boolean, dealer: Hand, player: Hand): Boolean {
        val mapKey = makeMapKey(insurance, split, splitAces, dealer, player)
        return mapKey in cacheMap
    }
//
//    fun loadFullCache() {
//        transaction {
//            GameStateClassicModel.selectAll().forEach { it ->
//                val key = makeMapKeyFromRow(it)
//                val value = convertRow(it)
//                cacheMap[key] = value
//            }
//        }
//        skipDb = true
//    }
//
//    fun makeMapKeyFromRow(resultRow: ResultRow): MapKey {
//        return makeMapKey(resultRow[insurance], if (resultRow[split] == "") null else Hand.fromUtf8(resultRow[split].toByteArray(Charset.defaultCharset())), resultRow[splitAces], Hand.fromUtf8(resultRow[player].toByteArray(Charset.defaultCharset())), Hand.fromUtf8(resultRow[player].toByteArray(Charset.defaultCharset())))
//    }

    abstract fun convertRow(resultRow: ResultRow): List<Pair<Action, BigDecimal>>


    abstract fun getHand(
            insurance: Boolean,
            split: Hand?,
            splitAces: Boolean,
            dealer: Hand,
            player: Hand
    ): List<Pair<Action, BigDecimal>>?

    abstract fun insertHand(
            insurance: Boolean,
            split: Hand?,
            splitAces: Boolean,
            dealer: Hand,
            player: Hand,
            calculations: List<Pair<Action, BigDecimal>>)

    abstract fun deleteHand(
            player: Hand
    )

    abstract fun flushCache()
}