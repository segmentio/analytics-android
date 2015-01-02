package com.segment.analytics;

import android.app.Activity;
import android.os.Bundle;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import java.util.Random;
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

import static com.segment.analytics.TestUtils.AliasPayloadBuilder;
import static com.segment.analytics.TestUtils.GroupPayloadBuilder;
import static com.segment.analytics.TestUtils.IdentifyPayloadBuilder;
import static com.segment.analytics.TestUtils.JSONObjectMatcher.jsonEq;
import static com.segment.analytics.TestUtils.ScreenPayloadBuilder;
import static com.segment.analytics.TestUtils.TrackPayloadBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(MixpanelAPI.class)
public class MixpanelTest extends AbstractIntegrationTestCase {
  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock MixpanelAPI mixpanelAPI;
  @Mock MixpanelAPI.People people;
  MixpanelIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    mockStatic(MixpanelAPI.class);
    integration = new MixpanelIntegration();
    when(mixpanelAPI.getPeople()).thenReturn(people);
    integration.mixpanelAPI = mixpanelAPI;
    integration.isPeopleEnabled = false;
  }

  @Test public void initialize() throws IllegalStateException {
    MixpanelIntegration adapter = new MixpanelIntegration();
    adapter.initialize(context, new ValueMap().putValue("token", "foo")
        .putValue("trackAllPages", true)
        .putValue("trackCategorizedPages", false)
        .putValue("trackNamedPages", true), true);
    verifyStatic();
    MixpanelAPI.getInstance(context, "foo");
    assertThat(adapter.token).isEqualTo("foo");
    assertThat(adapter.trackAllPages).isTrue();
    assertThat(adapter.trackCategorizedPages).isFalse();
    assertThat(adapter.trackNamedPages).isTrue();
  }

  @Test @Override public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.token = "foo";
    integration.onActivityCreated(activity, bundle);
    verifyStatic();
    MixpanelAPI.getInstance(activity, "foo");
    verifyNoMoreMixpanelInteractions();
  }

  @Test @Override public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test @Override public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test @Override public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test @Override public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test @Override public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreMixpanelInteractions();
  }

  @Test @Override public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test @Override public void screen() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = false;

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screenAllPages() {
    integration.trackAllPages = true;
    integration.trackCategorizedPages = new Random().nextBoolean();
    integration.trackNamedPages = new Random().nextBoolean();

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

  @Test @Override public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verify(mixpanelAPI).track(eq("foo"), jsonEq(new JSONObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test @Override public void alias() {
    integration.alias(
        new AliasPayloadBuilder().traits(new Traits().putUserId("foo")).previousId("bar").build());
    verify(mixpanelAPI).alias("foo", "bar");
    verifyNoMoreMixpanelInteractions();
  }

  @Test @Override public void identify() {
    Traits traits = new Traits().putUserId("foo");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(mixpanelAPI).identify("foo");
    verify(mixpanelAPI).registerSuperProperties(jsonEq(traits.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void identifyWithPeople() {
    integration.isPeopleEnabled = true;
    Traits traits = new Traits().putUserId("foo");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(mixpanelAPI).identify("foo");
    verify(mixpanelAPI).registerSuperProperties(jsonEq(traits.toJsonObject()));
    verify(mixpanelAPI).getPeople();
    verify(people).identify("foo");
    verify(people).set(jsonEq(traits.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test @Override public void group() {
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
    verify(mixpanelAPI).getPeople();
    verify(people).trackCharge(eq(20.0), jsonEq(properties.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test @Override public void flush() {
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
