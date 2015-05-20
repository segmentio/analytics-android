package com.segment.analytics.internal.integrations;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.segment.analytics.Analytics;
import com.segment.analytics.IntegrationTestRule;
import com.segment.analytics.Properties;
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

import static com.segment.analytics.TestUtils.createTraits;
import static com.segment.analytics.TestUtils.jsonEq;
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
        integration.initialize(analytics, new ValueMap().putValue("apiKey", "foo"));
        verifyStatic();
        Taplytics.startTaplytics(eq(context), eq("foo"));
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void track() {
        integration.track(new TrackPayloadBuilder().event("foo").build());

        verifyStatic();
        Taplytics.logEvent(eq("foo"), eq(0.0), jsonEq(new JSONObject()));
    }

    @Test
    public void trackWithPropertiesAndValue() throws JSONException {
        integration.track(new TrackPayloadBuilder().event("foo")
                .properties(new Properties().putReferrer("bar").putValue(20))
                .build());

        JSONObject metadata = new JSONObject();
        metadata.put("referrer", "bar");
        metadata.put("value", 20.0);
        verifyStatic();
        Taplytics.logEvent(eq("foo"), eq(20.0), jsonEq(metadata));
    }

    @Test
    public void identify() throws JSONException {
        Traits traits = createTraits("foo");
        integration.identify(new IdentifyPayloadBuilder().traits(traits).build());

        JSONObject attributes = new JSONObject();
        attributes.put("user_id", "foo");

        verifyStatic();
        Taplytics.setUserAttributes(jsonEq(attributes));
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void identifyWithTraits() throws JSONException {
        Traits traits = createTraits("foo") //
                .putName("bar")
                .putEmail("baz")
                .putGender("qux")
                .putFirstName("foobar")
                .putLastName("bazqux")
                .putAge(30)
                .putValue("custom", "foobarbazqux");

        integration.identify(new IdentifyPayloadBuilder().traits(traits).build());

        JSONObject userAttributes = new JSONObject();
        userAttributes.put("user_id", "foo");
        userAttributes.put("name", "bar");
        userAttributes.put("email", "baz");
        userAttributes.put("gender", "qux");
        userAttributes.put("firstName", "foobar");
        userAttributes.put("lastName", "bazqux");
        userAttributes.put("age", 30);
        userAttributes.put("custom", "foobarbazqux");
        verifyStatic();
        Taplytics.setUserAttributes(jsonEq(userAttributes));
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void group() throws JSONException {
        Traits traits = new Traits().putName("foo");
        integration.group(new GroupPayloadBuilder().groupTraits(traits).build());

        JSONObject userAttributes = new JSONObject();
        userAttributes.put("name", "foo");

        verifyStatic();
        Taplytics.setUserAttributes(jsonEq(userAttributes));
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void reset() {
        integration.reset();

        verifyStatic();
        Taplytics.resetAppUser(null);
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
