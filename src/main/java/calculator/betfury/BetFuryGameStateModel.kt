package calculator.betfury

import calculator.AbstractGameStateModel
import calculator.Action
import calculator.Hand
import calculator.betfury.BetFuryGameState.actionDouble
import calculator.betfury.BetFuryGameState.actionHit
import calculator.betfury.BetFuryGameState.actionInsurance
import calculator.betfury.BetFuryGameState.actionSplit
import calculator.betfury.BetFuryGameState.actionStand
import calculator.betfury.BetFuryGameState.dealer
import calculator.betfury.BetFuryGameState.insurance
import calculator.betfury.BetFuryGameState.player
import calculator.betfury.BetFuryGameState.split
import calculator.betfury.BetFuryGameState.splitAces


import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.nio.charset.Charset
import java.util.logging.Logger


object BetFuryGameState : Table() {
    val player = varchar("player", 22).primaryKey(0)
    val dealer = char("dealer").primaryKey(1)
    val split = varchar("split", 22).primaryKey(2)
    val splitAces = bool("split_aces").primaryKey(3)
    val insurance = bool("insurance").primaryKey(4)
    val actionHit = double("action_hit").nullable()
    val actionStand = double("action_stand").nullable()
    val actionSplit = double("action_split").nullable()
    val actionDouble = double("action_double").nullable()
    val actionInsurance = double("action_insurance").nullable()

    init {
        index(false, player)
        index(false, dealer)
        index(false, split)
        index(false, player, dealer, split)
    }
}

object BetFuryGameStateModel : AbstractGameStateModel() {

    override val logger: Logger = Logger.getLogger("CalculationDatabase")


    override fun initialize() {
        transaction {
            addLogger(StdOutSqlLogger)

            SchemaUtils.create(BetFuryGameState)
        }

    }

    override fun convertRow(resultRow: ResultRow): List<Pair<Action, BigDecimal>> {
        return listOf(
                Pair(Action.HIT, resultRow[actionHit]), Pair(Action.STAND, resultRow[actionStand]),
                Pair(Action.SPLIT, resultRow[actionSplit]),
                Pair(Action.DOUBLE, resultRow[actionDouble]),
                Pair(Action.INSURANCE, resultRow[actionInsurance]))
                .filter { it.second != null }
                .sortedBy { -(it.second as Double) }
                .map { entry -> Pair(entry.first, BigDecimal(entry.second as Double)) }
    }


    override fun getHand(
            insurance: Boolean,
            split: Hand?,
            splitAces: Boolean,
            dealer: Hand,
            player: Hand
    ): List<Pair<Action, BigDecimal>>? {
        val mapKey = makeMapKey(insurance, split, splitAces, dealer, player)
        val fetched = cacheMap[mapKey]
        if (fetched != null) {
            logger.info("Cache map hit")
            return fetched
        }
        val resultRow = transaction {
            BetFuryGameState.select {
                (BetFuryGameState.insurance.eq(insurance) and
                        BetFuryGameState.split.eq(if (split == null) "" else split.toUTF8().toString(Charset.defaultCharset())) and
                        BetFuryGameState.dealer.eq(dealer.toUTF8()[0].toChar()) and
                        BetFuryGameState.splitAces.eq(splitAces) and
                        BetFuryGameState.player.eq(player.toUTF8().toString(Charset.defaultCharset())))
            }.firstOrNull()
        } ?: return null

        val result = convertRow(resultRow)
        cacheMap[mapKey] = result
        return result

    }

    override fun insertHand(
            insurance: Boolean,
            split: Hand?,
            splitAces: Boolean,
            dealer: Hand,
            player: Hand,
            calculations: List<Pair<Action, BigDecimal>>) {
        val mapKey = makeMapKey(insurance, split, splitAces, dealer, player)
        cacheMap[mapKey] = calculations

        if (batchInsertMap.size < MAX_MAP_ENTRIES) {
            lock.lock()
            batchInsertMap[mapKey] = calculations
            lock.unlock()
            logger.info("Inserted into insertMap")
            return
        }

        logger.info("Map reached threshold, batch inserting")


        lock.lock()
        try {
            transaction {
                BetFuryGameState.batchInsert(batchInsertMap.entries, true) { entry ->
                    val (mapKey, rowCalculations) = entry
                    val calculationMap = rowCalculations.map {
                        it.first to it.second
                    }.toMap()
                    val (playerString, dealerString, splitString, splitAcesRow, insuranceRow) = mapKey
                    this[BetFuryGameState.insurance] = insuranceRow
                    this[BetFuryGameState.split] = splitString ?: ""
                    this[BetFuryGameState.player] = playerString
                    this[BetFuryGameState.dealer] = dealerString[0]
                    this[BetFuryGameState.splitAces] = splitAcesRow
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

    override fun deleteHand(
            player: Hand
    ) {
        transaction {
            BetFuryGameState.deleteWhere {
                BetFuryGameState.player.eq(player.toUTF8().toString(Charset.defaultCharset()))
            }
        }
    }

    override fun flushCache() {
        try {
            transaction {
                BetFuryGameState.batchInsert(batchInsertMap.entries, true) { entry ->
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




