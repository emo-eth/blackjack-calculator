enum class Action(val num: Int) {
    HIT(0),
    STAND(1),
    SPLIT(2),
    DOUBLE(3),
    SURRENDER(4),
    INSURANCE(5);

    companion object {
        private val map = Action.values().associateBy(Action::num)
        fun fromInt(type: Int) = map.getOrDefault(type, Action.STAND)
    }
}