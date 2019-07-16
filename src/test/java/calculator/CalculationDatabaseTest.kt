package calculator

import calculator.*
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import java.math.BigDecimal

object CalculationDatabaseTest : Spek({
    test("insert and fetch work") {
        initialize()
        val player = fromCards(Card.TEN, Card.TEN, Card.TEN, Card.TEN)
        val dealer = fromCard(Card.ACE)
        val actions = listOf(Pair(Action.STAND, BigDecimal(-1)), Pair(Action.INSURANCE, BigDecimal(-1.5)))
        insertHand(false, null, false, dealer, player, actions)
        val retrievedActions = getHand(false, null, false, dealer, player)
        assertThat(retrievedActions).isNotNull
        assertThat(retrievedActions!!.size).isEqualTo(2)
        deleteHand(player)
        val retrievedDelete = getHand(false, null, false, dealer, player)
        assertThat(retrievedDelete).isNull()

    }
})