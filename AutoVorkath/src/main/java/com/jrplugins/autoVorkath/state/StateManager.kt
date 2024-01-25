//sealed class State {
//    object WalkingToBank : State()
//    object Banking : State()
//    // ... other states
//    object None : State()
//}
//
//internal class StateManager {
//    var currentState: State = State.None
//
//    fun changeState(newState: State) {
//        currentState = newState
//        handleState()
//    }
//
//    private fun handleState() {
//        when (currentState) {
//            is State.WalkingToBank -> handleWalkingToBank()
//            is State.Banking -> handleBanking()
//            // ... handle other states
//            is State.None -> { /* Do nothing or handle default case */ }
//        }
//    }
//
//    private fun handleWalkingToBank() {
//        // Logic for WalkingToBank
//    }
//
//    private fun handleBanking() {
//        // Logic for Banking
//    }
//    // ... other state handlers
//}
