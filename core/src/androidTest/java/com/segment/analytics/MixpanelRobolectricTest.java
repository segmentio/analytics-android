package com.segment.analytics;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import java.util.Random;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.Mock;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(MixpanelAPI.class)
public class MixpanelRobolectricTest extends IntegrationRobolectricExam {
  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock MixpanelAPI mixpanelAPI;
  @Mock MixpanelAPI.People people;
  MixpanelIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    doReturn(people).when(mixpanelAPI).getPeople();

    PowerMockito.mockStatic(MixpanelAPI.class);
    integration = new MixpanelIntegration();
    integration.mixpanelAPI = mixpanelAPI;
  }

  @Test public void initialize() throws IllegalStateException {
    MixpanelIntegration adapter = new MixpanelIntegration();
    adapter.initialize(context, new JsonMap().putValue("token", "foo")
        .putValue("trackAllPages", true)
        .putValue("trackCategorizedPages", false)
        .putValue("trackNamedPages", true), true);
    verifyStatic();
    MixpanelAPI.getInstance(context, "foo");
  }

  @Test
  public void activityLifecycle() {
    integration.onActivityCreated(activity, bundle);
    integration.onActivityStarted(activity);
    integration.onActivityResumed(activity);
    integration.onActivityPaused(activity);
    integration.onActivitySaveInstanceState(activity, bundle);
    integration.onActivityStopped(activity);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(mixpanelAPI);
  }

  @Test public void flush() {
    integration.flush();
    verify(mixpanelAPI).flush();
  }

  @Test
  public void screenTrackNothing() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = false;
    integration.screen(screenPayload("foo", "bar"));
    verifyNoMoreInteractions(mixpanelAPI);
  }

  @Test
  public void screenTrackNamedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = true;

    integration.screen(screenPayload(null, "bar"));
    verify(mixpanelAPI).track(eq("Viewed bar Screen"), Matchers.<JSONObject>any());

    integration.screen(screenPayload("foo", null));
    verifyNoMoreInteractions(mixpanelAPI);
  }

  @Test
  public void screenTrackCategorizedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = true;
    integration.trackNamedPages = false;

    integration.screen(screenPayload("foo", null));
    verify(mixpanelAPI).track(eq("Viewed foo Screen"), Matchers.<JSONObject>any());

    integration.screen(screenPayload(null, "bar"));
    verifyNoMoreInteractions(mixpanelAPI);
  }

  @Test
  public void screenTrackAllPages() {
    integration.trackAllPages = true;
    integration.trackCategorizedPages = new Random().nextBoolean();
    integration.trackNamedPages = new Random().nextBoolean();

    integration.screen(screenPayload("foo", null));
    verify(mixpanelAPI).track(eq("Viewed foo Screen"), Matchers.<JSONObject>any());

    integration.screen(screenPayload(null, "bar"));
    verify(mixpanelAPI).track(eq("Viewed bar Screen"), Matchers.<JSONObject>any());

    integration.screen(screenPayload("bar", "baz"));
    verify(mixpanelAPI).track(eq("Viewed baz Screen"), Matchers.<JSONObject>any());
  }

  @Test public void track() {
    integration.track(trackPayload("Button Clicked"));
    verify(mixpanelAPI).track(eq("Button Clicked"), Matchers.<JSONObject>any());
    verifyNoMoreInteractions(mixpanelAPI);
  }

  @Test public void alias() {
    integration.track(trackPayload("Button Clicked"));
    verify(mixpanelAPI).track(eq("Button Clicked"), Matchers.<JSONObject>any());
    verifyNoMoreInteractions(mixpanelAPI);
  }

  @Test public void identify() {
    integration.identify(identifyPayload("foo"));
    verify(mixpanelAPI).identify("foo");
    verify(mixpanelAPI).registerSuperProperties(Matchers.<JSONObject>any());
  }

  @Test public void identifyWithPeople() {
    integration.isPeopleEnabled = true;
    integration.identify(identifyPayload("foo"));
    verify(mixpanelAPI).identify("foo");
    verify(mixpanelAPI).registerSuperProperties(Matchers.<JSONObject>any());
    verify(people).identify("foo");
    verify(people).set(Matchers.<JSONObject>any());
  }
}
