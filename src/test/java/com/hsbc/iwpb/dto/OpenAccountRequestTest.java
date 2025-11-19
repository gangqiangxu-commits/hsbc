package com.hsbc.iwpb.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OpenAccountRequestTest {
    @Test
    void testAllArgsConstructorAndGetters() {
        OpenAccountRequest req = new OpenAccountRequest("Alice", 123L);
        assertEquals("Alice", req.name());
        assertEquals(123L, req.personalId());
    }
    @Test
    void testEqualsAndHashCode() {
        OpenAccountRequest r1 = new OpenAccountRequest("Alice", 123L);
        OpenAccountRequest r2 = new OpenAccountRequest("Alice", 123L);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
    @Test
    void testToString() {
        OpenAccountRequest req = new OpenAccountRequest("Alice", 123L);
        assertTrue(req.toString().contains("name=Alice"));
    }
}
