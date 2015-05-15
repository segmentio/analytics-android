package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.segment.analytics.Analytics;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.Properties;
import com.segment.analytics.TestUtils;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.internal.model.payloads.util.AliasPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.GroupPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.IdentifyPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.ScreenPayloadBuilder;
import com.segment.analytics.internal.model.payloads.util.TrackPayloadBuilder;
import com.taplytics.sdk.Taplytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;

import static com.segment.analytics.TestUtils.createTraits;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;


@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"})
@PrepareForTest(Taplytics.class)
public class TaplyticsTest {
    TaplyticsIntegration integration;
    @Rule
    public PowerMockRule rule = new PowerMockRule();
    @Rule
    public IntegrationTestRule integrationTestRule = new IntegrationTestRule();
    @Mock
    Application context;
    @Mock
    Analytics analytics;

    @Before
    public void setUp() {
        initMocks(this);
        PowerMockito.mockStatic(Taplytics.class);
        integration = new TaplyticsIntegration();
    }

    @Test
    public void initialize() {
        when(analytics.getApplication()).thenReturn(context);
        integration.initialize(analytics, new ValueMap().putValue("appkey", "foo"));
        verifyStatic();
        HashMap options = new HashMap();
        Taplytics.startTaplytics(eq(context), eq("foo"), eq(options));
    }

    @Test
    public void initializeWithOptions() {
        when(analytics.getApplication()).thenReturn(context);
        integration.initialize(analytics, new ValueMap().putValue("appkey", "foo").putValue("debugLogging", true));
        verifyStatic();
        HashMap options = new HashMap();
        options.put("debugLogging", true);
        Taplytics.startTaplytics(context, "foo", options);
    }

    @Test
    public void track() {
        integration.track(new TrackPayloadBuilder().event("someEvent").build());
        verifyStatic();
        Taplytics.logEvent(eq("someEvent"), eq((Number) null), TestUtils.jsonEq(new JSONObject()));
    }

    @Test
    public void trackNumeric() throws JSONException {
        integration.track(new TrackPayloadBuilder().event("someEvent")
                .properties(new Properties().putValue("value", 416))
                .build());
        verifyStatic();
        JSONObject metadata = new JSONObject();
        metadata.put("value", 416);
        Taplytics.logEvent(eq("someEvent"), eq((Number) null), TestUtils.jsonEq(metadata));
    }

    @Test
    public void trackNumericInteger() throws JSONException {
        integration.track(new TrackPayloadBuilder().event("someEvent")
                .properties(new Properties().putValue("value", 6.0))
                .build());
        verifyStatic();
        JSONObject metadata = new JSONObject();
        metadata.put("value", 6.0);
        Taplytics.logEvent(eq("someEvent"), eq((Number) null), TestUtils.jsonEq(metadata));
    }

    @Test
    public void identify() throws JSONException {
        integration.identify(new IdentifyPayloadBuilder().traits(createTraits("magicnumber").putName("vicVu")).build());
        verifyStatic();
        JSONObject attributes = new JSONObject();
        attributes.put("name", "vicVu");
        attributes.put("user_id", "magicnumber");
        Taplytics.setUserAttributes(TestUtils.jsonEq(attributes));
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void identifyWithExtraTraits() throws JSONException {
        Traits traits = createTraits("magicnumber").putPhone("555 553 5541").putAge(22).putGender("male").putEmployees(30);
        integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
        verifyStatic();
        JSONObject attributes = new JSONObject();
        attributes.put("user_id", "magicnumber");
        attributes.put("age", 22);
        attributes.put("gender", "male");
        JSONObject customData = new JSONObject();
        customData.put("phone", "555 553 5541");
        customData.put("employees", 30L);
        attributes.put("customData", customData);
        Taplytics.setUserAttributes(TestUtils.jsonEq(attributes));
        verifyStatic();
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void group() throws JSONException {
        Traits traits = createTraits("group").putName("somegroup");
        integration.group(new GroupPayloadBuilder().groupTraits(traits).build());
        JSONObject attributes = new JSONObject();
        JSONObject metaData = new JSONObject();
        metaData.put("user_id", "group");
        metaData.put("name", "someGroup");
        attributes.put("metaData", "someGroup");
        Taplytics.setUserAttributes(TestUtils.jsonEq(attributes));
        verifyStatic();
    }

    @Test
    public void reset() {
        integration.reset();
        Taplytics.resetAppUser(null);
        verifyStatic();
    }

    @Test
    public void activityCreate() {
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        integration.onActivityCreated(activity, bundle);
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void activityStart() {
        Activity activity = mock(Activity.class);
        integration.onActivityStarted(activity);
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void activityResume() {
        Activity activity = mock(Activity.class);
        integration.onActivityResumed(activity);
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void activityPause() {
        Activity activity = mock(Activity.class);
        integration.onActivityPaused(activity);
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void activityStop() {
        Activity activity = mock(Activity.class);
        integration.onActivityStopped(activity);
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void activitySaveInstance() {
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        integration.onActivitySaveInstanceState(activity, bundle);
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void activityDestroy() {
        Activity activity = mock(Activity.class);
        integration.onActivityDestroyed(activity);
        verifyNoMoreInteractions(Taplytics.class);
    }


    @Test
    public void screen() {
        integration.screen(new ScreenPayloadBuilder().name("somePage").build());
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void flush() {
        integration.flush();
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void alias() {
        integration.alias(new AliasPayloadBuilder().build());
        verifyNoMoreInteractions(Taplytics.class);
    }

}
