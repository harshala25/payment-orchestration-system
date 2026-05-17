package com.yuno.gateway.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardMaskingUtilTest {

    @Test
    @DisplayName("Should mask card number showing last 4 digits")
    void shouldMaskCardNumber() {
        String masked = CardMaskingUtil.mask("4242424242424242");
        assertTrue(masked.endsWith("4242"));
        assertFalse(masked.contains("424242424242"));
    }

    @Test
    @DisplayName("Should handle null card number")
    void shouldHandleNullCardNumber() {
        assertEquals("****", CardMaskingUtil.mask(null));
    }

    @Test
    @DisplayName("Should handle short card number")
    void shouldHandleShortCardNumber() {
        assertEquals("****", CardMaskingUtil.mask("42"));
    }

    @Test
    @DisplayName("Should detect VISA card brand")
    void shouldDetectVisa() {
        assertEquals("VISA", CardMaskingUtil.detectBrand("4242424242424242"));
    }

    @Test
    @DisplayName("Should detect MASTERCARD brand")
    void shouldDetectMastercard() {
        assertEquals("MASTERCARD", CardMaskingUtil.detectBrand("5555555555554444"));
    }

    @Test
    @DisplayName("Should detect AMEX brand")
    void shouldDetectAmex() {
        assertEquals("AMEX", CardMaskingUtil.detectBrand("378282246310005"));
    }

    @Test
    @DisplayName("Should return UNKNOWN for unrecognized BIN")
    void shouldReturnUnknownForUnrecognizedBin() {
        assertEquals("UNKNOWN", CardMaskingUtil.detectBrand("9999999999999999"));
    }
}
