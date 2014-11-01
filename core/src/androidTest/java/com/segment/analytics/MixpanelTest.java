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
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(MixpanelAPI.class)
public class MixpanelTest extends IntegrationExam {
  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock MixpanelAPI mixpanelAPI;
  @Mock MixpanelAPI.People people;
  MixpanelIntegrationAdapter adapter;

  @Before @Override public void setUp() {
    super.setUp();
    doReturn(people).when(mixpanelAPI).getPeople();

    PowerMockito.mockStatic(MixpanelAPI.class);
    adapter = new MixpanelIntegrationAdapter(true);
    adapter.mixpanelAPI = mixpanelAPI;
  }

  @Test public void initialize() throws InvalidConfigurationException {
    MixpanelIntegrationAdapter adapter = new MixpanelIntegrationAdapter(true);
    adapter.initialize(context, new JsonMap().putValue("token", "foo")
        .putValue("trackAllPages", true)
        .putValue("trackCategorizedPages", false)
        .putValue("trackNamedPages", true));
    verifyStatic();
    MixpanelAPI.getInstance(context, "foo");
  }

  @Test
  public void activityLifecycle() {
    adapter.onActivityCreated(activity, bundle);
    adapter.onActivityStarted(activity);
    adapter.onActivityResumed(activity);
    adapter.onActivityPaused(activity);
    adapter.onActivitySaveInstanceState(activity, bundle);
    adapter.onActivityStopped(activity);
    adapter.onActivityDestroyed(activity);
    verifyNoMoreInteractions(mixpanelAPI);
  }

  @Test public void flush() {
    adapter.flush();
    verify(mixpanelAPI).flush();
  }

  @Test
  public void screenTrackNothing() {
    adapter.trackAllPages = false;
    adapter.trackCategorizedPages = false;
    adapter.trackNamedPages = false;
    adapter.screen(screenPayload("foo", "bar"));
    verifyNoMoreInteractions(mixpanelAPI);
  }

  @Test
  public void screenTrackNamedPages() {
    adapter.trackAllPages = false;
    adapter.trackCategorizedPages = false;
    adapter.trackNamedPages = true;

    adapter.screen(screenPayload(null, "bar"));
    verify(mixpanelAPI).track(eq("Viewed bar Screen"), Matchers.<JSONObject>any());

    adapter.screen(screenPayload("foo", null));
    verifyNoMoreInteractions(mixpanelAPI);
  }

  @Test
  public void screenTrackCategorizedPages() {
    adapter.trackAllPages = false;
    adapter.trackCategorizedPages = true;
    adapter.trackNamedPages = false;

    adapter.screen(screenPayload("foo", null));
    verify(mixpanelAPI).track(eq("Viewed foo Screen"), Matchers.<JSONObject>any());

    adapter.screen(screenPayload(null, "bar"));
    verifyNoMoreInteractions(mixpanelAPI);
  }

  @Test
  public void screenTrackAllPages() {
    adapter.trackAllPages = true;
    adapter.trackCategorizedPages = new Random().nextBoolean();
    adapter.trackNamedPages = new Random().nextBoolean();

    adapter.screen(screenPayload("foo", null));
    verify(mixpanelAPI).track(eq("Viewed foo Screen"), Matchers.<JSONObject>any());

    adapter.screen(screenPayload(null, "bar"));
    verify(mixpanelAPI).track(eq("Viewed bar Screen"), Matchers.<JSONObject>any());

    adapter.screen(screenPayload("bar", "baz"));
    verify(mixpanelAPI).track(eq("Viewed baz Screen"), Matchers.<JSONObject>any());
  }

  @Test public void track() {
    adapter.track(trackPayload("Button Clicked"));
    verify(mixpanelAPI).track(eq("Button Clicked"), Matchers.<JSONObject>any());
    verifyNoMoreInteractions(mixpanelAPI);
  }

  @Test public void alias() {
    adapter.track(trackPayload("Button Clicked"));
    verify(mixpanelAPI).track(eq("Button Clicked"), Matchers.<JSONObject>any());
    verifyNoMoreInteractions(mixpanelAPI);
  }

  @Test public void identify() {
    adapter.identify(identifyPayload("foo"));
    verify(mixpanelAPI).identify("foo");
    verify(mixpanelAPI).registerSuperProperties(Matchers.<JSONObject>any());
  }

  @Test public void identifyWithPeople() {
    adapter.isPeopleEnabled = true;
    adapter.identify(identifyPayload("foo"));
    verify(mixpanelAPI).identify("foo");
    verify(mixpanelAPI).registerSuperProperties(Matchers.<JSONObject>any());
    verify(people).identify("foo");
    verify(people).set(Matchers.<JSONObject>any());
  }
}
