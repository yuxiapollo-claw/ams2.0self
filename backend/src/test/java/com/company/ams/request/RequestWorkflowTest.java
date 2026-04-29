package com.company.ams.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ams.auth.UserPrincipal;
import com.company.ams.execution.ExecutionService;
import com.company.ams.execution.ExecutionSubmitResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class RequestWorkflowTest {
    private static final UserPrincipal APPLICANT = new UserPrincipal(1L, "admin", "System Admin", List.of("SYS_ADMIN"));

    @Test
    void transitionsFromDeptToQaToQmToQiToCompleted() {
        RequestStateMachine machine = new RequestStateMachine();
        assertThat(machine.next("WAIT_DEPT_MANAGER", "APPROVE")).isEqualTo("WAIT_QA");
        assertThat(machine.next("WAIT_QA", "APPROVE")).isEqualTo("WAIT_QM");
        assertThat(machine.next("WAIT_QM", "APPROVE")).isEqualTo("WAIT_QI_EXECUTE");
        assertThat(machine.next("WAIT_QI_EXECUTE", "EXECUTE_SUCCESS")).isEqualTo("COMPLETED");
    }

    @Test
    void rejectsInvalidTransition() {
        RequestStateMachine machine = new RequestStateMachine();

        assertThatThrownBy(() -> machine.next("WAIT_DEPT_MANAGER", "EXECUTE_SUCCESS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WAIT_DEPT_MANAGER")
                .hasMessageContaining("EXECUTE_SUCCESS");
    }

    @Test
    void createReturnsInitialWorkflowPayload() {
        RequestStateMachine machine = new RequestStateMachine();
        InMemoryRequestWorkflowStore workflowStore = new InMemoryRequestWorkflowStore();
        RequestService requestService = new RequestService(machine, workflowStore);

        RequestCreateResponse response = requestService.create(sampleCommand(), APPLICANT);

        assertThat(response.currentStatus()).isEqualTo("WAIT_DEPT_MANAGER");
        assertThat(response.currentStatusLabel()).isEqualTo("待部门负责人审批");
        assertThat(workflowStore.getCurrentState(response.id())).hasValue("WAIT_DEPT_MANAGER");
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.roleNodeId()).isEqualTo(300L);
            assertThat(item.itemStatus()).isEqualTo("PENDING");
        });
    }

    @Test
    void executionSubmitAdvancesWorkflowForExecutableRequest() {
        RequestStateMachine machine = new RequestStateMachine();
        InMemoryRequestWorkflowStore workflowStore = new InMemoryRequestWorkflowStore();
        RequestService requestService = new RequestService(machine, workflowStore);
        ExecutionService executionService = new ExecutionService(machine, workflowStore);
        RequestCreateResponse created = requestService.create(sampleCommand(), APPLICANT);

        requestService.advance(created.id(), "APPROVE");
        requestService.advance(created.id(), "APPROVE");
        requestService.advance(created.id(), "APPROVE");

        ExecutionSubmitResponse response = executionService.submit(created.id());

        assertThat(response.requestId()).isEqualTo(created.id());
        assertThat(response.currentStatus()).isEqualTo("COMPLETED");
        assertThat(workflowStore.getCurrentState(created.id())).hasValue("COMPLETED");
    }

    @Test
    void executionSubmitFailsForMissingRequest() {
        RequestStateMachine machine = new RequestStateMachine();
        ExecutionService executionService =
                new ExecutionService(machine, new InMemoryRequestWorkflowStore());

        assertThatThrownBy(() -> executionService.submit(9999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void executionSubmitFailsForNonExecutableRequest() {
        RequestStateMachine machine = new RequestStateMachine();
        InMemoryRequestWorkflowStore workflowStore = new InMemoryRequestWorkflowStore();
        RequestService requestService = new RequestService(machine, workflowStore);
        ExecutionService executionService = new ExecutionService(machine, workflowStore);
        RequestCreateResponse created = requestService.create(sampleCommand(), APPLICANT);

        assertThatThrownBy(() -> executionService.submit(created.id()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WAIT_DEPT_MANAGER");
    }

    @Test
    void executionSubmitAllowsOnlyOneWinnerUnderConcurrentDoubleSubmit() throws Exception {
        RequestStateMachine machine = new RequestStateMachine();
        InMemoryRequestWorkflowStore workflowStore = new InMemoryRequestWorkflowStore();
        RequestService requestService = new RequestService(machine, workflowStore);
        ExecutionService executionService = new ExecutionService(machine, workflowStore);
        RequestCreateResponse created = requestService.create(sampleCommand(), APPLICANT);
        requestService.advance(created.id(), "APPROVE");
        requestService.advance(created.id(), "APPROVE");
        requestService.advance(created.id(), "APPROVE");

        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<String>> tasks = List.of(
                    () -> submitAfterGate(executionService, created.id(), startGate),
                    () -> submitAfterGate(executionService, created.id(), startGate));

            List<Future<String>> futures = new ArrayList<>();
            for (Callable<String> task : tasks) {
                futures.add(executor.submit(task));
            }
            startGate.countDown();

            int successCount = 0;
            int illegalStateCount = 0;
            for (Future<String> future : futures) {
                String outcome = future.get();
                if ("SUCCESS".equals(outcome)) {
                    successCount++;
                }
                if ("ILLEGAL_STATE".equals(outcome)) {
                    illegalStateCount++;
                }
            }

            assertThat(successCount).isEqualTo(1);
            assertThat(illegalStateCount).isEqualTo(1);
            assertThat(workflowStore.getCurrentState(created.id())).hasValue("COMPLETED");
        } finally {
            executor.shutdownNow();
        }
    }

    private static CreateRequestCommand sampleCommand() {
        return new CreateRequestCommand(
                "ROLE_ADD",
                4L,
                100L,
                "device_a_wangwu",
                "workflow-test",
                List.of(new RequestItemCommand(300L)));
    }

    private static String submitAfterGate(
            ExecutionService executionService, long requestId, CountDownLatch startGate)
            throws InterruptedException {
        startGate.await();
        try {
            executionService.submit(requestId);
            return "SUCCESS";
        } catch (IllegalStateException ex) {
            return "ILLEGAL_STATE";
        }
    }
}
