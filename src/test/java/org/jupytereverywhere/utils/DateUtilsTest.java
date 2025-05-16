package org.jupytereverywhere.utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateUtilsTest {

  private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

  @Test
  void testFormatUTC_validDate() {
    Date testDate = new Date(0); // Epoch (1970-01-01 00:00:00 UTC)
    String expected = "1970-01-01 00:00:00";

    String result = DateUtils.formatUTC(testDate);

    assertEquals(expected, result, "Formatted date should match the expected UTC format");
  }

  @Test
  void testFormatUTC_withCurrentDate() {
    Date currentDate = new Date();
    String result = DateUtils.formatUTC(currentDate);

    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
    dateFormat.setTimeZone(UTC);
    String expected = dateFormat.format(currentDate);

    assertEquals(expected, result, "Formatted current date should match the expected UTC format");
  }

  @Test
  void testUtcDateToTimestamp_validDate() {
    Date testDate = new Date(0); // Epoch (1970-01-01 00:00:00 UTC)
    Timestamp expectedTimestamp = new Timestamp(0); // Epoch as Timestamp

    Timestamp result = DateUtils.utcDateToTimestamp(testDate);

    assertNotNull(result, "Timestamp should not be null");
    assertEquals(expectedTimestamp, result, "Timestamp should match the expected Epoch value");
  }

  @Test
  void testUtcDateToTimestamp_withCurrentDate() {
    Date currentDate = new Date();
    Timestamp result = DateUtils.utcDateToTimestamp(currentDate);

    assertNotNull(result, "Timestamp should not be null");

    long allowedDifference = 1000; // Tolerancia permitida en milisegundos
    long difference = Math.abs(result.getTime() - currentDate.getTime());

    assertTrue(difference <= allowedDifference,
        "Timestamp difference should be within the allowed tolerance. Difference: " + difference + "ms.");
  }

  @Test
  void testFormatUTC_handlesNullDate() {
    assertThrows(NullPointerException.class, () -> DateUtils.formatUTC(null));
  }

  @Test
  void testUtcDateToTimestamp_handlesNullDate() {
    assertThrows(NullPointerException.class, () -> DateUtils.utcDateToTimestamp(null));
  }
}
