package com.segment.analytics.android.integrations.amplitude;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amplitude.api.Amplitude;
import com.amplitude.api.AmplitudeClient;
import com.amplitude.api.Identify;
import com.amplitude.api.Revenue;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.BasePayload;
import com.segment.analytics.integrations.GroupPayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * Amplitude is an event tracking and segmentation tool for your mobile apps. By analyzing the
 * actions your users perform you can gain a better understanding of how they use your app.
 *
 * @see <a href="https://amplitude.com">Amplitude</a>
 * @see <a href="https://segment.com/docs/integrations/amplitude/">Amplitude Integration</a>
 * @see <a href="https://github.com/amplitude/Amplitude-Android">Amplitude Android SDK</a>
 */
public class AmplitudeIntegration extends Integration<AmplitudeClient> {

  public static final Factory FACTORY =
      new Factory() {
        @Override
        public Integration<?> create(ValueMap settings, Analytics analytics) {
          return new AmplitudeIntegration(Provider.REAL, analytics, settings);
        }

        @Override
        public String key() {
          return AMPLITUDE_KEY;
        }
      };
  private static final String AMPLITUDE_KEY = "Amplitude";
  private static final String VIEWED_EVENT_FORMAT = "Viewed %s Screen";

  private final AmplitudeClient amplitude;
  private final Logger logger;
  // mutable for testing.
  boolean trackAllPages;
  boolean trackAllPagesV2;
  boolean trackCategorizedPages;
  boolean trackNamedPages;
  boolean useLogRevenueV2;
  String groupTypeTrait;
  String groupValueTrait;
  Set<String> traitsToIncrement;
  Set<String> traitsToSetOnce;

  // Using PowerMockito fails with https://cloudup.com/c5JPuvmTCaH. So we introduce a provider
  // abstraction to mock what AmplitudeClient.getInstance() returns.
  interface Provider {

    AmplitudeClient get();

    Provider REAL =
        new Provider() {
          @Override
          public AmplitudeClient get() {
            return Amplitude.getInstance();
          }
        };
  }

  AmplitudeIntegration(Provider provider, Analytics analytics, ValueMap settings) {
    amplitude = provider.get();
    trackAllPages = settings.getBoolean("trackAllPages", false);
    trackAllPagesV2 = settings.getBoolean("trackAllPagesV2", true);
    trackCategorizedPages = settings.getBoolean("trackCategorizedPages", false);
    trackNamedPages = settings.getBoolean("trackNamedPages", false);
    useLogRevenueV2 = settings.getBoolean("useLogRevenueV2", false);
    groupTypeTrait = settings.getString("groupTypeTrait");
    groupValueTrait = settings.getString("groupTypeValue");
    traitsToIncrement = getStringSet(settings, "traitsToIncrement");
    traitsToSetOnce = getStringSet(settings, "traitsToSetOnce");
    logger = analytics.logger(AMPLITUDE_KEY);

    String apiKey = settings.getString("apiKey");
    amplitude.initialize(analytics.getApplication(), apiKey);
    logger.verbose("AmplitudeClient.getInstance().initialize(context, %s);", apiKey);

    amplitude.enableForegroundTracking(analytics.getApplication());
    logger.verbose("AmplitudeClient.getInstance().enableForegroundTracking(context);");

    boolean trackSessionEvents = settings.getBoolean("trackSessionEvents", false);
    amplitude.trackSessionEvents(trackSessionEvents);
    logger.verbose("AmplitudeClient.getInstance().trackSessionEvents(%s);", trackSessionEvents);

    boolean enableLocationListening = settings.getBoolean("enableLocationListening", true);
    if (!enableLocationListening) {
      amplitude.disableLocationListening();
    }

    boolean useAdvertisingIdForDeviceId = settings.getBoolean("useAdvertisingIdForDeviceId", false);
    if (useAdvertisingIdForDeviceId) {
      amplitude.useAdvertisingIdForDeviceId();
    }
  }

  static Set<String> getStringSet(ValueMap valueMap, String key) {
    try {
      //noinspection unchecked
      List<Object> incrementTraits = (List<Object>) valueMap.get(key);
      if (incrementTraits == null || incrementTraits.size() == 0) {
        return Collections.emptySet();
      }
      Set<String> stringSet = new HashSet<>(incrementTraits.size());
      for (int i = 0; i < incrementTraits.size(); i++) {
        stringSet.add((String) incrementTraits.get(i));
      }
      return stringSet;
    } catch (ClassCastException e) {
      return Collections.emptySet();
    }
  }

  @Override
  public AmplitudeClient getUnderlyingInstance() {
    return amplitude;
  }

