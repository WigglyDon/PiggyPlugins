package com.wigglydonplugins.AutoVardorvis.state;

import com.wigglydonplugins.AutoVardorvis.state.botStates.FightingState;
import com.wigglydonplugins.AutoVardorvis.state.botStates.TestingState;
import com.wigglydonplugins.AutoVardorvis.AutoVardorvisPlugin.MainClassContext;

public class StateHandler {
    public enum State {
        TESTING,
        FIGHTING,
    }

    public static void handleState(State state, MainClassContext context) {
        switch (state) {
            case TESTING:
                TestingState.execute();
                break;
            case FIGHTING:
                FightingState.execute(context);
                break;

            default:
                System.out.println("Unknown state!");
        }
    }
}
