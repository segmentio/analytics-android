package com.segment.analytics;

import android.app.Activity;
import android.os.Bundle;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.assertj.core.data.MapEntry;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
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

import static com.segment.analytics.TapstreamRobolectricTest.EventMatcher.eventEq;
import static com.segment.analytics.TestUtils.AliasPayloadBuilder;
import static com.segment.analytics.TestUtils.GroupPayloadBuilder;
import static com.segment.analytics.TestUtils.IdentifyPayloadBuilder;
import static com.segment.analytics.TestUtils.ScreenPayloadBuilder;
import static com.segment.analytics.TestUtils.TrackPayloadBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Tapstream.class)
public class TapstreamRobolectricTest extends AbstractIntegrationTest {
  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Tapstream tapstream;
  @Mock com.tapstream.sdk.Config config;
  TapstreamIntegration integration;

  @Before @Override public void setUp() {
    super.setUp();
    PowerMockito.mockStatic(Tapstream.class);
    integration = new TapstreamIntegration();
    integration.tapstream = tapstream;
    integration.config = config;
    when(context.getApplicationContext()).thenReturn(context);
  }

  @Test @Override public void initialize() throws IllegalStateException {
    TapstreamIntegration adapter = new TapstreamIntegration();
    adapter.initialize(context, new JsonMap().putValue("accountName", "foo")
        .putValue("sdkSecret", "bar")
        .putValue("trackAllPages", true)
        .putValue("trackCategorizedPages", false)
        .putValue("trackNamedPages", true), true);
    verifyStatic();
    Tapstream.create(eq(context), eq("foo"), eq("bar"), Matchers.<com.tapstream.sdk.Config>any());
  }

  @Test @Override public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreTapstreamInteractions();
  }

  @Test @Override public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreTapstreamInteractions();
  }

  @Test @Override public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreTapstreamInteractions();
  }

  @Test @Override public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreTapstreamInteractions();
  }

  @Test @Override public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyNoMoreTapstreamInteractions();
  }

  @Test @Override public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreTapstreamInteractions();
  }

  @Test @Override public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreTapstreamInteractions();
  }

  @Test @Override public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verify(tapstream).fireEvent(eventEq("foo"));
    verifyNoMoreTapstreamInteractions();
  }

  @Test @Override public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreTapstreamInteractions();
  }

  @Test @Override public void screen() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = false;

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void screenAllPages() {
    integration.trackAllPages = true;
    integration.trackCategorizedPages = new Random().nextBoolean();
    integration.trackNamedPages = new Random().nextBoolean();

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verify(tapstream).fireEvent(eventEq("viewed foo screen"));
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void screenNamedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = true;

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verify(tapstream).fireEvent(eventEq("viewed foo screen"));
    verifyNoMoreTapstreamInteractions();

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void screenCategorizedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = true;
    integration.trackNamedPages = false;

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verify(tapstream).fireEvent(eventEq("viewed foo screen"));
    verifyNoMoreTapstreamInteractions();

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyNoMoreTapstreamInteractions();
  }

  @Test @Override public void flush() {
    integration.flush();
    verifyNoMoreTapstreamInteractions();
  }

  @Test @Override public void identify() {
    Map<String, Object> emptyParams = new HashMap<String, Object>();
    config.globalEventParams = emptyParams;
    integration.identify(new IdentifyPayloadBuilder().userId("foo").build());
    // anonymousId is set automatically as well in the builder
    assertThat(emptyParams).hasSize(2).contains(MapEntry.entry("userId", "foo"));

    Map<String, Object> map = new HashMap<String, Object>();
    config.globalEventParams = map;
    Traits traits = new Traits().putValue("foo", "bar").putValue("baz", "qux");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    assertThat(map).hasSize(2)
        .contains(MapEntry.entry("foo", "bar"))
        .contains(MapEntry.entry("baz", "qux"));
  }

  @Test @Override public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreTapstreamInteractions();
  }

  private void verifyNoMoreTapstreamInteractions() {
    PowerMockito.verifyNoMoreInteractions(Tapstream.class);
    verifyNoMoreInteractions(tapstream);
  }

  static class EventMatcher extends TypeSafeMatcher<Event> {
    final String name;

    static Event eventEq(String name) {
      return argThat(new EventMatcher(name));
    }

    private EventMatcher(String name) {
      this.name = name;
    }

    @Override public boolean matchesSafely(Event event) {
      return event.getName().compareTo(name) == 0;
    }

    @Override protected void describeMismatchSafely(Event item, Description mismatchDescription) {
      super.describeMismatchSafely(item, mismatchDescription);
      mismatchDescription.appendText(item.getName());
      mismatchDescription.appendText(item.getEncodedName());
    }

    @Override public void describeTo(Description description) {
      description.appendText(name);
    }
  }
}
