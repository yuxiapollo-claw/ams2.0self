package com.company.ams.execution;

import com.company.ams.request.RequestStateMachine;
import com.company.ams.request.RequestWorkflowStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExecutionService {
    private static final String EXECUTION_PENDING_STATE = "WAIT_QI_EXECUTE";
    private static final String EXECUTION_SUCCESS_ACTION = "EXECUTE_SUCCESS";

    private final RequestStateMachine requestStateMachine;
    private final RequestWorkflowStore requestWorkflowStore;

    public ExecutionService(
            RequestStateMachine requestStateMachine,
            RequestWorkflowStore requestWorkflowStore) {
        this.requestStateMachine = requestStateMachine;
        this.requestWorkflowStore = requestWorkflowStore;
    }

    public ExecutionSubmitResponse submit(Long requestId) {
        return submit(requestId, "SYSTEM");
    }

    public ExecutionSubmitResponse submit(Long requestId, String operatorName) {
        while (true) {
            String currentState = currentState(requestId);
            if (!EXECUTION_PENDING_STATE.equals(currentState)) {
                throw new IllegalStateException(
                        "Request " + requestId + " is not executable from state " + currentState);
            }

            String nextState = requestStateMachine.next(currentState, EXECUTION_SUCCESS_ACTION);
            if (submitTransition(requestId, currentState, nextState, operatorName)) {
                return new ExecutionSubmitResponse(requestId, nextState);
            }
        }
    }

    private String currentState(long requestId) {
        return requestWorkflowStore.getRequiredCurrentState(requestId);
    }

    private boolean submitTransition(long requestId, String currentState, String nextState, String operatorName) {
        return requestWorkflowStore.submitExecution(requestId, currentState, nextState, operatorName);
    }
}
