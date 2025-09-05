package com.blockchain.blockpulseservice.client.ws.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconnectionManagerTest {

    @Mock private ScheduledExecutorService scheduler;
    @Mock private RetryTemplate retryTemplate;
    @Mock private ScheduledFuture<?> scheduledFuture;

    @InjectMocks private ReconnectionManager manager;

    private final URI serverUri = URI.create("wss://example.test/ws");

    @BeforeEach
    void setup() {
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .thenReturn(scheduledFuture);
    }

    @Test
    void scheduleReconnect_schedulesRunnable_andInvokesCallbackViaRetryTemplate() throws Exception {
        Runnable reconnectCallback = mock(Runnable.class);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        // Act: schedule
        manager.scheduleReconnect(reconnectCallback, serverUri);

        // Assert scheduling details
        verify(scheduler).schedule(runnableCaptor.capture(), eq(3L), eq(TimeUnit.SECONDS));

        // Stub retry to call through
        when(retryTemplate.execute(any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    RetryCallback<Object, Exception> cb = inv.getArgument(0);
                    RetryContext ctx = mock(RetryContext.class);
                    when(ctx.getRetryCount()).thenReturn(0);
                    return cb.doWithRetry(ctx);
                });

        // Trigger the scheduled runnable
        runnableCaptor.getValue().run();

        verify(retryTemplate).execute(any());
        verify(reconnectCallback).run();
    }

    @Test
    void cancelReconnect_cancelsWhenTaskPending() {
        Runnable reconnectCallback = mock(Runnable.class);
        manager.scheduleReconnect(reconnectCallback, serverUri);

        when(scheduledFuture.isDone()).thenReturn(false);

        manager.cancelReconnect();

        verify(scheduledFuture).cancel(false);
    }

    @Test
    void cancelReconnect_noopWhenNoTaskOrAlreadyDone() {
        // No task scheduled -> noop
        manager.cancelReconnect();

        // Now schedule but mark as done
        Runnable reconnectCallback = mock(Runnable.class);
        manager.scheduleReconnect(reconnectCallback, serverUri);
        when(scheduledFuture.isDone()).thenReturn(true);

        manager.cancelReconnect();

        verify(scheduledFuture, never()).cancel(anyBoolean());
    }
}

