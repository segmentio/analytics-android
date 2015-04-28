package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.kahuna.sdk.KahunaAnalytics;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
//import org.mockito.Mockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static com.kahuna.sdk.KahunaUserCredentialKeys.EMAIL_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.FACEBOOK_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.LINKEDIN_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.TWITTER_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.USERNAME_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.USER_ID_KEY;
import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.TestUtils.createTraits;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(KahunaAnalytics.class)
public class KahunaTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
  @Mock Application context;
  KahunaIntegration integration;

  // Segment constants (from KahunaInteation.java)
  private static final String SEG_VIEWED_PRODUCT_CATEGORY = "Viewed Product Category";
  private static final String SEG_VIEWED_PRODUCT = "Viewed Product";
  private static final String SEG_ADDED_PRODUCT = "Added Product";
  private static final String SEG_COMPLETED_ORDER = "Completed Order";
  private static final String SEG_DISCOUNT_KEY = "discount";
  private static final String SEG_NAME_KEY = "name";
  private static final String SEG_CATEGORY_KEY = "category";

  // Kahuna constants
  private static final String CATEGORIES_VIEWED = "Categories Viewed";
  private static final String LAST_VIEWED_CATEGORY = "Last Viewed Category";
  private static final String LAST_PRODUCT_VIEWED_NAME = "Last Product Viewed Name";
  private static final String LAST_PRODUCT_VIEWED_ID = "Last Product Viewed Id";
  private static final String LAST_PRODUCT_ADDED_TO_CART_NAME = "Last Product Added To Cart Name";
  private static final String LAST_PRODUCT_ADDED_TO_CART_CATEGORY          = "Last Product Added To Cart Category";
  private static final String LAST_PURCHASE_DISCOUNT = "Last Purchase Discount";
  private static final int MAX_CATEGORIES_VIEWED_ENTRIES = 50;
  private static final String STRING_NONE = "None";

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(KahunaAnalytics.class);
    integration = new KahunaIntegration();
  }

  @Test public void initialize() throws IllegalStateException {
    integration.initialize(context,
            new ValueMap().putValue("apiKey", "foo").putValue("pushSenderId", "bar"), NONE);

    verifyStatic();
    KahunaAnalytics.onAppCreate(context, "foo", "bar");
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyStatic();
    KahunaAnalytics.start();
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyStatic();
    KahunaAnalytics.stop();
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verifyStatic();
    KahunaAnalytics.trackEvent("foo");

    integration.track(new TrackPayloadBuilder().event("bar")
            .properties(new Properties().putValue("quantity", 3).putRevenue(10))
            .build());
    verifyStatic();
    KahunaAnalytics.trackEvent("bar", 3, 1000);
  }

  @Test public void testProcess_ViewedProductCategory_noProperties() {
    integration.track(new TrackPayloadBuilder().event(SEG_VIEWED_PRODUCT_CATEGORY).build());
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(LAST_VIEWED_CATEGORY, STRING_NONE);
    expectedMap.put(CATEGORIES_VIEWED, STRING_NONE);
    verifyStatic();
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(expectedMap)));
  }

  @Test public void testProcess_ViewedProductCategory_withCategory_once() {
    Properties properties = new Properties();
    properties.put(SEG_CATEGORY_KEY, "shoes");
    integration.track(new TrackPayloadBuilder().event(SEG_VIEWED_PRODUCT_CATEGORY)
            .properties(properties).build());
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(LAST_VIEWED_CATEGORY, "shoes");
    expectedMap.put(CATEGORIES_VIEWED, "shoes");
    verifyStatic();
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(expectedMap)));
  }

  @Test public void testProcess_ViewedProductCategory_withCategory_twice() {
    Properties properties = new Properties();
    properties.put(SEG_CATEGORY_KEY, "shoes");
    integration.track(new TrackPayloadBuilder().event(SEG_VIEWED_PRODUCT_CATEGORY)
            .properties(properties).build());

    verifyStatic();
    Map<String, String> expectedMap1 = new HashMap<>();
    expectedMap1.put(LAST_VIEWED_CATEGORY, "shoes");
    expectedMap1.put(CATEGORIES_VIEWED, "shoes");
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(expectedMap1)));

    mockStatic(KahunaAnalytics.class);
    Mockito.when(KahunaAnalytics.getUserAttributes()).thenReturn(expectedMap1);
    Properties properties2 = new Properties();
    properties2.put(SEG_CATEGORY_KEY, "cars");
    integration.track(new TrackPayloadBuilder().event(SEG_VIEWED_PRODUCT_CATEGORY)
            .properties(properties2).build());
    verifyStatic();
    Map<String, String> expectedMap2 = new HashMap<>();
    expectedMap2.put(LAST_VIEWED_CATEGORY, "cars");
    expectedMap2.put(CATEGORIES_VIEWED, "shoes,cars");
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(expectedMap2)));
  }

  @Test public void testProcess_CompletedOrder_noProperties() {
    integration.track(new TrackPayloadBuilder().event(SEG_COMPLETED_ORDER).build());
    verifyStatic();
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(
                    LAST_PURCHASE_DISCOUNT, "0")));
  }

  @Test public void testProcess_CompletedOrder_HasDiscount() {
    Properties properties = new Properties();
    properties.put(SEG_DISCOUNT_KEY, "2.5");
    integration.track(new TrackPayloadBuilder().event(SEG_COMPLETED_ORDER)
            .properties(properties).build());
    verifyStatic();
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(
                    LAST_PURCHASE_DISCOUNT, "2.5")));
  }

  @Test public void testProcess_AddedProduct_noName_noCategory() {
    integration.track(new TrackPayloadBuilder().event(SEG_ADDED_PRODUCT).build());
    verifyStatic(never());
    KahunaAnalytics.setUserAttributes(anyMap());
  }

  @Test public void testProcess_AddedProduct_noName_withCategory() {
    Properties properties = new Properties();
    properties.put(SEG_CATEGORY_KEY, "shoes");
    integration.track(new TrackPayloadBuilder().event(SEG_ADDED_PRODUCT)
            .properties(properties).build());
    verifyStatic();
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(
                    LAST_PRODUCT_ADDED_TO_CART_CATEGORY, "shoes")));
  }

  @Test public void testProcess_AddedProduct_withName_noCategory() {
    Properties properties = new Properties();
    properties.put(SEG_NAME_KEY, "Tesla Model S");
    integration.track(new TrackPayloadBuilder().event(SEG_ADDED_PRODUCT)
            .properties(properties).build());
    verifyStatic();
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(
                    LAST_PRODUCT_ADDED_TO_CART_NAME, "Tesla Model S")));
  }

  @Test public void testProcess_AddedProduct_withName_withCategory() {
    Properties properties = new Properties();
    properties.put(SEG_NAME_KEY, "Tesla Model S");
    properties.put(SEG_CATEGORY_KEY, "Electric cars");
    integration.track(new TrackPayloadBuilder().event(SEG_ADDED_PRODUCT)
            .properties(properties).build());
    verifyStatic();
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(LAST_PRODUCT_ADDED_TO_CART_NAME,
                    "Tesla Model S")));
    verifyStatic();
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(LAST_PRODUCT_ADDED_TO_CART_CATEGORY,
                    "Electric cars")));
  }

  @Test public void testProcess_ViewedProduct_noName() {
    integration.track(new TrackPayloadBuilder().event(SEG_VIEWED_PRODUCT).build());
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(LAST_VIEWED_CATEGORY, STRING_NONE);
    expectedMap.put(CATEGORIES_VIEWED, STRING_NONE);
    verifyStatic();
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(expectedMap)));
  }

  @Test public void testProcess_ViewedProduct_withName_noCategory() {
    Properties properties = new Properties();
    properties.put(SEG_NAME_KEY, "Tesla Model X");
    integration.track(new TrackPayloadBuilder().event(SEG_VIEWED_PRODUCT)
            .properties(properties).build());
    verifyStatic();
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(
                    LAST_PRODUCT_VIEWED_NAME, "Tesla Model X")));
  }

  @Test public void testProcess_ViewedProduct_noName_withCategory() {
    Properties properties = new Properties();
    properties.put(SEG_CATEGORY_KEY, "Electric cars");
    integration.track(new TrackPayloadBuilder().event(SEG_VIEWED_PRODUCT)
            .properties(properties).build());
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(LAST_VIEWED_CATEGORY, "Electric cars");
    expectedMap.put(CATEGORIES_VIEWED, "Electric cars");
    verifyStatic();
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(expectedMap)));
  }

  @Test public void testProcess_ViewedProduct_withName_withCategory() {
    Properties properties = new Properties();
    properties.put(SEG_NAME_KEY, "Tesla Model X");
    properties.put(SEG_CATEGORY_KEY, "Electric cars");
    integration.track(new TrackPayloadBuilder().event(SEG_VIEWED_PRODUCT)
            .properties(properties).build());
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(LAST_PRODUCT_VIEWED_NAME, "Tesla Model X");
    expectedMap.put(LAST_VIEWED_CATEGORY, "Electric cars");
    expectedMap.put(CATEGORIES_VIEWED, "Electric cars");
    verifyStatic();
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(LAST_PRODUCT_VIEWED_NAME, "Tesla Model X")));
    KahunaAnalytics.setUserAttributes(
            argThat(new KahunaUserAttributesMatcher(expectedMap)));
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().newId("myUserId").build());
    verifyStatic();
    KahunaAnalytics.setUserCredential(USER_ID_KEY, "myUserId");
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().build());
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void screenAllPages() {
    integration.trackAllPages = true;
    integration.screen(new ScreenPayloadBuilder().name("foo").build());

    verifyStatic();
    KahunaAnalytics.trackEvent("Viewed " + "foo" + " Screen");
  }

  @Test public void flush() {
    integration.flush();
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void identify() {
    integration.identify(new IdentifyPayloadBuilder().build());
    verifyStatic();
    KahunaAnalytics.setUserCredential(USER_ID_KEY, null);

    Traits traits = createTraits("someId");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verifyStatic();
    KahunaAnalytics.setUserCredential(USER_ID_KEY, "someId");
  }

  @Test public void identifyWithSocialAttributes() {
    Traits traits = new Traits().putUsername("foo")
            .putEmail("bar")
            .putValue(FACEBOOK_KEY, "baz")
            .putValue(TWITTER_KEY, "qux")
            .putValue(LINKEDIN_KEY, "quux");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verifyStatic();
    KahunaAnalytics.setUserCredential(USERNAME_KEY, "foo");
    verifyStatic();
    KahunaAnalytics.setUserCredential(EMAIL_KEY, "bar");
    verifyStatic();
    KahunaAnalytics.setUserCredential(FACEBOOK_KEY, "baz");
    verifyStatic();
    KahunaAnalytics.setUserCredential(TWITTER_KEY, "qux");
    verifyStatic();
    KahunaAnalytics.setUserCredential(LINKEDIN_KEY, "quux");
  }

  @Test public void reset() {
    integration.reset();
    verifyStatic();
    KahunaAnalytics.logout();
  }

  class KahunaUserAttributesMatcher extends ArgumentMatcher<Map> {
    String expectedKey;
    String expectedValue;
    Map<String, String> expectedProperties;

    KahunaUserAttributesMatcher(Map<String, String> expectedProperties) {
      this.expectedProperties = expectedProperties;
    }

    KahunaUserAttributesMatcher(String expectedKey, String expectedValue) {
      this.expectedKey = expectedKey;
      this.expectedValue = expectedValue;
    }

    @Override
    public boolean matches(Object map) {
      if (expectedProperties!= null) {
        for (String expectedKey: expectedProperties.keySet()) {
          String expectedValue = expectedProperties.get(expectedKey);
          if (((Map) map).containsKey(expectedKey)
                  && ((Map) map).get(expectedKey).equals(expectedValue)) {
            continue;
          } else {
            return false;
          }
        }
        return true;
      } else {
        return ((Map) map).containsKey(expectedKey)
                && ((Map) map).get(expectedKey).equals(expectedValue);
      }
    }
  }
}
