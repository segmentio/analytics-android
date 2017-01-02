/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment, Inc.
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

import com.segment.analytics.internal.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Properties are a dictionary of free-form information to attach to specific events.
 * <p/>
 * Just like traits, we also accept some properties with semantic meaning, and you should only ever
 * use these property names for that purpose.
 */
public class Properties extends ValueMap {

  // Common Properties
  private static final String REVENUE_KEY = "revenue";
  private static final String CURRENCY_KEY = "currency";
  private static final String VALUE_KEY = "value";
  // Screen Properties
  private static final String PATH_KEY = "path";
  private static final String REFERRER_KEY = "referrer";
  private static final String TITLE_KEY = "title";
  private static final String URL_KEY = "url";
  // Ecommerce API
  private static final String NAME_KEY = "name"; // used by product too
  private static final String CATEGORY_KEY = "category";
  private static final String SKU_KEY = "sku";
  private static final String PRICE_KEY = "price";
  private static final String ID_KEY = "id";
  private static final String ORDER_ID_KEY = "orderId";
  private static final String TOTAL_KEY = "total";
  private static final String SUBTOTAL_KEY = "subtotal";
  private static final String SHIPPING_KEY = "shipping";
  private static final String TAX_KEY = "tax";
  private static final String DISCOUNT_KEY = "discount";
  private static final String COUPON_KEY = "coupon";
  private static final String PRODUCTS_KEY = "products";
  private static final String REPEAT_KEY = "repeat";

  public Properties() {
  }

  public Properties(int initialCapacity) {
    super(initialCapacity);
  }

  // For deserialization
  Properties(Map<String, Object> delegate) {
    super(delegate);
  }

  @Override public Properties putValue(String key, Object value) {
    super.putValue(key, value);
    return this;
  }

  /**
   * Set the amount of revenue an event resulted in. This should be a decimal value in dollars, so
   * a shirt worth $19.99 would result in a revenue of 19.99.
   */
  public Properties putRevenue(double revenue) {
    return putValue(REVENUE_KEY, revenue);
  }

  public double revenue() {
    return getDouble(REVENUE_KEY, 0);
  }

  /**
   * Set an abstract value to associate with an event. This is typically used in situations where
   * the event doesn’t generate real-dollar revenue, but has an intrinsic value to a marketing
   * team, like newsletter signups.
   */
  public Properties putValue(double value) {
    return putValue(VALUE_KEY, value);
  }

  public double value() {
    double value = getDouble(VALUE_KEY, 0);
    if (value != 0) {
      return value;
    }
    return revenue();
  }

  /** The currency for the value set in {@link #putRevenue(double)}. */
  public Properties putCurrency(String currency) {
    return putValue(CURRENCY_KEY, currency);
  }

  public String currency() {
    return getString(CURRENCY_KEY);
  }

  /**
   * Set a path (usually the path of the URL) for the screen.
   *
   * @see <a href="https://segment.com/docs/api/tracking/page/#properties">Page Properties</a>
   */
  public Properties putPath(String path) {
    return putValue(PATH_KEY, path);
  }

  public String path() {
    return getString(PATH_KEY);
  }

  /**
   * Set the referrer that led the user to the screen. In the browser it is the document.referrer
   * property.
   *
   * @see <a href="https://segment.com/docs/api/tracking/page/#properties">Page Properties</a>
   */
  public Properties putReferrer(String referrer) {
    return putValue(REFERRER_KEY, referrer);
  }

  public String referrer() {
    return getString(REFERRER_KEY);
  }

  /**
   * Set the title of the screen.
   *
   * @see <a href="https://segment.com/docs/api/tracking/page/#properties">Page Properties</a>
   */
  public Properties putTitle(String title) {
    return putValue(TITLE_KEY, title);
  }

  public String title() {
    return getString(TITLE_KEY);
  }

  /**
   * Set a url for the screen.
   *
   * @see <a href="https://segment.com/docs/api/tracking/page/#properties">Page Properties</a>
   */
  public Properties putUrl(String url) {
    return putValue(URL_KEY, url);
  }

  public String url() {
    return getString(URL_KEY);
  }

