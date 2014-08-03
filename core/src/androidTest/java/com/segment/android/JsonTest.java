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
        "{\"revenue\":39.99,\"currency\":\"dollars\",\"value\":\"starter\",\"discount\":true}");

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
    assertThat(json).isEqualTo(
        "{\"revenue\":39.99,\"currency\":\"dollars\",\"value\":\"starter\",\"discount\":true,\"extra\":{\"someKey\":\"someValue\",\"anotherKey\":\"anotherValue\"}}");

    assertThat(new Properties(json)).isEqualTo(properties);
  }
}