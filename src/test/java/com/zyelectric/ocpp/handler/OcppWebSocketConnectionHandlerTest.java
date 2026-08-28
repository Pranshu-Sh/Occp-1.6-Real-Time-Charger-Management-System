package com.zyelectric.ocpp.handler;

import com.zyelectric.ocpp.service.OcppMessageProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcppWebSocketConnectionHandlerTest {

    @Mock
    private OcppMessageProcessor messageProcessor;

    private OcppWebSocketConnectionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OcppWebSocketConnectionHandler(messageProcessor);
    }

    private WebSocketSession mockSession(String chargePointName) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(new URI("ws://localhost:9093/" + chargePointName));
        when(session.getId()).thenReturn(UUID.randomUUID().toString());
        lenient().when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    void messagesForSameCharger_processedInOrder() throws Exception {
        WebSocketSession session = mockSession("CP001");
        handler.afterConnectionEstablished(session);

        List<String> receivedOrder = Collections.synchronizedList(new ArrayList<>());
        doAnswer(inv -> {
            receivedOrder.add(inv.getArgument(1));
            Thread.sleep(5); // encourages interleaving across threads if order isn't actually serialized
            return null;
        }).when(messageProcessor).processMessage(eq("CP001"), anyString(), eq(session));

        int count = 20;
        for (int i = 0; i < count; i++) {
            handler.handleMessage(session, new TextMessage("msg-" + i));
        }

        waitUntil(() -> receivedOrder.size() >= count, 5000);

        List<String> expected = IntStream.range(0, count).mapToObj(i -> "msg-" + i).collect(Collectors.toList());
        assertThat(receivedOrder).containsExactlyElementsOf(expected);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
    }

    @Test
    void sessionClose_midProcessing_doesNotSpinThread() throws Exception {
        WebSocketSession session = mockSession("CP002");
        handler.afterConnectionEstablished(session);

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(inv -> {
            started.countDown();
            release.await(2, TimeUnit.SECONDS);
            return null;
        }).when(messageProcessor).processMessage(eq("CP002"), anyString(), eq(session));

        handler.handleMessage(session, new TextMessage("msg-0"));
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

        release.countDown();

        long startNanos = System.nanoTime();
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        // The old implementation could spin a thread forever re-polling a closed session's
        // queue. The new one just shuts the per-session executor down - bounded, deterministic.
        assertThat(elapsedMs).isLessThan(6000);
    }

    private void waitUntil(java.util.function.BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertThat(condition.getAsBoolean()).as("condition met within timeout").isTrue();
    }
}
