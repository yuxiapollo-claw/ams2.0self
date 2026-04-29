package com.company.ams.request;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
public class RequestRuntimeStateStore {
    private final Map<Long, String> requestStates = new ConcurrentHashMap<>();

    public void save(long requestId, String state) {
        requestStates.put(requestId, state);
    }

    public Optional<String> getCurrentState(long requestId) {
        return Optional.ofNullable(requestStates.get(requestId));
    }

    public String getRequiredCurrentState(long requestId) {
        return getCurrentState(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request " + requestId + " does not exist"));
    }

    public boolean compareAndSet(long requestId, String expectedState, String nextState) {
        AtomicBoolean exists = new AtomicBoolean(false);
        AtomicBoolean updated = new AtomicBoolean(false);
        requestStates.compute(requestId, (id, currentState) -> {
            if (currentState == null) {
                return null;
            }
            exists.set(true);
            if (Objects.equals(currentState, expectedState)) {
                updated.set(true);
                return nextState;
            }
            return currentState;
        });
        if (!exists.get()) {
            throw new IllegalArgumentException("Request " + requestId + " does not exist");
        }
        return updated.get();
    }
}
