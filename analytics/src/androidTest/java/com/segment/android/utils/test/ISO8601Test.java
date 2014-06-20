package com.segment.android.utils.test;

import android.test.AndroidTestCase;
import com.segment.android.utils.ISO8601;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.junit.Assert;
import org.junit.Test;

public class ISO8601Test extends AndroidTestCase {

  @Test
  public void testSerialize() throws ParseException {

    Date date = new Date();
    Calendar expected = new GregorianCalendar();
    expected.setTime(date);

    String iso8601 = ISO8601.fromCalendar(expected);

    Calendar got = ISO8601.toCalendar(iso8601);

    // test this way until millisecond testing is fixed
    Assert.assertEquals(date.toString(), got.getTime().toString());
  }
}
