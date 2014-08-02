package com.segment.android;

import static org.fest.assertions.api.Assertions.assertThat;

public class JsonTest extends BaseAndroidTestCase {
  public void testSimpleProperties() throws Exception {
    Properties properties = new Properties().putRevenue(39.99)
        .putCurrency("dollars")
        .putValue("starter")
        .put("discount", true);
    String json = properties.toString();
    assertThat(json).isEqualTo(
        "{\"value\":\"starter\",\"discount\":true,\"currency\":\"dollars\",\"revenue\":39.99}");

    Properties duplicate = new Properties().putRevenue(39.99)
        .putCurrency("dollars")
        .putValue("starter")
        .put("discount", true);
    assertThat(properties).isEqualTo(duplicate);

    assertThat(new Properties(json)).isEqualTo(properties);
  }

  public void testComplexProperties() throws Exception {
    Json nested = Json.create().put("someKey", "someValue").put("anotherKey", "anotherValue");
    String nestedJson = nested.toString();
    assertThat(nestedJson).isEqualTo("{\"someKey\":\"someValue\",\"anotherKey\":\"anotherValue\"}");

    Properties properties = new Properties().putRevenue(39.99)
        .putCurrency("dollars")
        .putValue("starter")
        .put("discount", true)
        .put("extra", nested);
    String json = properties.toString();
    assertThat(json).isEqualTo("{\"value\":\"starter\",\"extra\":{\"someKey\":\"someValue\","
        + "\"anotherKey\":\"anotherValue\"},\"discount\":true,\"currency\":\"dollars\","
        + "\"revenue\":39.99}");

    assertThat(new Properties(json)).isEqualTo(properties);
  }
}