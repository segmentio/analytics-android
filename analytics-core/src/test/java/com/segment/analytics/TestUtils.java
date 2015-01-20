package com.segment.analytics;

import android.app.Application;
import com.segment.analytics.internal.model.payloads.AliasPayload;
import com.segment.analytics.internal.model.payloads.GroupPayload;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Map;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONObject;
import org.junit.Ignore;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;

import static android.Manifest.permission.INTERNET;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
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
  public static final String SAMPLE_JSON = "{\n"
      + "  \"glossary\": {\n"
      + "    \"title\": \"example glossary\",\n"
      + "    \"GlossDiv\": {\n"
      + "      \"title\": \"S\",\n"
      + "      \"GlossList\": {\n"
      + "        \"GlossEntry\": {\n"
      + "          \"ID\": \"SGML\",\n"
      + "          \"SortAs\": \"SGML\",\n"
      + "          \"GlossTerm\": \"Standard Generalized Markup Language\",\n"
      + "          \"Acronym\": \"SGML\",\n"
      + "          \"Abbrev\": \"ISO 8879:1986\",\n"
      + "          \"GlossDef\": {\n"
      + "            \"para\": "
      + "\"A meta-markup language, used to decode markup languages such as DocBook.\",\n"
      + "            \"GlossSeeAlso\": [\n"
      + "              \"GML\",\n"
      + "              \"XML\"\n"
      + "            ]\n"
      + "          },\n"
      + "          \"GlossSee\": \"markup\"\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";
  // from http://json.org/example
  public static final String SAMPLE_JSON_LIST = "[\n"
      + "        {\"id\": \"Open\"},\n"
      + "        {\"id\": \"OpenNew\", \"label\": \"Open New\"},\n"
      + "        null,\n"
      + "        {\"id\": \"ZoomIn\", \"label\": \"Zoom In\"},\n"
      + "        {\"id\": \"ZoomOut\", \"label\": \"Zoom Out\"},\n"
      + "        {\"id\": \"OriginalView\", \"label\": \"Original View\"},\n"
      + "        null,\n"
      + "        {\"id\": \"Quality\"},\n"
      + "        {\"id\": \"Pause\"},\n"
      + "        {\"id\": \"Mute\"},\n"
      + "        null,\n"
      + "        {\"id\": \"Find\", \"label\": \"Find...\"},\n"
      + "        {\"id\": \"FindAgain\", \"label\": \"Find Again\"},\n"
      + "        {\"id\": \"Copy\"},\n"
      + "        {\"id\": \"CopyAgain\", \"label\": \"Copy Again\"},\n"
      + "        {\"id\": \"CopySVG\", \"label\": \"Copy SVG\"},\n"
      + "        {\"id\": \"ViewSVG\", \"label\": \"View SVG\"},\n"
      + "        {\"id\": \"ViewSource\", \"label\": \"View Source\"},\n"
      + "        {\"id\": \"SaveAs\", \"label\": \"Save As\"},\n"
      + "        null,\n"
      + "        {\"id\": \"Help\"},\n"
      + "        {\"id\": \"About\", \"label\": \"About Adobe CVG Viewer...\"}\n"
      + "    ]";

  public static Application mockApplication() {
    Application application = mock(Application.class);
    when(application.checkCallingOrSelfPermission(INTERNET)).thenReturn(PERMISSION_GRANTED);
    final File parent = Robolectric.getShadowApplication().getFilesDir();
    doAnswer(new Answer() {
      @Override public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        String fileName = (String) args[0];
        return new File(parent, fileName);
      }
    }).when(application).getDir(anyString(), anyInt());
    return application;
  }

  private TestUtils() {
    throw new AssertionError("no instances");
  }

  static class TrackPayloadBuilder {
    private AnalyticsContext context;
    private Traits traits;
    private String event;
    private Properties properties;
    private Options options;

    public TrackPayloadBuilder context(AnalyticsContext context) {
      this.context = context;
      return this;
    }

    public TrackPayloadBuilder traits(Traits traits) {
      this.traits = traits;
      return this;
    }

    public TrackPayloadBuilder event(String event) {
      this.event = event;
      return this;
    }

    public TrackPayloadBuilder properties(Properties properties) {
      this.properties = properties;
      return this;
    }

    public TrackPayloadBuilder options(Options options) {
      this.options = options;
      return this;
    }

    public TrackPayload build() {
      if (traits == null) {
        traits = Traits.create(Robolectric.application);
      }
      if (event == null) {
        event = "bar";
      }
      if (context == null) {
        context = new AnalyticsContext(Robolectric.application, traits);
      }
      if (properties == null) {
        properties = new Properties();
      }
      if (options == null) {
        options = new Options();
      }
      return new TrackPayload(context, options, event, properties);
    }
  }

  static class IdentifyPayloadBuilder {
    private AnalyticsContext context;
    private Traits traits;
    private Options options;

    public IdentifyPayloadBuilder traits(Traits traits) {
      this.traits = traits;
      return this;
    }

    public IdentifyPayloadBuilder options(Options options) {
      this.options = options;
      return this;
    }

    public IdentifyPayloadBuilder context(AnalyticsContext context) {
      this.context = context;
      return this;
    }

    public IdentifyPayload build() {
      if (traits == null) {
        traits = Traits.create(Robolectric.application);
      }
      if (context == null) {
        context = new AnalyticsContext(Robolectric.application, traits);
      }
      if (options == null) {
        options = new Options();
      }
      return new IdentifyPayload(context, options, traits);
    }
  }

  static class ScreenPayloadBuilder {
    private AnalyticsContext context;
    private Traits traits;
    private String category;
    private String name;
    private Properties properties;
    private Options options;

    public ScreenPayloadBuilder context(AnalyticsContext context) {
      this.context = context;
      return this;
    }

    public ScreenPayloadBuilder traits(Traits traits) {
      this.traits = traits;
      return this;
    }

    public ScreenPayloadBuilder category(String category) {
      this.category = category;
      return this;
    }

    public ScreenPayloadBuilder name(String name) {
      this.name = name;
      return this;
    }

    public ScreenPayloadBuilder properties(Properties properties) {
      this.properties = properties;
      return this;
    }

    public ScreenPayloadBuilder options(Options options) {
      this.options = options;
      return this;
    }

    public ScreenPayload build() {
      if (traits == null) {
        traits = Traits.create(Robolectric.application);
      }
      if (context == null) {
        context = new AnalyticsContext(Robolectric.application, traits);
      }
      if (options == null) {
        options = new Options();
      }
      if (category == null && name == null) {
        category = "foo";
        name = "bar";
      }
      if (properties == null) {
        properties = new Properties();
      }
      return new ScreenPayload(context, options, category, name, properties);
    }
  }

  static class AliasPayloadBuilder {
    private AnalyticsContext context;
    private Traits traits;
    private String previousId;
    private Options options;

    public AliasPayloadBuilder traits(Traits traits) {
      this.traits = traits;
      return this;
    }

    public AliasPayloadBuilder context(AnalyticsContext context) {
      this.context = context;
      return this;
    }

    public AliasPayloadBuilder previousId(String previousId) {
      this.previousId = previousId;
      return this;
    }

    public AliasPayloadBuilder options(Options options) {
      this.options = options;
      return this;
    }

    public AliasPayload build() {
      if (traits == null) {
        traits = Traits.create(Robolectric.application);
      }
      if (context == null) {
        context = new AnalyticsContext(Robolectric.application, traits);
      }
      if (options == null) {
        options = new Options();
      }
      if (previousId == null) {
        previousId = "foo";
      }
      return new AliasPayload(context, options, previousId);
    }
  }

  static class GroupPayloadBuilder {
    private AnalyticsContext context;
    private String groupId;
    private Traits traits;
    private Traits groupTraits;
    private Options options;

    public GroupPayloadBuilder context(AnalyticsContext context) {
      this.context = context;
      return this;
    }

    public GroupPayloadBuilder groupId(String groupId) {
      this.groupId = groupId;
      return this;
    }

    public GroupPayloadBuilder traits(Traits traits) {
      this.traits = traits;
      return this;
    }

    public GroupPayloadBuilder groupTraits(Traits groupTraits) {
      this.groupTraits = groupTraits;
      return this;
    }

    public GroupPayloadBuilder options(Options options) {
      this.options = options;
      return this;
    }

    public GroupPayload build() {
      if (traits == null) {
        traits = Traits.create(Robolectric.application);
      }
      if (groupTraits == null) {
        groupTraits = new Traits();
      }
      if (context == null) {
        context = new AnalyticsContext(Robolectric.application, traits);
      }
      if (options == null) {
        options = new Options();
      }
      if (groupId == null) {
        groupId = "bar";
      }
      return new GroupPayload(context, options, groupId, groupTraits);
    }
  }

  public static class JSONObjectMatcher extends TypeSafeMatcher<JSONObject> {
    private final JSONObject expected;

    public static JSONObject jsonEq(JSONObject expected) {
      return argThat(new JSONObjectMatcher(expected));
    }

    private JSONObjectMatcher(JSONObject expected) {
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

  public static <T extends ValueMap> T createValueMap(Map map, Class<T> clazz) {
    try {
      Constructor<T> constructor = clazz.getDeclaredConstructor(Map.class);
      constructor.setAccessible(true);
      return constructor.newInstance(map);
    } catch (Exception e) {
      throw new RuntimeException("Could not create instance of " + clazz.getCanonicalName(), e);
    }
  }
}
