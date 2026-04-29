package com.company.ams.request;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RequestStateMachine {
    private static final String INITIAL_STATE = "WAIT_DEPT_MANAGER";

    private static final Map<Transition, String> TRANSITIONS = Map.of(
            new Transition("WAIT_DEPT_MANAGER", "APPROVE"), "WAIT_QA",
            new Transition("WAIT_QA", "APPROVE"), "WAIT_QM",
            new Transition("WAIT_QM", "APPROVE"), "WAIT_QI_EXECUTE",
            new Transition("WAIT_QI_EXECUTE", "EXECUTE_SUCCESS"), "COMPLETED");

    private static final Map<String, String> STATUS_LABELS = Map.of(
            "WAIT_DEPT_MANAGER", "待部门负责人审批",
            "WAIT_QA", "待QA审批",
            "WAIT_QM", "待QM审批",
            "WAIT_QI_EXECUTE", "待QI执行",
            "COMPLETED", "已完成");

    public String initialState() {
        return INITIAL_STATE;
    }

    public String next(String currentState, String action) {
        String nextState = TRANSITIONS.get(new Transition(currentState, action));
        if (nextState == null) {
            throw new IllegalArgumentException(
                    "Invalid request workflow transition: " + currentState + " -> " + action);
        }
        return nextState;
    }

    public String labelFor(String state) {
        return STATUS_LABELS.getOrDefault(state, state);
    }

    private record Transition(String currentState, String action) {}
}
