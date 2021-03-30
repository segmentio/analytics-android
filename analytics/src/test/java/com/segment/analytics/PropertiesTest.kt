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
package com.segment.analytics

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class PropertiesTest {
    private lateinit var properties: Properties

    @Before
    fun setUp() {
        properties = Properties()
    }

    @Test
    fun revenue() {
        properties.putRevenue(3.14)
        assertThat(properties.value()).isEqualTo(3.14)
    }

    @Test
    fun value() {
        properties.putRevenue(2.72)
        assertThat(properties.value()).isEqualTo(2.72)

        properties.putValue(3.14)
        assertThat(properties.revenue()).isEqualTo(2.72)
        assertThat(properties.value()).isEqualTo(3.14)
    }

    @Test
    fun currency() {
        properties.putCurrency("INR")
        assertThat(properties.currency()).isEqualTo("INR")
    }

    @Test
    fun path() {
        properties.putPath("/sources")
        assertThat(properties.path()).isEqualTo("/sources")
    }

    @Test
    fun referrer() {
        properties.putReferrer("https://www.google.ca/")
        assertThat(properties.referrer()).isEqualTo("https://www.google.ca/")
    }

    @Test
    fun title() {
        properties.putTitle("Sports")
        assertThat(properties.title()).isEqualTo("Sports")
    }

    @Test
    fun url() {
        properties.putUrl("https://segment.com/docs/spec")
        assertThat(properties.url()).isEqualTo("https://segment.com/docs/spec")
    }

    @Test
    fun name() {
        properties.putName("Stripe")
        assertThat(properties.name()).isEqualTo("Stripe")
    }

    @Test
    fun category() {
        properties.putCategory("Sources")
        assertThat(properties.category()).isEqualTo("Sources")
    }

    @Test
    fun sku() {
        properties.putSku("sku-code")
        assertThat(properties.sku()).isEqualTo("sku-code")
    }

    @Test
    fun price() {
        assertThat(properties.price()).isEqualTo(0.0)

        properties.putPrice(1.01)
        assertThat(properties.price()).isEqualTo(1.01)
    }

    @Test
    fun productId() {
        properties.putProductId("123e4567-e89b-12d3-a456-426655440000")
        assertThat(properties.productId()).isEqualTo("123e4567-e89b-12d3-a456-426655440000")
    }

    @Test
    fun orderId() {
        properties.putOrderId("123e4567-e89b-12d3-a456-426655440000")
        assertThat(properties.orderId()).isEqualTo("123e4567-e89b-12d3-a456-426655440000")
    }

    @Test
    fun total() {
        assertThat(properties.total()).isEqualTo(0.0)

        properties.putValue(3.14)
        assertThat(properties.total()).isEqualTo(3.14)

        properties.putRevenue(2.72)
        assertThat(properties.total()).isEqualTo(2.72)

        properties.putTotal(1.02)
        assertThat(properties.total()).isEqualTo(1.02)
    }

    @Test
    fun subTotal() {
        properties.putSubtotal(9.99)
        assertThat(properties.subtotal()).isEqualTo(9.99)
    }

    @Test
    fun shipping() {
        properties.putShipping(2.72)
        assertThat(properties.shipping()).isEqualTo(2.72)
    }

    @Test
    fun tax() {
        properties.putTax(1.49)
        assertThat(properties.tax()).isEqualTo(1.49)
    }

    @Test
    fun discount() {
        properties.putDiscount(1.99)
        assertThat(properties.discount()).isEqualTo(1.99)
    }

    @Test
    fun coupon() {
        properties.putCoupon("segment")
        assertThat(properties.coupon()).isEqualTo("segment")
    }

    @Test
    fun products() {
        try {
            properties.putProducts()
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessage("products cannot be null or empty.")
        }

        val product1 = Properties.Product("id-1", "sku-1", 1.0)
        properties.putProducts(product1)
        assertThat(properties.products()).containsExactly(product1)

        val product2 = Properties.Product("id-2", "sku-2", 2.0)
        properties.putProducts(product1, product2)
        assertThat(properties.products()).containsExactly(product1, product2)
    }

    @Test
    fun repeatCustomer() {
        properties.putRepeatCustomer(true)

        assertThat(properties.isRepeatCustomer).isTrue()
    }

    @Test
    fun product() {
        val product = Properties.Product("id", "sku", 3.14)
        assertThat(product.id()).isEqualTo("id")
        assertThat(product.sku()).isEqualTo("sku")
        assertThat(product.price()).isEqualTo(3.14)

        assertThat(product.name()).isNull()
        product.putName("name")
        assertThat(product.name()).isEqualTo("name")

        product.remove("id")
        product.putValue("product_id", "id")
        assertThat(product.id()).isEqualTo("id")
    }
}
