package com.segment.analytics;

import android.app.Application;
import com.segment.analytics.integrations.TrackPayload;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONObject;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

import static android.Manifest.permission.INTERNET;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.segment.analytics.Utils.createContext;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TestUtils {

  public static final String PROJECT_SETTINGS_JSON_SAMPLE = "{\n"
      + "  \"Amplitude\": {\n"
      + "    \"trackNamedPages\": true,\n"
      + "    \"trackCategorizedPages\": true,\n"
      + "    \"trackAllPages\": false,\n"
      + "    \"apiKey\": \"ad3c426eb736d7442a65da8174bc1b1b\"\n"
      + "  },\n"
      + "  \"Flurry\": {\n"
      + "    \"apiKey\": \"8DY3D6S7CCWH54RBJ9ZM\",\n"
      + "    \"captureUncaughtExceptions\": false,\n"
      + "    \"useHttps\": true,\n"
      + "    \"sessionContinueSeconds\": 10\n"
      + "  },\n"
      + "  \"Mixpanel\": {\n"
      + "    \"people\": true,\n"
      + "    \"token\": \"f7afe0cb436685f61a2b203254779e02\",\n"
      + "    \"trackAllPages\": false,\n"
      + "    \"trackCategorizedPages\": true,\n"
      + "    \"trackNamedPages\": true,\n"
      + "    \"increments\": [\n"
      + "      \n"
      + "    ],\n"
      + "    \"legacySuperProperties\": false\n"
      + "  },\n"
      + "  \"Segment\": {\n"
      + "    \"apiKey\": \"l8v1ga655b\"\n"
      + "  }\n"
      + "}";

  static final String TRACK_PAYLOAD_JSON;
  static final TrackPayload TRACK_PAYLOAD;

  static {
    TRACK_PAYLOAD_JSON = "{\""
        + "messageId\":\"a161304c-498c-4830-9291-fcfb8498877b\","
        + "\"type\":\"track\","
        + "\"channel\":\"mobile\","
        + "\"context\":{\"traits\":{}},"
        + "\"anonymousId\":null,"
        + "\"timestamp\":\"2014-12-15T13:32:44-0700\","
        + "\"integrations\":"
        + "{},"
        + "\"event\":\"foo\","
        + "\"properties\":{}"
        + "}";

    AnalyticsContext analyticsContext = createContext(new Traits());
    TRACK_PAYLOAD = new TrackPayload(analyticsContext, new Options(), "foo", new Properties());
    // put some predictable values for data that is automatically generated
    TRACK_PAYLOAD.put("messageId", "a161304c-498c-4830-9291-fcfb8498877b");
    TRACK_PAYLOAD.put("timestamp", "2014-12-15T13:32:44-0700");
  }

  public static Application mockApplication() {
    Application application = mock(Application.class);
    when(application.checkCallingOrSelfPermission(INTERNET)).thenReturn(PERMISSION_GRANTED);
    final File parent = RuntimeEnvironment.application.getFilesDir();
    doAnswer(new Answer() {
      @Override public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        String fileName = (String) args[0];
        return new File(parent, fileName);
      }
    }).when(application).getDir(anyString(), anyInt());
    doAnswer(new Answer() {
      @Override public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        String name = (String) args[0];
        int mode = (int) args[1];
        return RuntimeEnvironment.application.getSharedPreferences(name, mode);
      }
    }).when(application).getSharedPreferences(anyString(), anyInt());
    return application;
  }

  public static <T extends ValueMap> T createValueMap(Map map, Class<T> clazz) {
    try {
      Constructor<T> constructor = clazz.getDeclaredConstructor(Map.class);
      constructor.setAccessible(true);
      return constructor.newInstance(map);
    } catch (Exception e) {
      throw new AssertionError(
          "Could not create instance of " + clazz.getCanonicalName() + ".\n" + e);
    }
  }

  private TestUtils() {
    throw new AssertionError("no instances");
  }

  public static <K, V> Map<K, V> mapEq(Map<K, V> expected) {
    return argThat(new MapMatcher<>(expected));
  }

  private static class MapMatcher<K, V> extends TypeSafeMatcher<Map<K, V>> {
    private final Map<K, V> expected;

    MapMatcher(Map<K, V> expected) {
      this.expected = expected;
    }

    @Override public boolean matchesSafely(Map<K, V> map) {
      return expected.equals(map);
    }

    @Override public void describeTo(Description description) {
      description.appendText(expected.toString());
    }
  }

  public static JSONObject jsonEq(JSONObject expected) {
    return argThat(new JSONObjectMatcher(expected));
  }

  private static class JSONObjectMatcher extends TypeSafeMatcher<JSONObject> {
    private final JSONObject expected;

    JSONObjectMatcher(JSONObject expected) {
      this.expected = expected;
    }

    @Override public boolean matchesSafely(JSONObject jsonObject) {
      // todo: this relies on having the same order
      return expected.toString().equals(jsonObject.toString());
    }

    @Override public void describeTo(Description description) {
      description.appendText(expected.toString());
    }
  }

  public static class SynchronousExecutor extends AbstractExecutorService {
    private final AtomicBoolean terminated = new AtomicBoolean(false);

    @Override public void shutdown() {
      terminated.set(true);
    }

    @Override public List<Runnable> shutdownNow() {
      return Collections.emptyList();
    }

    @Override public boolean isShutdown() {
      return terminated.get();
    }

    @Override public boolean isTerminated() {
      return terminated.get();
    }

    @Override public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
      return false;
    }

    @Override public void execute(Runnable command) {
      command.run();
    }
  }

  public static abstract class NoDescriptionMatcher<T> extends TypeSafeMatcher<T> {
    @Override public void describeTo(Description description) {
    }
  }
}