  /**
   * Set the name of the product associated with an event.
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putName(String name) {
    return putValue(NAME_KEY, name);
  }

  public String name() {
    return getString(NAME_KEY);
  }

  /**
   * Set a category for this action. You’ll want to track all of your product category pages so
   * you can quickly see which categories are most popular.
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putCategory(String category) {
    return putValue(CATEGORY_KEY, category);
  }

  public String category() {
    return getString(CATEGORY_KEY);
  }

  /**
   * Set a sku for the product associated with an event.
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putSku(String sku) {
    return putValue(SKU_KEY, sku);
  }

  public String sku() {
    return getString(SKU_KEY);
  }

  /**
   * Set a price (in dollars) for the product associated with an event.
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putPrice(double price) {
    return putValue(PRICE_KEY, price);
  }

  public double price() {
    return getDouble(PRICE_KEY, 0);
  }

  /**
   * Set an ID for the product associated with an event.
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putProductId(String id) {
    return putValue(ID_KEY, id);
  }

  public String productId() {
    return getString(ID_KEY);
  }

  /**
   * Set the order ID associated with an event.
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putOrderId(String orderId) {
    return putValue(ORDER_ID_KEY, orderId);
  }

  public String orderId() {
    return getString(ORDER_ID_KEY);
  }

  /**
   * Set the total amount (in dollars) for an order associated with an event.
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putTotal(double total) {
    return putValue(TOTAL_KEY, total);
  }

  public double total() {
    double total = getDouble(TOTAL_KEY, 0);
    if (total != 0) {
      return total;
    }
    double revenue = revenue();
    if (revenue != 0) {
      return revenue;
    }
    return value();
  }

  /**
   * Set the subtotal (in dollars) for an order associated with an event (excluding tax and
   * shipping).
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putSubtotal(double subtotal) {
    return putValue(SUBTOTAL_KEY, subtotal);
  }

  public double putSubtotal() {
    return getDouble(SUBTOTAL_KEY, 0);
  }

  /**
   * Set the shipping amount (in dollars) for an order associated with an event.
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putShipping(double shipping) {
    return putValue(SHIPPING_KEY, shipping);
  }

  public double shipping() {
    return getDouble(SHIPPING_KEY, 0);
  }

  /**
   * Set the tax amount (in dollars) for an order associated with an event.
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putTax(double tax) {
    return putValue(TAX_KEY, tax);
  }

  public double tax() {
    return getDouble(TAX_KEY, 0);
  }

  /**
   * Set the discount amount (in dollars) for an order associated with an event.
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putDiscount(double discount) {
    return putValue(DISCOUNT_KEY, discount);
  }

  public double discount() {
    return getDouble(DISCOUNT_KEY, 0);
  }

  /**
   * Set a coupon name for an order associated with an event.
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putCoupon(String coupon) {
    return putValue(COUPON_KEY, coupon);
  }

  /**
   * Set the individual products for an order associated with an event.
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putProducts(Product... products) {
    if (Utils.isNullOrEmpty(products)) {
      throw new IllegalArgumentException("products cannot be null or empty.");
    }
    List<Product> productList = new ArrayList<>(products.length);
    Collections.addAll(productList, products);
    return putValue(PRODUCTS_KEY, Collections.unmodifiableList(productList));
  }

  /** @deprecated Use {@link #products()} instead. */
  public List<Product> products(Product... products) {
    return products();
  }

  public List<Product> products() {
    return getList(PRODUCTS_KEY, Product.class);
  }

  /**
   * Set whether an order associated with an event is from a repeating customer.
   *
   * @see <a href="https://segment.com/docs/api/tracking/ecommerce/">Ecommerce API</a>
   */
  public Properties putRepeatCustomer(boolean repeat) {
    return putValue(REPEAT_KEY, repeat);
  }

  public boolean isRepeatCustomer() {
    return getBoolean(REPEAT_KEY, false);
  }

  /**
   * A representation of an e-commerce product.
   * <p/>
   * Use this only when you have multiple products, usually for the "Completed Order" event. If you
   * have only one product, {@link Properties} has methods on it directly to attach this
   * information.
   */
  public static class Product extends ValueMap {

    private static final String ID_KEY = "id";
    private static final String SKU_KEY = "sku";
    private static final String NAME_KEY = "name";
    private static final String PRICE_KEY = "price";

    /**
     * Create an e-commerce product with the given id, sku and price (in dollars). All parameters
     * are required for our ecommerce API.
     *
     * @param id The product ID in your database
     * @param sku The product SKU
     * @param price The price of the product (in dollars)
     */
    public Product(String id, String sku, double price) {
      put(ID_KEY, id);
      put(SKU_KEY, sku);
      put(PRICE_KEY, price);
    }

    // For deserialization
    private Product(Map<String, Object> map) {
      super(map);
    }

    /** Set an optional name for this product. */
    public Product putName(String name) {
      return putValue(NAME_KEY, name);
    }

    public String name() {
      return getString(NAME_KEY);
    }

    public String id() {
      return getString(ID_KEY);
    }

    public String sku() {
      return getString(SKU_KEY);
    }

    public double price() {
      return getDouble(PRICE_KEY, 0);
    }

    @Override public Product putValue(String key, Object value) {
      super.putValue(key, value);
      return this;
    }
  }
}
