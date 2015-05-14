import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.segment.analytics.Traits;
import com.taplytics.sdk.Taplytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;


@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18, manifest = Config.NONE)
public class TaplyticsTest extends AbstractIntegrationTest {
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
        Taplytics.startTaplytics(context, "foo");
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
    public void track() {
        integration.track(new TrackPayloadBuilder().event("someEvent").build());
        verifyStatic();
        Taplytics.track(eq("someEvent"));
    }

    @Test
    public void trackNumeric() {
        integration.track(new TrackPayloadBuilder().event("someEvent")
                .properties(new Properties().putValue("number", 416))
                .build());
        verifyStatic();
        Taplytics.track(eq("pressPlay"), eq(416));
    }

    @Test
    public void trackNumericInteger() {
        integration.track(new TrackPayloadBuilder().event("someEvent")
                .properties(new Properties().putValue("value", 6))
                .build());
        verifyStatic();
        Taplytics.track(eq("pressPlay"), eq(6.0));
    }

    @Test
    public void alias() {
        integration.alias(new AliasPayloadBuilder().build());
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void identify() throws JSONException {
        integration.identify(new IdentifyPayloadBuilder().traits(createTraits("vicVu")).build());
        verifyStatic();
        JSONObject attributes = new JSONObject();
        attributes.put("name", "vicVu");
        attributes.put("user_id", "magicnumber")
        Taplytics.setUserAttributes(attributes);
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void identifyTraits() throws JSONException {
        Traits traits = createTraits("magicnumber").putPhone("555 553 5541").putAge(22).putGender("male");
        integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
        verifyStatic();
        JSONObject attributes = new JSONObject();
        attributes.put("user_id", "magicnumber")
        Taplytics.setUserAttributes(attributes);
        verifyStatic();
        attributes.put("age", 22);
        Taplytics.setUserAttributes(attributes);
        verifyStatic();
        attributes.put("gender", "male");
        Taplytics.setUserAttributes(attributes);
        verifyNoMoreInteractions(Taplytics.class);
    }

    @Test
    public void group() {
        integration.group(new GroupPayloadBuilder().build());
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
    public void reset() {
        integration.reset();
        Taplytics.resetAppUser(null);
        verifyNoMoreInteractions(Taplytics.class);
    }
}
