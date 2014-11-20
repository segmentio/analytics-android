package com.segment.analytics;

import android.app.Application;
import java.io.File;
import java.util.UUID;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONObject;
import org.robolectric.Robolectric;

import static android.Manifest.permission.INTERNET;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class TestUtils {
  static final String PROJECT_SETTINGS_JSON_SAMPLE = "{\n"
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
      + "  \"Segment.io\": {\n"
      + "    \"apiKey\": \"l8v1ga655b\"\n"
      + "  }\n"
      + "}";
  static final String SAMPLE_JSON = "{\n"
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
  static final String SAMPLE_JSON_LIST = "[\n"
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

  private TestUtils() {
    throw new AssertionError("no instances");
  }

  static Application mockApplication() {
    Application application = mock(Application.class);
    when(application.checkCallingOrSelfPermission(INTERNET)).thenReturn(PERMISSION_GRANTED);
    File parent = Robolectric.getShadowApplication().getFilesDir();
    File temp = new File(parent, "temp");
    when(application.getFilesDir()).thenReturn(temp);
    return application;
  }

  static class TrackPayloadBuilder {
    private AnalyticsContext context;
    private String anonymousId;
    private String userId;
    private String event;
    private Properties properties;
    private Options options;

    public void context(AnalyticsContext context) {
      this.context = context;
    }

    public TrackPayloadBuilder anonymousId(String anonymousId) {
      this.anonymousId = anonymousId;
      return this;
    }

    public TrackPayloadBuilder userId(String userId) {
      this.userId = userId;
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
      if (anonymousId == null) {
        anonymousId = UUID.randomUUID().toString();
      }
      if (userId == null) {
        userId = "foo";
      }
      if (event == null) {
        event = "bar";
      }
      if (context == null) {
        Traits traits = new Traits();
        context = new AnalyticsContext(Robolectric.application, traits);
      }
      if (properties == null) {
        properties = new Properties();
      }
      if (options == null) {
        options = new Options();
      }
      return new TrackPayload(anonymousId, context, userId, event, properties, options);
    }
  }

  static class IdentifyPayloadBuilder {
    private AnalyticsContext context;
    private String anonymousId;
    private String userId;
    private Traits traits;
    private Options options;

    public IdentifyPayloadBuilder anonymousId(String anonymousId) {
      this.anonymousId = anonymousId;
      return this;
    }

    public IdentifyPayloadBuilder userId(String userId) {
      this.userId = userId;
      return this;
    }

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
      if (anonymousId == null) {
        anonymousId = UUID.randomUUID().toString();
      }
      if (userId == null) {
        if (traits == null) {
          userId = "foo";
        } else if (traits.userId() != null) {
          userId = traits.userId();
        }
      }
      if (traits == null) {
        traits = new Traits().putUserId(userId).putAnonymousId(anonymousId);
      }
      if (context == null) {
        context = new AnalyticsContext(Robolectric.application, traits);
      }
      if (options == null) {
        options = new Options();
      }
      return new IdentifyPayload(anonymousId, context, userId, traits, options);
    }
  }

  static class ScreenPayloadBuilder {
    private AnalyticsContext context;
    private String anonymousId;
    private String userId;
    private String category;
    private String name;
    private Properties properties;
    private Options options;

    public void context(AnalyticsContext context) {
      this.context = context;
    }

    public ScreenPayloadBuilder anonymousId(String anonymousId) {
      this.anonymousId = anonymousId;
      return this;
    }

    public ScreenPayloadBuilder userId(String userId) {
      this.userId = userId;
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
      if (anonymousId == null) {
        anonymousId = UUID.randomUUID().toString();
      }
      if (userId == null) {
        userId = "foo";
      }
      if (category == null && name == null) {
        category = "bar";
        name = "baz";
      }
      if (properties == null) {
        properties = new Properties();
      }
      if (context == null) {
        Traits traits = new Traits().putUserId(userId).putAnonymousId(anonymousId);
        context = new AnalyticsContext(Robolectric.application, traits);
      }
      if (options == null) {
        options = new Options();
      }
      return new ScreenPayload(anonymousId, context, userId, category, name, properties, options);
    }
  }

  static class AliasPayloadBuilder {
    private String anonymousId;
    private AnalyticsContext context;
    private String userId;
    private String previousId;
    private Options options;

    public AliasPayloadBuilder anonymousId(String anonymousId) {
      this.anonymousId = anonymousId;
      return this;
    }

    public AliasPayloadBuilder context(AnalyticsContext context) {
      this.context = context;
      return this;
    }

    public AliasPayloadBuilder userId(String userId) {
      this.userId = userId;
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
      if (anonymousId == null) {
        anonymousId = UUID.randomUUID().toString();
      }
      if (userId == null) {
        userId = "foo";
      }
      if (previousId == null) {
        userId = "bar";
      }
      if (context == null) {
        Traits traits = new Traits().putUserId(userId).putAnonymousId(anonymousId);
        context = new AnalyticsContext(Robolectric.application, traits);
      }
      if (options == null) {
        options = new Options();
      }
      return new AliasPayload(anonymousId, context, userId, previousId, options);
    }
  }

  static class GroupPayloadBuilder {
    private String anonymousId;
    private AnalyticsContext context;
    private String userId;
    private String groupId;
    private Traits traits;
    private Options options;

    public GroupPayloadBuilder anonymousId(String anonymousId) {
      this.anonymousId = anonymousId;
      return this;
    }

    public GroupPayloadBuilder context(AnalyticsContext context) {
      this.context = context;
      return this;
    }

    public GroupPayloadBuilder userId(String userId) {
      this.userId = userId;
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

    public GroupPayloadBuilder options(Options options) {
      this.options = options;
      return this;
    }

    public GroupPayload build() {
      if (anonymousId == null) {
        anonymousId = UUID.randomUUID().toString();
      }
      if (userId == null) {
        if (traits == null) {
          userId = "foo";
        } else if (traits.userId() != null) {
          userId = traits.userId();
        }
      }
      if (groupId == null) {
        groupId = "bar";
      }
      if (traits == null) {
        traits = new Traits().putUserId(userId).putAnonymousId(anonymousId);
      }
      if (context == null) {
        context = new AnalyticsContext(Robolectric.application, traits);
      }
      if (options == null) {
        options = new Options();
      }
      return new GroupPayload(anonymousId, context, userId, groupId, traits, options);
    }
  }

  static class JSONObjectMatcher extends TypeSafeMatcher<JSONObject> {
    private final JSONObject expected;

    private JSONObjectMatcher(JSONObject expected) {
      this.expected = expected;
    }

    static JSONObject jsonEq(JSONObject expected) {
      return argThat(new JSONObjectMatcher(expected));
    }

    @Override public boolean matchesSafely(JSONObject jsonObject) {
      // this relies on having the same order
      return expected.toString().equals(jsonObject.toString());
    }

    @Override public void describeTo(Description description) {
      description.appendText(expected.toString());
    }
  }
}
