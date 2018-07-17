/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.segment.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.segment.analytics.Properties.Product;
import org.junit.Before;
import org.junit.Test;

public class PropertiesTest {
  Properties properties;

  @Before
  public void setUp() {
    properties = new Properties();
  }

  @Test
  public void revenue() {
    properties.putRevenue(3.14);
    assertThat(properties.revenue()).isEqualTo(3.14);
  }

  @Test
  public void value() {
    properties.putRevenue(2.72);
    assertThat(properties.value()).isEqualTo(2.72);

    properties.putValue(3.14);
    assertThat(properties.revenue()).isEqualTo(2.72);
    assertThat(properties.value()).isEqualTo(3.14);
  }

  @Test
  public void currency() {
    properties.putCurrency("INR");
    assertThat(properties.currency()).isEqualTo("INR");
  }

  @Test
  public void path() {
    properties.putPath("/sources");
    assertThat(properties.path()).isEqualTo("/sources");
  }

  @Test
  public void referrer() {
    properties.putReferrer("https://www.google.ca/");
    assertThat(properties.referrer()).isEqualTo("https://www.google.ca/");
  }

  @Test
  public void title() {
    properties.putTitle("Sports");
    assertThat(properties.title()).isEqualTo("Sports");
  }

  @Test
  public void url() {
    properties.putUrl("https://segment.com/docs/spec");
    assertThat(properties.url()).isEqualTo("https://segment.com/docs/spec");
  }

  @Test
  public void name() {
    properties.putName("Stripe");
    assertThat(properties.name()).isEqualTo("Stripe");
  }

  @Test
  public void category() {
    properties.putCategory("Sources");
    assertThat(properties.category()).isEqualTo("Sources");
  }

  @Test
  public void sku() {
    properties.putSku("sku-code");
    assertThat(properties.sku()).isEqualTo("sku-code");
  }

  @Test
  public void price() {
    assertThat(properties.price()).isEqualTo(0);

    properties.putPrice(1.01);
    assertThat(properties.price()).isEqualTo(1.01);
  }

  @Test
  public void productId() {
    properties.putProductId("123e4567-e89b-12d3-a456-426655440000");
    assertThat(properties.productId()).isEqualTo("123e4567-e89b-12d3-a456-426655440000");
  }

  @Test
  public void orderId() {
    properties.putOrderId("123e4567-e89b-12d3-a456-426655440000");
    assertThat(properties.orderId()).isEqualTo("123e4567-e89b-12d3-a456-426655440000");
  }

  @Test
  public void total() {
    assertThat(properties.total()).isEqualTo(0);

    properties.putValue(3.14);
    assertThat(properties.total()).isEqualTo(3.14);

    properties.putRevenue(2.72);
    assertThat(properties.total()).isEqualTo(2.72);

    properties.putTotal(1.02);
    assertThat(properties.total()).isEqualTo(1.02);
  }

  @Test
  public void subtotal() {
    properties.putSubtotal(9.99);
    assertThat(properties.subtotal()).isEqualTo(9.99);
  }

  @Test
  public void shipping() {
    properties.putShipping(2.72);
    assertThat(properties.shipping()).isEqualTo(2.72);
  }

  @Test
  public void tax() {
    properties.putTax(1.49);
    assertThat(properties.tax()).isEqualTo(1.49);
  }

  @Test
  public void discount() {
    properties.putDiscount(1.99);
    assertThat(properties.discount()).isEqualTo(1.99);
  }

  @Test
  public void coupon() {
    properties.putCoupon("segment");
    assertThat(properties.coupon()).isEqualTo("segment");
  }

  @Test
  public void products() {
    try {
      properties.putProducts();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("products cannot be null or empty.");
    }

    Product product1 = new Product("id-1", "sku-1", 1);
    properties.putProducts(product1);
    assertThat(properties.products()).containsExactly(product1);

    Product product2 = new Product("id-2", "sku-2", 2);
    properties.putProducts(product1, product2);
    assertThat(properties.products()).containsExactly(product1, product2);
  }

  @Test
  public void repeatCustomer() {
    properties.putRepeatCustomer(true);

    assertThat(properties.isRepeatCustomer()).isTrue();
  }

  @Test
  public void product() {
    Product product = new Product("id", "sku", 3.14);
    assertThat(product.id()).isEqualTo("id");
    assertThat(product.sku()).isEqualTo("sku");
    assertThat(product.price()).isEqualTo(3.14);

    assertThat(properties.name()).isNull();
    product.putName("name");
    assertThat(product.name()).isEqualTo("name");
  }
}
