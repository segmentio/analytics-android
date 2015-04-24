package com.segment.analytics.internal.integrations;

import android.text.TextUtils;

import com.kahuna.sdk.KahunaAnalytics;
import com.segment.analytics.internal.model.payloads.TrackPayload;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Kahuna helps mobile marketers send push notifications and in-app messages.
 *
 * @see <a href="https://www.kahuna.com/">Kahuna</a>
 * @see <a href="https://segment.com/docs/integrations/kahuna/">Kahuna Integration</a>
 * @see <a href="http://app.usekahuna.com/tap/getstarted/android/">Kahuna Android SDK</a>
 */
public class KahunaEcommerceHelper {

  // Segment constants
  private static final String SEG_VIEWED_PRODUCT_CATEGORY = "Viewed Product Category";
  private static final String SEG_VIEWED_PRODUCT = "Viewed Product";
  private static final String SEG_ADDED_PRODUCT = "Added Product";
  private static final String SEG_COMPLETED_ORDER = "Completed Order";
  private static final String SEG_DISCOUNT_KEY = "discount";

  // Kahuna constants
  private static final String CATEGORIES_VIEWED = "Categories Viewed";
  private static final String LAST_VIEWED_CATEGORY = "Last Viewed Category";
  private static final String LAST_PRODUCT_VIEWED_NAME = "Last Product Viewed Name";
  private static final String LAST_PRODUCT_VIEWED_ID = "Last Product Viewed Id";
  private static final String LAST_PRODUCT_ADDED_TO_CART_NAME = "Last Product Added To Cart Name";
  private static final String LAST_PRODUCT_ADDED_TO_CART_CATEGORY
          = "Last Product Added To Cart Category";
  private static final String LAST_PURCHASE_DISCOUNT = "Last Purchase Discount";

  private static final int MAX_CATEGORIES_VIEWED_ENTRIES = 50;
  private static final String NONE = "None";

  public void process(TrackPayload track) {
    if (track != null && !TextUtils.isEmpty(track.event()) && track.properties() != null) {

      if (SEG_VIEWED_PRODUCT_CATEGORY.equalsIgnoreCase(track.event())) {

        processProductCategory(track);

      } else if (SEG_VIEWED_PRODUCT.equalsIgnoreCase(track.event())) {

        processViewedProductName(track);
        processProductCategory(track);

      } else if (SEG_ADDED_PRODUCT.equalsIgnoreCase(track.event())) {

        processAddedProductName(track);
        processAddedProductCategory(track);

      } else if (SEG_COMPLETED_ORDER.equalsIgnoreCase(track.event())) {

        processCompletedOrderDiscount(track);

      }
    }
  }

  private void processCompletedOrderDiscount(TrackPayload track) {
    String discountString = track.properties().getString(SEG_DISCOUNT_KEY);
    if (!TextUtils.isEmpty(discountString)) {
      Map<String, String> userAttributes = KahunaAnalytics.getUserAttributes();
      userAttributes.put(LAST_PURCHASE_DISCOUNT, discountString);
      KahunaAnalytics.setUserAttributes(userAttributes);
    }
  }

  private void processAddedProductCategory(TrackPayload track) {
    if (!TextUtils.isEmpty(track.properties().category())) {
      Map<String, String> userAttributes = KahunaAnalytics.getUserAttributes();
      userAttributes.put(LAST_PRODUCT_ADDED_TO_CART_CATEGORY, track.properties().category());
      KahunaAnalytics.setUserAttributes(userAttributes);
    }
  }

  private void processAddedProductName(TrackPayload track) {
    if (!TextUtils.isEmpty(track.properties().name())) {
      Map<String, String> userAttributes = KahunaAnalytics.getUserAttributes();
      userAttributes.put(LAST_PRODUCT_ADDED_TO_CART_NAME, track.properties().name());
      KahunaAnalytics.setUserAttributes(userAttributes);
    }
  }

  private void processViewedProductName(TrackPayload track) {
    if (!TextUtils.isEmpty(track.properties().name())) {
      Map<String, String> userAttributes = KahunaAnalytics.getUserAttributes();
      userAttributes.put(LAST_PRODUCT_VIEWED_NAME, track.properties().name());
      KahunaAnalytics.setUserAttributes(userAttributes);
    }
  }

  private void processProductCategory(TrackPayload track) {

    if (!TextUtils.isEmpty(track.properties().category())) {

      Set<String> kahunaCategoriesSet
              = Collections.newSetFromMap(new LinkedHashMap<String, Boolean>() {
        protected boolean removeEldestEntry(Entry<String, Boolean> eldest) {
          return size() > MAX_CATEGORIES_VIEWED_ENTRIES;
        }
      });

      if (KahunaAnalytics.getUserAttributes().containsKey(CATEGORIES_VIEWED)) {
        String serializedCategories = KahunaAnalytics.getUserAttributes().get(CATEGORIES_VIEWED);
        kahunaCategoriesSet.addAll(Arrays.asList(serializedCategories.split(",")));
        kahunaCategoriesSet.remove(NONE);
      }

      kahunaCategoriesSet.add(track.properties().category());
      Map<String, String> userAttributes = KahunaAnalytics.getUserAttributes();
      userAttributes.put(CATEGORIES_VIEWED, TextUtils.join(",", kahunaCategoriesSet));
      userAttributes.put(LAST_VIEWED_CATEGORY, track.properties().category());
      KahunaAnalytics.setUserAttributes(userAttributes);

    } else {

      Map<String, String> userAttributes = KahunaAnalytics.getUserAttributes();
      userAttributes.put(LAST_VIEWED_CATEGORY, NONE);
      KahunaAnalytics.setUserAttributes(userAttributes);

    }
  }
}
