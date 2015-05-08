package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.text.TextUtils;
import com.kahuna.sdk.KahunaAnalytics;
import com.segment.analytics.Analytics;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.kahuna.sdk.KahunaUserCredentialKeys.EMAIL_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.FACEBOOK_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.GOOGLE_PLUS_ID;
import static com.kahuna.sdk.KahunaUserCredentialKeys.INSTALL_TOKEN_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.LINKEDIN_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.TWITTER_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.USERNAME_KEY;
import static com.kahuna.sdk.KahunaUserCredentialKeys.USER_ID_KEY;
import static com.segment.analytics.TestUtils.createTraits;
import static com.segment.analytics.internal.AbstractIntegration.ADDED_PRODUCT;
import static com.segment.analytics.internal.AbstractIntegration.COMPLETED_ORDER;
import static com.segment.analytics.internal.AbstractIntegration.VIEWED_PRODUCT;
import static com.segment.analytics.internal.AbstractIntegration.VIEWED_PRODUCT_CATEGORY;
import static com.segment.analytics.internal.integrations.KahunaIntegration.CATEGORIES_VIEWED;
import static com.segment.analytics.internal.integrations.KahunaIntegration.LAST_PRODUCT_ADDED_TO_CART_CATEGORY;
import static com.segment.analytics.internal.integrations.KahunaIntegration.LAST_PRODUCT_ADDED_TO_CART_NAME;
import static com.segment.analytics.internal.integrations.KahunaIntegration.LAST_PRODUCT_VIEWED_NAME;
import static com.segment.analytics.internal.integrations.KahunaIntegration.LAST_PURCHASE_DISCOUNT;
import static com.segment.analytics.internal.integrations.KahunaIntegration.LAST_VIEWED_CATEGORY;
import static com.segment.analytics.internal.integrations.KahunaIntegration.NONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
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
  @Mock Analytics analytics;
  KahunaIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(KahunaAnalytics.class);
    integration = new KahunaIntegration();
  }

  @Test public void initialize() throws IllegalStateException {
    when(analytics.getApplication()).thenReturn(context);

    integration.initialize(analytics, new ValueMap() //
        .putValue("apiKey", "foo") //
        .putValue("pushSenderId", "bar"));

    verifyStatic();
    KahunaAnalytics.onAppCreate(context, "foo", "bar");
    assertThat(integration.trackAllPages).isFalse();
  }

  @Test public void initializeWithArgs() throws IllegalStateException {
    when(analytics.getApplication()).thenReturn(context);

    integration.initialize(analytics, new ValueMap() //
        .putValue("trackAllPages", true).putValue("apiKey", "foo") //
        .putValue("pushSenderId", "bar"));

    verifyStatic();
    KahunaAnalytics.onAppCreate(context, "foo", "bar");
    assertThat(integration.trackAllPages).isTrue();
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
  }

  @Test public void trackWithQuantityAndRevenue() {
    integration.track(new TrackPayloadBuilder().event("bar")
        .properties(new Properties().putValue("quantity", 3).putRevenue(10))
        .build());

    verifyStatic();
    KahunaAnalytics.trackEvent("bar", 3, 1000);
  }

  @Test public void trackViewedProductCategoryCalled() {
    final AtomicInteger called = new AtomicInteger();
    integration = new KahunaIntegration() {
      @Override void trackViewedProductCategory(TrackPayload track) {
        super.trackViewedProductCategory(track);
        called.incrementAndGet();
      }
    };

    integration.track(new TrackPayloadBuilder().event(VIEWED_PRODUCT_CATEGORY).build());
    assertThat(called.get()).isEqualTo(1);

    integration.track(new TrackPayloadBuilder().event(VIEWED_PRODUCT).build());
    assertThat(called.get()).isEqualTo(2);

    integration.track(new TrackPayloadBuilder().event(ADDED_PRODUCT).build());
    assertThat(called.get()).isEqualTo(2);

    integration.track(new TrackPayloadBuilder().event(COMPLETED_ORDER).build());
    assertThat(called.get()).isEqualTo(2);
  }

  @Test public void trackViewedProductCategoryWithoutCategory() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(KahunaAnalytics.getUserAttributes()).thenReturn(map);

    integration.trackViewedProductCategory(new TrackPayloadBuilder() //
        .event(VIEWED_PRODUCT_CATEGORY).build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(CATEGORIES_VIEWED, NONE);
    expectedAttributes.put(LAST_VIEWED_CATEGORY, NONE);

    verifyStatic();
    KahunaAnalytics.getUserAttributes();
    verifyStatic();
    KahunaAnalytics.setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void trackViewedProductCategoryWithCategory() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(KahunaAnalytics.getUserAttributes()).thenReturn(map);

    integration.trackViewedProductCategory(new TrackPayloadBuilder().event(VIEWED_PRODUCT_CATEGORY)
        .properties(new Properties().putCategory("foo"))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(CATEGORIES_VIEWED, "foo");
    expectedAttributes.put(LAST_VIEWED_CATEGORY, "foo");

    verifyStatic();
    KahunaAnalytics.getUserAttributes();
    verifyStatic();
    KahunaAnalytics.setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void trackViewedProductCategoryWithPreviouslyViewedCategory() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put(CATEGORIES_VIEWED, NONE);
    PowerMockito.when(KahunaAnalytics.getUserAttributes()).thenReturn(map);

    integration.trackViewedProductCategory(new TrackPayloadBuilder().event(VIEWED_PRODUCT_CATEGORY)
        .properties(new Properties().putCategory("foo"))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(CATEGORIES_VIEWED, "None,foo");
    expectedAttributes.put(LAST_VIEWED_CATEGORY, "foo");

    verifyStatic();
    KahunaAnalytics.getUserAttributes();
    verifyStatic();
    KahunaAnalytics.setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void trackViewedProductCategoryWithPreviouslyViewedCategoryMax() {
    List<Integer> list = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      list.add(i);
    }

    Map<String, String> map = new LinkedHashMap<>();
    map.put(CATEGORIES_VIEWED, TextUtils.join(",", list));
    PowerMockito.when(KahunaAnalytics.getUserAttributes()).thenReturn(map);

    integration.trackViewedProductCategory(new TrackPayloadBuilder() //
        .event(VIEWED_PRODUCT_CATEGORY) //
        .properties(new Properties().putCategory("51")) //
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    // the '1' is removed
    expectedAttributes.put(CATEGORIES_VIEWED,
        "2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34"
            + ",35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51");
    expectedAttributes.put(LAST_VIEWED_CATEGORY, "51");

    verifyStatic();
    KahunaAnalytics.getUserAttributes();
    verifyStatic();
    KahunaAnalytics.setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void trackViewedProductCalled() {
    final AtomicInteger called = new AtomicInteger();
    integration = new KahunaIntegration() {
      @Override void trackViewedProduct(TrackPayload track) {
        super.trackViewedProduct(track);
        called.incrementAndGet();
      }
    };

    integration.track(new TrackPayloadBuilder().event(VIEWED_PRODUCT_CATEGORY).build());
    assertThat(called.get()).isEqualTo(0);

    integration.track(new TrackPayloadBuilder().event(VIEWED_PRODUCT).build());
    assertThat(called.get()).isEqualTo(1);

    integration.track(new TrackPayloadBuilder().event(ADDED_PRODUCT).build());
    assertThat(called.get()).isEqualTo(1);

    integration.track(new TrackPayloadBuilder().event(COMPLETED_ORDER).build());
    assertThat(called.get()).isEqualTo(1);
  }

  @Test public void trackViewedProductWithoutName() {
    integration.trackViewedProduct(new TrackPayloadBuilder().event(VIEWED_PRODUCT).build());

    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void trackViewedProductWithName() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(KahunaAnalytics.getUserAttributes()).thenReturn(map);

    integration.trackViewedProduct(new TrackPayloadBuilder().event(VIEWED_PRODUCT)
        .properties(new Properties().putName("foo"))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(LAST_PRODUCT_VIEWED_NAME, "foo");

    verifyStatic();
    KahunaAnalytics.getUserAttributes();
    verifyStatic();
    KahunaAnalytics.setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void trackAddedProductCalled() {
    final AtomicInteger called = new AtomicInteger();
    integration = new KahunaIntegration() {
      @Override void trackAddedProduct(TrackPayload track) {
        super.trackAddedProduct(track);
        called.incrementAndGet();
      }
    };

    integration.track(new TrackPayloadBuilder().event(VIEWED_PRODUCT_CATEGORY).build());
    assertThat(called.get()).isEqualTo(0);

    integration.track(new TrackPayloadBuilder().event(VIEWED_PRODUCT).build());
    assertThat(called.get()).isEqualTo(0);

    integration.track(new TrackPayloadBuilder().event(ADDED_PRODUCT).build());
    assertThat(called.get()).isEqualTo(1);

    integration.track(new TrackPayloadBuilder().event(COMPLETED_ORDER).build());
    assertThat(called.get()).isEqualTo(1);
  }

  @Test public void trackAddedProductWithoutName() {
    integration.trackViewedProduct(new TrackPayloadBuilder().event(ADDED_PRODUCT).build());

    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void trackAddedProductWithName() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(KahunaAnalytics.getUserAttributes()).thenReturn(map);

    integration.trackAddedProduct(new TrackPayloadBuilder().event(ADDED_PRODUCT)
        .properties(new Properties().putName("foo"))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(LAST_PRODUCT_ADDED_TO_CART_NAME, "foo");

    verifyStatic();
    KahunaAnalytics.getUserAttributes();
    verifyStatic();
    KahunaAnalytics.setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void trackAddedProductCategoryCalled() {
    final AtomicInteger called = new AtomicInteger();
    integration = new KahunaIntegration() {
      @Override void trackAddedProductCategory(TrackPayload track) {
        super.trackAddedProductCategory(track);
        called.incrementAndGet();
      }
    };

    integration.track(new TrackPayloadBuilder().event(VIEWED_PRODUCT_CATEGORY).build());
    assertThat(called.get()).isEqualTo(0);

    integration.track(new TrackPayloadBuilder().event(VIEWED_PRODUCT).build());
    assertThat(called.get()).isEqualTo(0);

    integration.track(new TrackPayloadBuilder().event(ADDED_PRODUCT).build());
    assertThat(called.get()).isEqualTo(1);

    integration.track(new TrackPayloadBuilder().event(COMPLETED_ORDER).build());
    assertThat(called.get()).isEqualTo(1);
  }

  @Test public void trackAddedProductCategoryWithoutCategory() {
    integration.trackAddedProductCategory(new TrackPayloadBuilder().event(ADDED_PRODUCT).build());

    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void trackAddedProductCategoryWithCategory() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(KahunaAnalytics.getUserAttributes()).thenReturn(map);

    integration.trackAddedProductCategory(new TrackPayloadBuilder().event(ADDED_PRODUCT)
        .properties(new Properties().putCategory("foo"))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(LAST_PRODUCT_ADDED_TO_CART_CATEGORY, "foo");

    verifyStatic();
    KahunaAnalytics.getUserAttributes();
    verifyStatic();
    KahunaAnalytics.setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void trackCompletedOrderCalled() {
    final AtomicInteger called = new AtomicInteger();
    integration = new KahunaIntegration() {
      @Override void trackCompletedOrder(TrackPayload track) {
        super.trackCompletedOrder(track);
        called.incrementAndGet();
      }
    };

    integration.track(new TrackPayloadBuilder().event(VIEWED_PRODUCT_CATEGORY).build());
    assertThat(called.get()).isEqualTo(0);

    integration.track(new TrackPayloadBuilder().event(VIEWED_PRODUCT).build());
    assertThat(called.get()).isEqualTo(0);

    integration.track(new TrackPayloadBuilder().event(ADDED_PRODUCT).build());
    assertThat(called.get()).isEqualTo(0);

    integration.track(new TrackPayloadBuilder().event(COMPLETED_ORDER).build());
    assertThat(called.get()).isEqualTo(1);
  }

  @Test public void trackCompletedOrder() {
    integration.trackCompletedOrder(new TrackPayloadBuilder().event(COMPLETED_ORDER)
        .properties(new Properties().putDiscount(10))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(LAST_PURCHASE_DISCOUNT, String.valueOf(10.0));

    verifyStatic();
    KahunaAnalytics.getUserAttributes();
    verifyStatic();
    KahunaAnalytics.setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().build());
    verifyNoMoreInteractions(KahunaAnalytics.class);
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
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(KahunaAnalytics.getUserAttributes()).thenReturn(map);

    integration.identify(new IdentifyPayloadBuilder().traits(createTraits("foo")).build());

    verifyStatic();
    KahunaAnalytics.getUserAttributes();
    verifyStatic();
    KahunaAnalytics.setUserCredential(USER_ID_KEY, "foo");
    verifyStatic();
    //noinspection Convert2Diamond
    KahunaAnalytics.setUserAttributes(new LinkedHashMap<String, String>());
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void identifyWithSocialAttributes() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(KahunaAnalytics.getUserAttributes()).thenReturn(map);
    Traits traits = new Traits() //
        .putUsername("foo")
        .putEmail("bar")
        .putValue("fbid", "baz")
        .putValue("twtr", "qux")
        .putValue("lnk", "quux")
        .putValue("install_token", "foobar")
        .putValue("gplus_id", "foobaz")
        .putValue("non_kahuna_credential", "foobarqazqux");

    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());

    verifyStatic();
    KahunaAnalytics.getUserAttributes();
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
    verifyStatic();
    KahunaAnalytics.setUserCredential(INSTALL_TOKEN_KEY, "foobar");
    verifyStatic();
    KahunaAnalytics.setUserCredential(GOOGLE_PLUS_ID, "foobaz");
    verifyStatic();
    KahunaAnalytics.setUserAttributes(new ValueMap() //
        .putValue("non_kahuna_credential", "foobarqazqux").toStringMap());
    verifyNoMoreInteractions(KahunaAnalytics.class);
  }

  @Test public void reset() {
    integration.reset();

    verifyStatic();
    KahunaAnalytics.logout();
  }
}
