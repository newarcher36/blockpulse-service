package com.blockchain.blockpulseservice.client.ws.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionStateManagerTest {

    private ConnectionStateManager connectionStateManager;

    @BeforeEach
    void setUp() {
        connectionStateManager = new ConnectionStateManager();
    }

    @Test
    void defaultsToDisconnected() {
        assertThat(connectionStateManager.isConnected()).isFalse();
    }

    @Test
    void canSetConnectedTrueThenFalse() {
        connectionStateManager.setConnected(true);
        assertThat(connectionStateManager.isConnected()).isTrue();

        connectionStateManager.setConnected(false);
        assertThat(connectionStateManager.isConnected()).isFalse();
    }

    @Test
    void idempotentStateUpdates() {
        connectionStateManager.setConnected(true);
        connectionStateManager.setConnected(true);
        assertThat(connectionStateManager.isConnected()).isTrue();

        connectionStateManager.setConnected(false);
        connectionStateManager.setConnected(false);
        assertThat(connectionStateManager.isConnected()).isFalse();
    }
}

