package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.text.TextUtils;
import com.kahuna.sdk.EmptyCredentialsException;
import com.kahuna.sdk.IKahuna;
import com.kahuna.sdk.IKahunaUserCredentials;
import com.kahuna.sdk.Kahuna;
import com.segment.analytics.Analytics;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.integrations.kahuna.BuildConfig;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.Utils;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.kahuna.sdk.KahunaUserCredentials.EMAIL_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.FACEBOOK_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.GOOGLE_PLUS_ID;
import static com.kahuna.sdk.KahunaUserCredentials.INSTALL_TOKEN_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.LINKEDIN_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.TWITTER_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.USERNAME_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.USER_ID_KEY;
import static com.segment.analytics.Analytics.LogLevel;
import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.internal.Utils.error;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.isOnClassPath;

/**
 * Kahuna helps mobile marketers send push notifications and in-app messages.
 *
 * @see <a href="https://www.kahuna.com/">Kahuna</a>
 * @see <a href="https://segment.com/docs/integrations/kahuna/">Kahuna Integration</a>
 * @see <a href="http://app.usekahuna.com/tap/getstarted/android/">Kahuna Android SDK</a>
 */
public class KahunaIntegration extends AbstractIntegration<Void> {
  static final String CATEGORIES_VIEWED = "Categories Viewed";
  static final String LAST_VIEWED_CATEGORY = "Last Viewed Category";
  static final String LAST_PRODUCT_VIEWED_NAME = "Last Product Viewed Name";
  static final String LAST_PRODUCT_ADDED_TO_CART_NAME = "Last Product Added To Cart Name";
  static final String LAST_PRODUCT_ADDED_TO_CART_CATEGORY = "Last Product Added To Cart Category";
  static final String LAST_PURCHASE_DISCOUNT = "Last Purchase Discount";
  static final int MAX_CATEGORIES_VIEWED_ENTRIES = 50;
  static final String NONE = "None";
  static final String KAHUNA_KEY = "Kahuna";
  static final String SEGMENT_WRAPPER_VERSION = "segment";
  static final Set<String> KAHUNA_CREDENTIALS =
      Utils.newSet(USERNAME_KEY, EMAIL_KEY, FACEBOOK_KEY, TWITTER_KEY, LINKEDIN_KEY,
          INSTALL_TOKEN_KEY, GOOGLE_PLUS_ID);
  private static final String SEGMENT_USER_ID_KEY = "userId";

  boolean trackAllPages;
  IKahuna kahuna;

  @Override public void initialize(Analytics analytics, ValueMap settings)
      throws IllegalStateException {
    if (!isOnClassPath("android.support.v4.app.Fragment")) {
      throw new IllegalStateException("Kahuna requires the support library to be bundled.");
    }

    trackAllPages = settings.getBoolean("trackAllPages", false);
    String apiKey = settings.getString("apiKey");
    String pushSenderId = settings.getString("pushSenderId");
    kahuna = Kahuna.getInstance();
    kahuna.onAppCreate(analytics.getApplication(), apiKey, pushSenderId);
    kahuna.setHybridSDKVersion(SEGMENT_WRAPPER_VERSION, BuildConfig.VERSION_NAME);

    LogLevel logLevel = analytics.getLogLevel();
    kahuna.setDebugMode(logLevel == INFO || logLevel == VERBOSE);
  }

  @Override public String key() {
    return KAHUNA_KEY;
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    kahuna.start();
  }

