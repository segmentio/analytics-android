package com.segment.analytics.internal.integrations;

import com.segment.analytics.core.tests.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Created by VicV on 5/14/15.
 * <p/>
 * Testing class for {@link TaplyticsJSONObject}
 */

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 18, manifest = Config.NONE)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"})
@PrepareForTest(TaplyticsJSONObject.class)
public class TaplyticsJSONObjectTest {

    @Test
    public void testEmptyComparison() {
        TaplyticsJSONObject object = new TaplyticsJSONObject();
        JSONObject object2 = new JSONObject();
        assertTrue(object.equals(object2));
    }

    @Test
    public void testNullComparison() {
        TaplyticsJSONObject object = new TaplyticsJSONObject();
        JSONObject object2 = null;
        assertFalse(object.equals(object2));
    }

    @Test
    public void testBasicComparison() throws JSONException {
        TaplyticsJSONObject object = new TaplyticsJSONObject();
        JSONObject object2 = new JSONObject();
        object.put("someString", "string");
        assertFalse(object.equals(object2));
        object2.put("someString", "string2");
        assertFalse(object.equals(object2));
        object2.put("someString", "string");
        assertTrue(object.equals(object2));
    }

    @Test
    public void testValueTypeComparison() throws JSONException {
        TaplyticsJSONObject object = new TaplyticsJSONObject();
        JSONObject object2 = new JSONObject();
        object.put("someString", "string");
        object2.put("someString", false);
        assertFalse(object.equals(object2));
        object.put("someString", 3);
        assertFalse(object.equals(object2));
    }

    @Test
    public void testMultipleComparison() throws JSONException {
        TaplyticsJSONObject object = new TaplyticsJSONObject();
        JSONObject object2 = new JSONObject();
        object.put("something", "string");
        object2.put("something", "string");
        object.put("something1", "string2");
        object2.put("something1", "string2");
        object.put("something2", true);
        object2.put("something2", true);
        object.put("something3", false);
        object2.put("something3", false);
        object.put("something4", 6660L);
        object2.put("something4", 6660L);
        object.put("something5", 53);
        object2.put("something5", 53);
        object.put("something7", 10d);
        object2.put("something7", 10d);
        object.put("something8", 30f);
        object2.put("something8", 30f);
        assertTrue(object.equals(object2));
    }


    @Test
    public void testNestedJSONPass() throws JSONException {
        TaplyticsJSONObject object = new TaplyticsJSONObject();
        JSONObject object2 = new JSONObject();
        TaplyticsJSONObject object4 = new TaplyticsJSONObject();
        JSONObject object5 = new JSONObject();
        object4.put("something", "string");
        object5.put("something", "string");
        object4.put("something1", "string2");
        object5.put("something1", "string2");
        object4.put("something2", true);
        object5.put("something2", true);
        object4.put("something3", false);
        object5.put("something3", false);
        object4.put("something4", 6660L);
        object5.put("something4", 6660L);
        object4.put("something5", 53);
        object5.put("something5", 53);
        object4.put("something7", 10d);
        object5.put("something7", 10d);
        object4.put("something8", 30f);
        object5.put("something8", 30f);
        object.put("json", object4);
        object2.put("json", object5);
        assertTrue(object4.equals(object5));
        assertTrue(object.equals(object2));
    }

    @Test
    public void testSuperNestedJSONPass() throws JSONException {
        TaplyticsJSONObject object = new TaplyticsJSONObject();
        JSONObject object2 = new JSONObject();
        JSONObject object4 = new JSONObject();
        JSONObject object5 = new JSONObject();
        JSONObject object7 = new JSONObject();
        JSONObject object8 = new JSONObject();
        TaplyticsJSONObject object9 = new TaplyticsJSONObject();
        JSONObject object10 = new JSONObject();
        object4.put("something", "string");
        object5.put("something", "string");
        object4.put("something1", "string2");
        object5.put("something1", "string2");
        object4.put("something2", true);
        object5.put("something2", true);
        object4.put("something3", false);
        object5.put("something3", false);
        object4.put("something4", 6660L);
        object5.put("something4", 6660L);
        object4.put("something5", 53);
        object5.put("something5", 53);
        object4.put("something7", 10d);
        object5.put("something7", 10d);
        object4.put("something8", 30f);
        object5.put("something8", 30f);
        object.put("json", object4);
        object2.put("json", object5);

        object7.put("someJson", object);
        object8.put("someJson", object2);

        object7.put("someValue", true);
        object8.put("someValue", true);

        object9.put("someMoreJson", object7);
        object10.put("someMoreJson", object8);

        object9.put("someMoreJson", object);
        object10.put("someMoreJson", object);

        object9.put("someRandomValue", 10L);
        object10.put("someRandomValue", 10L);

        assertTrue(object9.equals(object10));
    }

    @Test
    public void testNestedJSONFail() throws JSONException {
        TaplyticsJSONObject object = new TaplyticsJSONObject();
        JSONObject object2 = new JSONObject();
        TaplyticsJSONObject object4 = new TaplyticsJSONObject();
        JSONObject object5 = new JSONObject();
        object4.put("something", "string");
        object5.put("something", "string");
        object4.put("something1", "string2");
        object5.put("something1", "string2");
        object4.put("something2", true);
        object5.put("something2", true);
        object4.put("something3", false);
        object5.put("something3", false);
        object4.put("something4", 6660L);
        object5.put("something4", 6660L);
        object4.put("something5", 53);
        object5.put("something5", 53);
        object4.put("something7", 10d);
        object5.put("something7", 10d);
        object4.put("something8", 30L);
        object5.put("something8", 30f);
        object.put("json", object4);
        object2.put("json", object5);
        assertFalse(object4.equals(object5));
        assertFalse(object.equals(object2));
    }
}
