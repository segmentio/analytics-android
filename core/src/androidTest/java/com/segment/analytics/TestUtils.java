package com.segment.analytics;

import android.app.Application;

import static android.Manifest.permission.INTERNET;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
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
    return application;
  }
}