  @Override public void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    kahuna.stop();
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);

    Traits traits = identify.traits();
    IKahunaUserCredentials credentials = kahuna.createUserCredentials();
    Map<String, String> userAttributes = kahuna.getUserAttributes();
    for (String key : traits.keySet()) {
      if (KAHUNA_CREDENTIALS.contains(key)) {
        // Only set credentials if it is a key recognized by Kahuna
        credentials.add(key, traits.getString(key));
      } else if (SEGMENT_USER_ID_KEY.equals(key)) {
        credentials.add(USER_ID_KEY, identify.userId());
      } else {
        // Set it as a user attribute otherwise
        Object value = traits.get(key);
        if (value instanceof Date) {
          userAttributes.put(key, Utils.toISO8601Date((Date) value));
        } else {
          userAttributes.put(key, String.valueOf(value));
        }
      }
    }
    try {
      kahuna.login(credentials);
    } catch (EmptyCredentialsException e) {
      error(e, "You should call reset() instead of passed in all empty/null values to identify().");
    }
    kahuna.setUserAttributes(userAttributes);
  }

  @Override public void track(TrackPayload track) {
    super.track(track);

    String event = track.event();
    if (VIEWED_PRODUCT_CATEGORY.equalsIgnoreCase(event)) {
      trackViewedProductCategory(track);
    } else if (VIEWED_PRODUCT.equalsIgnoreCase(event)) {
      trackViewedProduct(track);
      trackViewedProductCategory(track);
    } else if (ADDED_PRODUCT.equalsIgnoreCase(event)) {
      trackAddedProduct(track);
      trackAddedProductCategory(track);
    } else if (COMPLETED_ORDER.equalsIgnoreCase(event)) {
      trackCompletedOrder(track);
    }

    int quantity = track.properties().getInt("quantity", -1);
    double revenue = track.properties().revenue();
    if (quantity == -1 && revenue == 0) {
      // Kahuna requires revenue in cents
      kahuna.trackEvent(event);
    } else {
      kahuna.trackEvent(event, quantity, (int) (revenue * 100));
    }
  }

  void trackViewedProductCategory(TrackPayload track) {
    String category = track.properties().category();

    if (isNullOrEmpty(category)) {
      category = NONE;
    }

    Map<String, String> userAttributes = kahuna.getUserAttributes();
    Set<String> categoriesViewed;
    if (userAttributes.containsKey(CATEGORIES_VIEWED)) {
      categoriesViewed = Collections.newSetFromMap(new LinkedHashMap<String, Boolean>() {
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
          return size() > MAX_CATEGORIES_VIEWED_ENTRIES;
        }
      });
      String serializedCategories = userAttributes.get(CATEGORIES_VIEWED);
      String[] categories = serializedCategories.split(",");
      categoriesViewed.addAll(Arrays.asList(categories));
    } else {
      categoriesViewed = new HashSet<>();
    }
    categoriesViewed.add(category);

    userAttributes.put(CATEGORIES_VIEWED, TextUtils.join(",", categoriesViewed));
    userAttributes.put(LAST_VIEWED_CATEGORY, category);
    kahuna.setUserAttributes(userAttributes);
  }

  void trackViewedProduct(TrackPayload track) {
    String name = track.properties().name();
    if (!isNullOrEmpty(name)) {
      Map<String, String> userAttributes = kahuna.getUserAttributes();
      userAttributes.put(LAST_PRODUCT_VIEWED_NAME, name);
      kahuna.setUserAttributes(userAttributes);
    }
  }

  void trackAddedProduct(TrackPayload track) {
    String name = track.properties().name();
    if (!isNullOrEmpty(name)) {
      Map<String, String> userAttributes = kahuna.getUserAttributes();
      userAttributes.put(LAST_PRODUCT_ADDED_TO_CART_NAME, name);
      kahuna.setUserAttributes(userAttributes);
    }
  }

  void trackAddedProductCategory(TrackPayload track) {
    String category = track.properties().category();
    if (!isNullOrEmpty(category)) {
      Map<String, String> userAttributes = kahuna.getUserAttributes();
      userAttributes.put(LAST_PRODUCT_ADDED_TO_CART_CATEGORY, category);
      kahuna.setUserAttributes(userAttributes);
    }
  }

  void trackCompletedOrder(TrackPayload track) {
    double discount = track.properties().discount();
    Map<String, String> userAttributes = kahuna.getUserAttributes();
    userAttributes.put(LAST_PURCHASE_DISCOUNT, String.valueOf(discount));
    kahuna.setUserAttributes(userAttributes);
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);

    if (trackAllPages) {
      kahuna.trackEvent(String.format(VIEWED_EVENT_FORMAT, screen.event()));
    }
  }

  @Override public void reset() {
    super.reset();

    kahuna.logout();
  }
}
