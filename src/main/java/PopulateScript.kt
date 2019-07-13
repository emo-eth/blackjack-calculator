


fun doForDealerCard(card: Card, reverse:Boolean) {
    val shoe = makeShoe(6);
    val dealer = fromCard(card);
    val shoeAfterDealer = removeCard(card, shoe);
    val nextStatesAndProbabilities = getNextStatesAndProbabilities(shoeAfterDealer);
//    if (reverse) nextStatesAndProbabilities.reverse();
    for ((card2, prob2) in nextStatesAndProbabilities) {
        val player1Card = fromCard(card2);
        val shoeAfterPlayerCard1 = removeCard(card, shoeAfterDealer);
        val nextNextStatesAndProbabilities = getNextStatesAndProbabilities(shoeAfterPlayerCard1);
//        if (reverse) nextNextStatesAndProbabilities.reverse();
        for ((card3, prob3) in nextNextStatesAndProbabilities) {
        val player2Cards = addCard(card3, player1Card);
        val shoeAfterPlayerCard2 = removeCard(card3, shoeAfterPlayerCard1);
        println(listOf(player2Cards, dealer))
        getBestAction(
                player2Cards,
        dealer,
        shoeAfterPlayerCard2,
        false,
        null,
        false,
        false
        );
    }
    }
//    await pool.end();
}
//
//async function populate() {
//    console.log(+process.argv[2], +process.argv[3]>0);
//    val reverse = +process.argv[3] > 0;
//    doForDealerCard(+process.argv[2], reverse);
//}
//
//populate().catch(err => {
//    console.log(err);
//});
