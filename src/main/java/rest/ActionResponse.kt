package rest

import calculator.Action

data class ActionResponse(val action: Action, val utility: Double)