package com.fichedecontrole.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests pour DateUtils
 */
class DateUtilsTest {

    @Test
    void testFormatDate_withLocalDate_shouldReturnFormattedString() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        String result = DateUtils.formatDate(date);

        assertThat(result).isEqualTo("15/01/2024");
    }

    @Test
    void testParseDate_withValidString_shouldReturnLocalDate() {
        LocalDate result = DateUtils.parseDate("15/01/2024");

        assertThat(result).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    void testParseDate_withInvalidString_shouldThrow() {
        assertThatThrownBy(() -> {
            DateUtils.parseDate("invalid");
        }).isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void testFormatDate_withNull_shouldReturnEmpty() {
        String result = DateUtils.formatDate((LocalDate) null);
        assertThat(result).isEmpty();
    }
}
