/*
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

import java.util.List;
import java.util.Map;

/**
 * Just like traits, we also imbue some properties with semantic meaning, and you should only ever
 * use these property names for that purpose.
 */
public class Properties extends JsonMap {
  // Common
  private static final String REVENUE_KEY = "revenue";
  private static final String VALUE_KEY = "value";
  private static final String CURRENCY_KEY = "currency";

  // Page
  private static final String CATEGORY_KEY = "category";
  private static final String NAME_KEY = "name"; // used by product too
  private static final String PATH_KEY = "path";
  private static final String REFERER_KEY = "referer";
  private static final String TITLE_KEY = "title";
  private static final String URL_KEY = "url";

  // Transaction
  private static final String ORDER_ID_KEY = "orderId";
  private static final String TOTAL_KEY = "total";
  private static final String SUB_TOTAL_KEY = "subtotal";
  private static final String SHIPPING_KEY = "shipping";
  private static final String TAX_KEY = "tax";
  private static final String DISCOUNT_KEY = "discount";
  private static final String COUPON_KEY = "coupon";
  private static final String PRODUCTS_KEY = "products";

  // Product
  private static final String ID_KEY = "id";
  private static final String SKU_KEY = "sku";
  private static final String PRICE_KEY = "price";

  public Properties() {
  }

  // For deserialization
  Properties(Map<String, Object> delegate) {
    super(delegate);
  }

  @Override public Properties putValue(String key, Object value) {
    super.putValue(key, value);
    return this;
  }

  // Common Properties

  public Properties putRevenue(double revenue) {
    return putValue(REVENUE_KEY, revenue);
  }

  double revenue() {
    return getDouble(REVENUE_KEY, 0);
  }

  public Properties putValue(String value) {
    return putValue(VALUE_KEY, value);
  }

  public Properties putCurrency(String currency) {
    return putValue(CURRENCY_KEY, currency);
  }

  String currency() {
    return getString(CURRENCY_KEY);
  }

  public Properties putId(String id) {
    return putValue(ID_KEY, id);
  }

  String id() {
    return getString(ID_KEY);
  }

  // Page Properties
  public Properties putCategory(String category) {
    return putValue(CATEGORY_KEY, category);
  }

  String category() {
    return getString(CATEGORY_KEY);
  }

  public Properties putName(String name) {
    return putValue(NAME_KEY, name);
  }

  String name() {
    return getString(NAME_KEY);
  }

  public Properties putPath(String path) {
    return putValue(PATH_KEY, path);
  }

  public Properties putReferer(String referer) {
    return putValue(REFERER_KEY, referer);
  }

  public Properties putTitle(String title) {
    return putValue(TITLE_KEY, title);
  }

  public Properties putUrl(String url) {
    return putValue(URL_KEY, url);
  }

  // Transaction Properties

  public Properties putOrderId(String orderId) {
    return putValue(ORDER_ID_KEY, orderId);
  }

  String orderId() {
    return getString(ORDER_ID_KEY);
  }

  public Properties putTotal(long total) {
    return putValue(TOTAL_KEY, total);
  }

  public double total() {
    return getDouble(TOTAL_KEY, 0);
  }

  public Properties putSubTotal(long subTotal) {
    return putValue(SUB_TOTAL_KEY, subTotal);
  }

  public Properties putShipping(double shipping) {
    return putValue(SHIPPING_KEY, shipping);
  }

  double shipping() {
    return getDouble(SHIPPING_KEY, 0);
  }

  public Properties putTax(double tax) {
    return putValue(TAX_KEY, tax);
  }

  double tax() {
    return getDouble(TAX_KEY, 0);
  }

  public Properties putDiscount(double discount) {
    return putValue(DISCOUNT_KEY, discount);
  }

  public Properties putCoupon(String coupon) {
    return putValue(COUPON_KEY, coupon);
  }

  public Properties putProducts(Product... products) {
    return putValue(PRODUCTS_KEY, products);
  }

  List<Product> products(Product... products) {
    return getJsonList(PRODUCTS_KEY, Product.class);
  }

  // Product Properties

  public Properties putSku(String sku) {
    return putValue(SKU_KEY, sku);
  }

  String sku() {
    return getString(SKU_KEY);
  }

  public Properties putPrice(double price) {
    return putValue(PRICE_KEY, price);
  }

  double price() {
    return getDouble(PRICE_KEY, 0);
  }

  static class Product extends JsonMap {
    private static final String ID_KEY = "id";
    private static final String SKU_KEY = "sku";
    private static final String NAME_KEY = "name";
    private static final String PRICE_KEY = "price";

    public Product(String id, String sku, double price) {
      putValue(ID_KEY, id);
      putValue(SKU_KEY, sku);
      putValue(PRICE_KEY, price);
    }

    // For deserialization
    Product(Map<String, Object> delegate) {
      super(delegate);
    }

    public Product putName(String name) {
      return putValue(NAME_KEY, name);
    }

    String id() {
      return getString(ID_KEY);
    }

    String sku() {
      return getString(SKU_KEY);
    }

    String name() {
      return getString(NAME_KEY);
    }

    double price() {
      return getDouble(PRICE_KEY, 0);
    }

    @Override public Product putValue(String key, Object value) {
      super.putValue(key, value);
      return this;
    }
  }
}