  @Override
  public void identify(IdentifyPayload identify) {
    super.identify(identify);

    String userId = identify.userId();
    amplitude.setUserId(userId);
    logger.verbose("AmplitudeClient.getInstance().setUserId(%s);", userId);

    Traits traits = identify.traits();
    if (!isNullOrEmpty(traitsToIncrement) || !isNullOrEmpty(traitsToSetOnce)) {
      handleTraits(traits);
    } else {
      JSONObject userTraits = traits.toJsonObject();
      amplitude.setUserProperties(userTraits);
      logger.verbose("AmplitudeClient.getInstance().setUserProperties(%s);", userTraits);
    }

    JSONObject groups = groups(identify);
    if (groups == null) {
      return;
    }
    Iterator<String> it = groups.keys();
    while (it.hasNext()) {
      String key = it.next();
      try {
        Object value = groups.get(key);
        amplitude.setGroup(key, value);
      } catch (JSONException e) {
        logger.error(e, "error reading %s from %s", key, groups);
      }
    }
  }

  private void handleTraits(Traits traits) {
    Identify identify = new Identify();
    for (Map.Entry<String, Object> entry : traits.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (traitsToIncrement.contains(key)) {
        incrementTrait(key, value, identify);
      } else if (traitsToSetOnce.contains(key)) {
        setOnce(key, value, identify);
      } else {
        setTrait(key, value, identify);
      }
    }
    amplitude.identify(identify);
    logger.verbose("Amplitude.getInstance().identify(identify)");
  }

  private void incrementTrait(String key, Object value, Identify identify) {
    if (value instanceof Double) {
      double doubleValue = (Double) value;
      identify.add(key, doubleValue);
    }
    if (value instanceof Float) {
      float floatValue = (Float) value;
      identify.add(key, floatValue);
    }
    if (value instanceof Integer) {
      int intValue = (Integer) value;
      identify.add(key, intValue);
    }
    if (value instanceof Long) {
      long longValue = (Long) value;
      identify.add(key, longValue);
    }
    if (value instanceof String) {
      String stringValue = String.valueOf(value);
      identify.add(key, stringValue);
    }
  }

  private void setOnce(String key, Object value, Identify identify) {
    if (value instanceof Double) {
      double doubleValue = (Double) value;
      identify.setOnce(key, doubleValue);
    }
    if (value instanceof Float) {
      float floatValue = (Float) value;
      identify.setOnce(key, floatValue);
    }
    if (value instanceof Integer) {
      int intValue = (Integer) value;
      identify.setOnce(key, intValue);
    }
    if (value instanceof Long) {
      long longValue = (Long) value;
      identify.setOnce(key, longValue);
    }
    if (value instanceof String) {
      String stringValue = String.valueOf(value);
      identify.setOnce(key, stringValue);
    }
  }

  private void setTrait(String key, Object value, Identify identify) {
    if (value instanceof Double) {
      double doubleValue = (Double) value;
      identify.set(key, doubleValue);
    }
    if (value instanceof Float) {
      float floatValue = (Float) value;
      identify.set(key, floatValue);
    }
    if (value instanceof Integer) {
      int intValue = (Integer) value;
      identify.set(key, intValue);
    }
    if (value instanceof Long) {
      long longValue = (Long) value;
      identify.set(key, longValue);
    }
    if (value instanceof String) {
      String stringValue = String.valueOf(value);
      identify.set(key, stringValue);
    }
  }

