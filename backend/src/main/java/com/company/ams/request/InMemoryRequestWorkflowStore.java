package com.company.ams.request;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryRequestWorkflowStore implements RequestWorkflowStore {
    private static final String ITEM_PENDING = "PENDING";

    private final AtomicLong idSequence = new AtomicLong(1000);
    private final RequestRuntimeStateStore runtimeStateStore;
    private final Map<Long, RequestListItem> requests = new ConcurrentHashMap<>();

    public InMemoryRequestWorkflowStore() {
        this(new RequestRuntimeStateStore());
    }

    public InMemoryRequestWorkflowStore(RequestRuntimeStateStore runtimeStateStore) {
        this.runtimeStateStore = runtimeStateStore;
    }

    @Override
    public RequestCreateResponse createRequest(
            CreateRequestCommand command,
            Long applicantUserId,
            String applicantUserName,
            String initialState,
            String currentStatusLabel) {
        if (command == null || command.items() == null || command.items().isEmpty()) {
            throw new IllegalArgumentException("items is required");
        }

        long id = idSequence.incrementAndGet();
        runtimeStateStore.save(id, initialState);
        requests.put(id, new RequestListItem(
                id,
                "REQ" + id,
                command.requestType(),
                command.targetAccountName(),
                initialState,
                command.reason(),
                "in-memory"));
        List<RequestWorkflowItem> items = command.items().stream()
                .map(item -> new RequestWorkflowItem(item.roleNodeId(), ITEM_PENDING))
                .toList();
        return new RequestCreateResponse(id, "REQ" + id, initialState, currentStatusLabel, items);
    }

    @Override
    public RequestListPayload listRequests() {
        List<RequestListItem> list = requests.values().stream()
                .sorted((left, right) -> Long.compare(right.id(), left.id()))
                .toList();
        return new RequestListPayload(list, list.size());
    }

    @Override
    public String getRequiredCurrentState(long requestId) {
        return runtimeStateStore.getRequiredCurrentState(requestId);
    }

    @Override
    public boolean compareAndSet(long requestId, String expectedState, String nextState) {
        boolean updated = runtimeStateStore.compareAndSet(requestId, expectedState, nextState);
        if (updated) {
            requests.computeIfPresent(requestId, (ignored, current) -> new RequestListItem(
                    current.id(),
                    current.requestNo(),
                    current.requestType(),
                    current.targetAccountName(),
                    nextState,
                    current.reason(),
                    current.createdAt()));
        }
        return updated;
    }

    @Override
    public boolean submitExecution(long requestId, String expectedState, String nextState, String operatorName) {
        return runtimeStateStore.compareAndSet(requestId, expectedState, nextState);
    }

    public Optional<String> getCurrentState(long requestId) {
        return runtimeStateStore.getCurrentState(requestId);
    }
}
