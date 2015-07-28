package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.text.TextUtils;

import com.kahuna.sdk.EmptyCredentialsException;
import com.kahuna.sdk.IKahuna;
import com.kahuna.sdk.IKahunaUserCredentials;
import com.kahuna.sdk.Kahuna;
import com.kahuna.sdk.KahunaCommon;
import com.kahuna.sdk.KahunaUserCredentials;
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

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.mockito.expectation.*;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.kahuna.sdk.KahunaUserCredentials.EMAIL_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.FACEBOOK_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.GOOGLE_PLUS_ID;
import static com.kahuna.sdk.KahunaUserCredentials.INSTALL_TOKEN_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.LINKEDIN_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.TWITTER_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.USERNAME_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.USER_ID_KEY;
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
import static com.segment.analytics.internal.integrations.kahuna.BuildConfig.VERSION_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest({Kahuna.class, KahunaCommon.class})
@SuppressStaticInitializationFor("com.kahuna.sdk.Kahuna")
public class KahunaTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Rule public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
  @Mock Application context;
  @Mock Analytics analytics;
  @Mock Kahuna kahuna;
  @Mock KahunaUserCredentials credentials;
  KahunaIntegration integration;

  @Before public void setUp() throws Exception {
    initMocks(this);
    final KahunaCommon mockedKahunaCommon = mock(KahunaCommon.class);
    PowerMockito.mockStatic(Kahuna.class);
    PowerMockito.when(Kahuna.getInstance()).thenReturn(kahuna);
    PowerMockito.when(kahuna.createUserCredentials()).thenReturn(credentials);
    integration = new KahunaIntegration();
  }

  @Test public void initialize() throws IllegalStateException {
    when(analytics.getApplication()).thenReturn(context);

    integration.initialize(analytics, new ValueMap() //
        .putValue("apiKey", "foo") //
        .putValue("pushSenderId", "bar"));

    verify(kahuna).onAppCreate(context, "foo", "bar");
    verify(kahuna).setHybridSDKVersion("segment", VERSION_NAME);
    verify(kahuna).setDebugMode(false);
    assertThat(integration.trackAllPages).isFalse();
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void initializeWithArgs() throws IllegalStateException {
    when(analytics.getApplication()).thenReturn(context);

    integration.initialize(analytics, new ValueMap() //
        .putValue("trackAllPages", true).putValue("apiKey", "foo") //
        .putValue("pushSenderId", "bar"));

    verify(kahuna).onAppCreate(context, "foo", "bar");
    verify(kahuna).setHybridSDKVersion("segment", VERSION_NAME);
    verify(kahuna).setDebugMode(false);
    assertThat(integration.trackAllPages).isTrue();
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verify(kahuna).start();
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verify(kahuna).stop();
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());

    verify(kahuna).trackEvent("foo");
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackWithQuantityAndRevenue() {
    integration.track(new TrackPayloadBuilder().event("bar")
        .properties(new Properties().putValue("quantity", 3).putRevenue(10))
            .build());

    verify(kahuna).trackEvent("bar", 3, 1000);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackWithNoQuantityAndOnlyRevenue() {
    integration.track(new TrackPayloadBuilder().event("bar")
        .properties(new Properties().putRevenue(10))
        .build());

    verify(kahuna).trackEvent("bar", -1, 1000);
    verifyNoMoreInteractions(kahuna);
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
    PowerMockito.when(Kahuna.getInstance().getUserAttributes()).thenReturn(map);

    integration.trackViewedProductCategory(new TrackPayloadBuilder() //
        .event(VIEWED_PRODUCT_CATEGORY).build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(CATEGORIES_VIEWED, NONE);
    expectedAttributes.put(LAST_VIEWED_CATEGORY, NONE);

    Assert.assertEquals(Kahuna.getInstance().getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackViewedProductCategoryWithCategory() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(Kahuna.getInstance().getUserAttributes()).thenReturn(map);

    integration.trackViewedProductCategory(new TrackPayloadBuilder().event(VIEWED_PRODUCT_CATEGORY)
        .properties(new Properties().putCategory("foo"))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(CATEGORIES_VIEWED, "foo");
    expectedAttributes.put(LAST_VIEWED_CATEGORY, "foo");

    Assert.assertEquals(Kahuna.getInstance().getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackViewedProductCategoryWithPreviouslyViewedCategory() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put(CATEGORIES_VIEWED, NONE);
    PowerMockito.when(Kahuna.getInstance().getUserAttributes()).thenReturn(map);

    integration.trackViewedProductCategory(new TrackPayloadBuilder().event(VIEWED_PRODUCT_CATEGORY)
        .properties(new Properties().putCategory("foo"))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(CATEGORIES_VIEWED, "None,foo");
    expectedAttributes.put(LAST_VIEWED_CATEGORY, "foo");

    Assert.assertEquals(Kahuna.getInstance().getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackViewedProductCategoryWithPreviouslyViewedCategoryMax() {
    List<Integer> list = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      list.add(i);
    }

    Map<String, String> map = new LinkedHashMap<>();
    map.put(CATEGORIES_VIEWED, TextUtils.join(",", list));
    PowerMockito.when(Kahuna.getInstance().getUserAttributes()).thenReturn(map);

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

    Assert.assertEquals(Kahuna.getInstance().getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);;
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

    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void trackViewedProductWithName() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(Kahuna.getInstance().getUserAttributes()).thenReturn(map);

    integration.trackViewedProduct(new TrackPayloadBuilder().event(VIEWED_PRODUCT)
        .properties(new Properties().putName("foo"))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(LAST_PRODUCT_VIEWED_NAME, "foo");

    Assert.assertEquals(Kahuna.getInstance().getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
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

    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void trackAddedProductWithName() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(Kahuna.getInstance().getUserAttributes()).thenReturn(map);

    integration.trackAddedProduct(new TrackPayloadBuilder().event(ADDED_PRODUCT)
        .properties(new Properties().putName("foo"))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(LAST_PRODUCT_ADDED_TO_CART_NAME, "foo");

    Assert.assertEquals(Kahuna.getInstance().getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
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

    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackAddedProductCategoryWithCategory() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(Kahuna.getInstance().getUserAttributes()).thenReturn(map);

    integration.trackAddedProductCategory(new TrackPayloadBuilder().event(ADDED_PRODUCT)
        .properties(new Properties().putCategory("foo"))
            .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(LAST_PRODUCT_ADDED_TO_CART_CATEGORY, "foo");

    Assert.assertEquals(Kahuna.getInstance().getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
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
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(Kahuna.getInstance().getUserAttributes()).thenReturn(map);

    integration.trackCompletedOrder(new TrackPayloadBuilder().event(COMPLETED_ORDER)
        .properties(new Properties().putDiscount(10))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(LAST_PURCHASE_DISCOUNT, String.valueOf(10.0));

    Assert.assertEquals(Kahuna.getInstance().getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().build());
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void flush() {
    integration.flush();
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void identify() throws EmptyCredentialsException {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(Kahuna.getInstance().getUserAttributes()).thenReturn(map);

    integration.identify(new IdentifyPayloadBuilder().traits(createTraits("foo")).build());

    verify(credentials).add(USER_ID_KEY, "foo");
    verify(kahuna).createUserCredentials();
    verify(kahuna).getUserAttributes();
    verify(kahuna).login(credentials);
    verify(kahuna).setUserAttributes(map);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void identifyWithSocialAttributes() throws EmptyCredentialsException {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(Kahuna.getInstance().getUserAttributes()).thenReturn(map);
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

    Map<String, String> expectedAttributes = new ValueMap() //
        .putValue("non_kahuna_credential", "foobarqazqux").toStringMap();
    Assert.assertEquals(Kahuna.getInstance().getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verify(kahuna).setUserAttributes(expectedAttributes);

    verify(credentials).add(USERNAME_KEY, "foo");
    verify(credentials).add(EMAIL_KEY, "bar");
    verify(credentials).add(FACEBOOK_KEY, "baz");
    verify(credentials).add(TWITTER_KEY, "qux");
    verify(credentials).add(LINKEDIN_KEY, "quux");
    verify(credentials).add(INSTALL_TOKEN_KEY, "foobar");
    verify(credentials).add(GOOGLE_PLUS_ID, "foobaz");
    verify(kahuna).createUserCredentials();
    verify(kahuna).login(credentials);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void reset() {
    integration.reset();

    verify(kahuna).logout();
    verifyNoMoreInteractions(kahuna);
  }
}
