package com.yuno.gateway.util;

/**
 * PCI-DSS compliant card data masking utility.
 * 
 * Ensures card numbers are never stored or returned in plain text.
 * Only the last 4 digits are preserved for customer identification.
 */
public final class CardMaskingUtil {

    private CardMaskingUtil() {}

    /**
     * Mask a card number, preserving only the last N digits.
     * Example: "4242424242424242" → "****-****-****-4242"
     *
     * @param cardNumber Raw card number
     * @param showLast   Number of digits to show (default 4)
     * @return Masked card number
     */
    public static String mask(String cardNumber, int showLast) {
        if (cardNumber == null || cardNumber.length() < showLast) {
            return "****";
        }

        String cleaned = cardNumber.replaceAll("[^0-9]", "");
        if (cleaned.length() < showLast) {
            return "****";
        }

        String lastDigits = cleaned.substring(cleaned.length() - showLast);
        int maskedLength = cleaned.length() - showLast;
        StringBuilder masked = new StringBuilder();

        for (int i = 0; i < maskedLength; i++) {
            if (i > 0 && i % 4 == 0) {
                masked.append("-");
            }
            masked.append("*");
        }
        masked.append("-").append(lastDigits);

        return masked.toString();
    }

    /**
     * Mask with default 4 visible digits.
     */
    public static String mask(String cardNumber) {
        return mask(cardNumber, 4);
    }

    /**
     * Detect card brand from card number (BIN-based detection).
     *
     * @param cardNumber Raw or partial card number
     * @return Card brand name
     */
    public static String detectBrand(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return "UNKNOWN";
        }

        String cleaned = cardNumber.replaceAll("[^0-9]", "");

        if (cleaned.startsWith("4")) {
            return "VISA";
        } else if (cleaned.startsWith("5") || cleaned.startsWith("2")) {
            return "MASTERCARD";
        } else if (cleaned.startsWith("3")) {
            return "AMEX";
        } else if (cleaned.startsWith("6")) {
            return "DISCOVER";
        } else {
            return "UNKNOWN";
        }
    }
}
