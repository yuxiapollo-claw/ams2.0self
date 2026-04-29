package com.company.ams.request;

import com.company.ams.auth.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class RequestService {
    private final RequestStateMachine requestStateMachine;
    private final RequestWorkflowStore requestWorkflowStore;

    public RequestService(
            RequestStateMachine requestStateMachine,
            RequestWorkflowStore requestWorkflowStore) {
        this.requestStateMachine = requestStateMachine;
        this.requestWorkflowStore = requestWorkflowStore;
    }

    public RequestCreateResponse create(CreateRequestCommand command, UserPrincipal principal) {
        String currentStatus = requestStateMachine.initialState();
        return requestWorkflowStore.createRequest(
                command,
                principal.id(),
                principal.userName(),
                currentStatus,
                requestStateMachine.labelFor(currentStatus));
    }

    public RequestListPayload list() {
        return requestWorkflowStore.listRequests();
    }

    public RequestAdvanceResponse advance(long requestId, String action) {
        while (true) {
            String currentState = currentState(requestId);
            String nextState = requestStateMachine.next(currentState, action);
            if (compareAndSet(requestId, currentState, nextState)) {
                return new RequestAdvanceResponse(
                        requestId,
                        nextState,
                        requestStateMachine.labelFor(nextState));
            }
        }
    }

    private String currentState(long requestId) {
        return requestWorkflowStore.getRequiredCurrentState(requestId);
    }

    private boolean compareAndSet(long requestId, String currentState, String nextState) {
        return requestWorkflowStore.compareAndSet(requestId, currentState, nextState);
    }
}
