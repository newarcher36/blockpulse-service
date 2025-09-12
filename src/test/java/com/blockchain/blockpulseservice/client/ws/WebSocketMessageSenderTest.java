package com.blockchain.blockpulseservice.client.ws;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketMessageSenderTest {
    private static final URI SERVER_URI = URI.create("ws://example.test");
    private final WebSocketMessageSender sender = new WebSocketMessageSender();
    @Mock
    private WebSocketSession session;
    @Captor
    private ArgumentCaptor<TextMessage> msgCaptor;

    @Test
    void doesNothingWhenSessionIsNull() {
        Runnable failure = mock(Runnable.class);

        sender.send(null, "hello", SERVER_URI, failure);

        verifyNoInteractions(failure);
    }

    @Test
    void doesNothingWhenSessionClosed() throws Exception {
        when(session.isOpen()).thenReturn(false);
        var failure = mock(Runnable.class);

        sender.send(session, "hello", SERVER_URI, failure);

        verify(session, never()).sendMessage(any());
        verifyNoInteractions(failure);
    }

    @Test
    void sendsWhenSessionOpen() throws Exception {
        when(session.isOpen()).thenReturn(true);
        var failure = mock(Runnable.class);

        sender.send(session, "hello", SERVER_URI, failure);

        verify(session).sendMessage(msgCaptor.capture());
        assertThat(msgCaptor.getValue().getPayload()).isEqualTo("hello");
        verifyNoInteractions(failure);
    }

    @Test
    void runsFailureCallbackOnIOException() throws Exception {
        when(session.isOpen()).thenReturn(true);
        doThrow(new IOException("boom")).when(session).sendMessage(any());
        var failure = mock(Runnable.class);

        sender.send(session, "hello", SERVER_URI, failure);

        verify(failure, times(1)).run();
    }
}