  @Override
  public void screen(ScreenPayload screen) {
    super.screen(screen);
    if (trackAllPagesV2) {
      Properties properties = new Properties();
      properties.putAll(screen.properties());
      properties.put("name", screen.name());
      event("Loaded a Screen", properties, null, null);
      return;
    }

    if (trackAllPages) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties(), null, null);
    } else if (trackCategorizedPages && !isNullOrEmpty(screen.category())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.category()), screen.properties(), null, null);
    } else if (trackNamedPages && !isNullOrEmpty(screen.name())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.name()), screen.properties(), null, null);
    }
  }

  @Override
  public void track(TrackPayload track) {
    super.track(track);

    JSONObject groups = groups(track);
    Map<String, Object> eventOptions = track.integrations().getValueMap(AMPLITUDE_KEY);
    event(track.event(), track.properties(), eventOptions, groups);
  }

  static @Nullable JSONObject groups(BasePayload payload) {
    ValueMap integrations = payload.integrations();
    if (isNullOrEmpty(integrations)) {
      return null;
    }

    ValueMap amplitudeOptions = integrations.getValueMap(AMPLITUDE_KEY);
    if (isNullOrEmpty(amplitudeOptions)) {
      return null;
    }

    ValueMap groups = amplitudeOptions.getValueMap("groups");
    if (isNullOrEmpty(groups)) {
      return null;
    }

    return groups.toJsonObject();
  }

  private void event(
      @NonNull String name,
      @NonNull Properties properties,
      @Nullable Map options,
      @Nullable JSONObject groups) {
    JSONObject propertiesJSON = properties.toJsonObject();

    amplitude.logEvent(name, propertiesJSON, groups, getOptOutOfSessionFromOptions(options));
    logger.verbose(
        "AmplitudeClient.getInstance().logEvent(%s, %s, %s, %s);",
        name, propertiesJSON, groups, getOptOutOfSessionFromOptions(options));

    // use containsKey since revenue and total can have negative values.
    if (properties.containsKey("revenue") || properties.containsKey("total")) {
      if (useLogRevenueV2) {
        trackWithLogRevenueV2(properties, propertiesJSON);
      } else {
        logRevenueV1(properties);
      }
    }
  }

  private boolean getOptOutOfSessionFromOptions(@Nullable Map options) {
    if (isNullOrEmpty(options)) {
      return false;
    }
    Object outOfSession = options.get("outOfSession");
    if (outOfSession == null) {
      return false;
    }
    if (outOfSession instanceof Boolean) {
      return (Boolean) options.get("outOfSession");
    }
    return false;
  }

  @SuppressWarnings("deprecation")
  private void logRevenueV1(Properties properties) {
    double revenue = properties.getDouble("revenue", 0);
    if (revenue == 0) {
      revenue = properties.getDouble("total", 0);
    }
    String productId = properties.getString("productId");
    int quantity = properties.getInt("quantity", 0);
    String receipt = properties.getString("receipt");
    String receiptSignature = properties.getString("receiptSignature");
    amplitude.logRevenue(productId, quantity, revenue, receipt, receiptSignature);
    logger.verbose(
        "AmplitudeClient.getInstance().logRevenue(%s, %s, %s, %s, %s);",
        productId, quantity, revenue, receipt, receiptSignature);
  }

  private void trackWithLogRevenueV2(Properties properties, JSONObject propertiesJSON) {
    double price = properties.getDouble("price", 0);
    int quantity = properties.getInt("quantity", 1);

    // if no price, fallback to using revenue, then total
    if (!properties.containsKey("price")) {
      price = properties.getDouble("revenue", 0);
      if (price == 0) {
        price = properties.getDouble("total", 0);
      }
      // overrides quantity to 1; Amplitude will internally calculate revenue as price * quantity
      quantity = 1;
    }

    Revenue ampRevenue = new Revenue().setPrice(price).setQuantity(quantity);
    if (properties.containsKey("productId")) {
      ampRevenue.setProductId(properties.getString("productId"));
    }
    if (properties.containsKey("revenueType")) {
      ampRevenue.setRevenueType(properties.getString("revenueType"));
    }
    if (properties.containsKey("receipt") && properties.containsKey("receiptSignature")) {
      ampRevenue.setReceipt(
          properties.getString("receipt"), properties.getString("receiptSignature"));
    }
    ampRevenue.setEventProperties(propertiesJSON);
    amplitude.logRevenueV2(ampRevenue);
    logger.verbose("AmplitudeClient.getInstance().logRevenueV2(%s, %s);", price, quantity);
  }

  @Override
  public void group(GroupPayload group) {
    String groupName = null;
    String groupValue = group.groupId();

    Traits traits = group.traits();
    if (!isNullOrEmpty(traits)) {
      if (traits.containsKey(groupTypeTrait) && traits.containsKey(groupValueTrait)) {
        groupName = traits.getString(groupTypeTrait);
        groupValue = traits.getString(groupValueTrait);
      } else {
        groupName = traits.name();
      }
    }

    if (isNullOrEmpty(groupName)) {
      groupName = "[Segment] Group";
    }

    // Set group
    amplitude.setGroup(groupName, groupValue);

    // Set group properties
    Identify groupIdentify = new Identify();
    groupIdentify.set("library", "segment");
    if (!isNullOrEmpty(traits)) {
      groupIdentify.set("group_properties", traits.toJsonObject());
    }

    amplitude.groupIdentify(groupName, groupValue, groupIdentify);
  }

  @Override
  public void flush() {
    super.flush();

    amplitude.uploadEvents();
    logger.verbose("AmplitudeClient.getInstance().uploadEvents();");
  }

  @Override
  public void reset() {
    super.reset();

    amplitude.setUserId(null);
    amplitude.regenerateDeviceId();
    logger.verbose("AmplitudeClient.getInstance().setUserId(null)");
    logger.verbose("AmplitudeClient.getInstance().regenerateDeviceId();");
  }
}
