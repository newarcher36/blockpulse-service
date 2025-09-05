package com.blockchain.blockpulseservice.client.ws;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.WebSocketSession;

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
}

