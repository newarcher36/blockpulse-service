package com.blockchain.blockpulseservice.client.ws;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.mockito.Mockito.*;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketSessionHolderTest {

    @Test
    void defaultsToNull() {
        var holder = new WebSocketSessionHolder();
        assertThat(holder.get()).isNull();
    }

    @Test
    void setThenGetReturnsSameInstance() {
        var holder = new WebSocketSessionHolder();
        var s = Mockito.mock(WebSocketSession.class);
        holder.set(s);
        assertThat(holder.get()).isSameAs(s);
    }

    @Test
    void replacesSessionReference() {
        var holder = new WebSocketSessionHolder();
        var s1 = Mockito.mock(WebSocketSession.class);
        var s2 = Mockito.mock(WebSocketSession.class);
        holder.set(s1);
        holder.set(s2);
        assertThat(holder.get()).isSameAs(s2);
    }


    @Test
    void closesSessionIfIsOpen() throws Exception {
        var holder = new WebSocketSessionHolder();
        var session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        holder.set(session);

        holder.closeIfOpen(CloseStatus.NORMAL, URI.create("wss://unit.test/ws"));

        verify(session).close(any(CloseStatus.class));
    }

    @Test
    void doesNotCloseWhenNoSessionOrIsAlreadyClosed() throws Exception {
        var holder = new WebSocketSessionHolder();
        holder.closeIfOpen(CloseStatus.NORMAL, URI.create("wss://unit.test/ws"));

        var session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(false);
        holder.set(session);

        holder.closeIfOpen(CloseStatus.NORMAL, URI.create("wss://unit.test/ws"));

        verify(session, never()).close(any(CloseStatus.class));
    }
}