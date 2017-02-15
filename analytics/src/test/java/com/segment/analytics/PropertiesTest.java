package com.segment.analytics;

import com.segment.analytics.Properties.Product;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesTest {
  @Test public void products() {
    Product product = new Product("foo", "bar", 10);
    Properties properties = new Properties();
    properties.putProducts(product);

    assertThat(properties.products()).containsExactly(product);
  }
}
