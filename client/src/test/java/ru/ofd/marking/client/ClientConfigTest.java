package ru.ofd.marking.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ClientConfigTest {
    @Test
    void defaults() {
        ClientConfig c = new ClientConfig();
        assertEquals("http://127.0.0.1:18443", c.getServerUrl());
        assertEquals(25.0, c.getThreshold());
        assertEquals(30, c.getPeriodDays());
    }
}
