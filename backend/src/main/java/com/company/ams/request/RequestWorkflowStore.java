package com.company.ams.request;

public interface RequestWorkflowStore {
    RequestListPayload listRequests();

    RequestCreateResponse createRequest(
            CreateRequestCommand command,
            Long applicantUserId,
            String applicantUserName,
            String initialState,
            String currentStatusLabel);

    String getRequiredCurrentState(long requestId);

    boolean compareAndSet(long requestId, String expectedState, String nextState);

    boolean submitExecution(long requestId, String expectedState, String nextState, String operatorName);
}
