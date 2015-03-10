package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.segment.analytics.Properties;
import com.segment.analytics.Randoms;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import java.util.Arrays;
import java.util.Collections;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Analytics.LogLevel.NONE;
import static com.segment.analytics.TestUtils.JSONObjectMatcher.jsonEq;
import static com.segment.analytics.TestUtils.createTraits;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(MixpanelAPI.class)
public class MixpanelTest {
  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock MixpanelAPI mixpanelAPI;
  @Mock Application context;
  @Mock MixpanelAPI.People people;
  MixpanelIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    mockStatic(MixpanelAPI.class);
    PowerMockito.when(MixpanelAPI.getInstance(context, "foo")).thenReturn(mixpanelAPI);
    integration = new MixpanelIntegration();
    when(mixpanelAPI.getPeople()).thenReturn(people);
    integration.mixpanelAPI = mixpanelAPI;
    integration.people = people;
    integration.isPeopleEnabled = false;
    integration.increments = Collections.emptySet();
  }

  @Test public void initialize() throws IllegalStateException {
    MixpanelIntegration integration = new MixpanelIntegration();
    integration.initialize(context, new ValueMap().putValue("token", "foo")
        .putValue("trackAllPages", true)
        .putValue("trackCategorizedPages", false)
        .putValue("trackNamedPages", true), NONE);
    verifyStatic();
    MixpanelAPI.getInstance(context, "foo");
    verify(mixpanelAPI, never()).getPeople();
    assertThat(integration.token).isEqualTo("foo");
    assertThat(integration.trackAllPages).isTrue();
    assertThat(integration.trackCategorizedPages).isFalse();
    assertThat(integration.trackNamedPages).isTrue();
    assertThat(integration.increments).isNotNull().isEmpty();
  }

  @Test public void initializeWithIncrementsAndPeople() throws IllegalStateException {
    MixpanelIntegration integration = new MixpanelIntegration();
    integration.initialize(context, new ValueMap().putValue("token", "foo")
        .putValue("people", true)
        .putValue("trackAllPages", true)
        .putValue("trackCategorizedPages", false)
        .putValue("trackNamedPages", true)
        .putValue("increments", Arrays.asList("baz", "qaz", "qux")), NONE);
    verifyStatic();
    MixpanelAPI.getInstance(context, "foo");
    verify(mixpanelAPI).getPeople();
    assertThat(integration.token).isEqualTo("foo");
    assertThat(integration.trackAllPages).isTrue();
    assertThat(integration.trackCategorizedPages).isFalse();
    assertThat(integration.trackNamedPages).isTrue();
    verify(mixpanelAPI).getPeople();
    assertThat(integration.increments).containsExactly("qux", "baz", "qaz");
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.token = "foo";
    integration.onActivityCreated(activity, bundle);
    verifyStatic();
    MixpanelAPI.getInstance(activity, "foo");
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screen() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = false;

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screenAllPages() {
    integration.trackAllPages = true;
    integration.trackCategorizedPages = Randoms.nextBoolean();
    integration.trackNamedPages = Randoms.nextBoolean();

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verify(mixpanelAPI).track(eq("Viewed foo Screen"), jsonEq(new JSONObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screenNamedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = true;

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verify(mixpanelAPI).track(eq("Viewed foo Screen"), jsonEq(new JSONObject()));
    verifyNoMoreMixpanelInteractions();

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screenCategorizedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = true;
    integration.trackNamedPages = false;

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verify(mixpanelAPI).track(eq("Viewed foo Screen"), jsonEq(new JSONObject()));
    verifyNoMoreMixpanelInteractions();

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verify(mixpanelAPI).track(eq("foo"), jsonEq(new JSONObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void trackIncrement() {
    integration.isPeopleEnabled = true;
    integration.increments = Collections.singleton("baz");

    integration.track(new TrackPayloadBuilder().event("baz").build());

    verify(people).increment("baz", 1);
    verify(people).set(eq("Last baz"), any());
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void trackIncrementWithoutPeople() {
    // Disabling people should do a regular track call
    integration.track(new TrackPayloadBuilder().event("baz").build());
    verify(mixpanelAPI).track(eq("baz"), jsonEq(new JSONObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().traits(createTraits("foo")).newId("bar") //
        .build());
    verify(mixpanelAPI).alias("bar", "foo");
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void aliasWithoutAnonymousId() {
    integration.alias(new AliasPayloadBuilder().traits(new Traits() //
        .putValue("anonymousId", "qaz")).newId("qux").build());
    verify(mixpanelAPI).alias("qux", null);
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void identify() {
    Traits traits = createTraits("foo");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(mixpanelAPI).identify("foo");
    verify(mixpanelAPI).registerSuperProperties(jsonEq(traits.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void identifyWithPeople() {
    integration.isPeopleEnabled = true;
    Traits traits = createTraits("foo");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(mixpanelAPI).identify("foo");
    verify(mixpanelAPI).registerSuperProperties(jsonEq(traits.toJsonObject()));
    verify(people).identify("foo");
    verify(people).set(jsonEq(traits.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void gidentifyWithSpecialProperties() throws JSONException {
    integration.isPeopleEnabled = true;
    Traits traits = createTraits("foo")
        .putEmail("friends@segment.com")
        .putFirstName("prateek")
        .putLastName("srivastava")
        .putPhone("1-844-611-0621")
        .putCreatedAt("15th Feb, 2015")
        .putUsername("segmentio");
    JSONObject expected = new JSONObject();
    expected.put("userId", "foo");
    expected.put("$email", "friends@segment.com");
    expected.put("$first_name", "prateek");
    expected.put("$last_name", "srivastava");
    expected.put("$name", "prateek srivastava");
    expected.put("$phone", "1-844-611-0621");
    expected.put("$username", "segmentio");
    expected.put("$created", "15th Feb, 2015");

    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(mixpanelAPI).identify("foo");
    verify(mixpanelAPI).registerSuperProperties(jsonEq(expected));
    verify(people).identify("foo");
    verify(people).set(jsonEq(expected));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void identifyWithSpecialPropertiesAsNull() throws JSONException {
    integration.isPeopleEnabled = true;
    Traits traits = createTraits("foo")
        .putFirstName(null)
        .putPhone("1-844-611-0621")
        .putUsername("segmentio");
    JSONObject expected = new JSONObject();
    expected.put("userId", "foo");
    expected.put("$phone", "1-844-611-0621");
    expected.put("$username", "segmentio");

    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(mixpanelAPI).identify("foo");
    verify(mixpanelAPI).registerSuperProperties(jsonEq(expected));
    verify(people).identify("foo");
    verify(people).set(jsonEq(expected));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void event() {
    Properties properties = new Properties().putRevenue(20);
    integration.event("foo", properties);
    verify(mixpanelAPI).track(eq("foo"), jsonEq(properties.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void eventWithPeople() {
    integration.isPeopleEnabled = true;
    Properties properties = new Properties();
    integration.isPeopleEnabled = true;
    integration.event("foo", properties);
    verify(mixpanelAPI).track(eq("foo"), jsonEq(properties.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void eventWithPeopleAndRevenue() {
    integration.isPeopleEnabled = true;
    Properties properties = new Properties().putRevenue(20);
    integration.event("foo", properties);
    verify(mixpanelAPI).track(eq("foo"), jsonEq(properties.toJsonObject()));
    verify(people).trackCharge(eq(20.0), jsonEq(properties.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void flush() {
    integration.flush();
    verify(mixpanelAPI).flush();
    verifyNoMoreMixpanelInteractions();
  }

  private void verifyNoMoreMixpanelInteractions() {
    PowerMockito.verifyNoMoreInteractions(MixpanelAPI.class);
    verifyNoMoreInteractions(mixpanelAPI);
    verifyNoMoreInteractions(people);
  }
}
