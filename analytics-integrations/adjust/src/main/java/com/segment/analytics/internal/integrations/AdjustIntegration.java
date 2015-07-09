package com.segment.analytics.internal.integrations;

import android.app.Activity;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAttribution;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustEvent;
import com.adjust.sdk.LogLevel;
import com.adjust.sdk.OnAttributionChangedListener;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.BuildConfig;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.TrackPayload;

/**
 * Adjust is a business intelligence platform for mobile app marketers.
 *
 * @see <a href="Adjust Website URL">https://www.adjust.com/</a>
 * @see <a href="https://segment.com/docs/integrations/adjust/">Adjust Integration</a>
 */
public class AdjustIntegration extends AbstractIntegration<Void> {

  static final String ADJUST_KEY = "Adjust";
  public static final String ADJUST_TOKEN = "appToken";

  private OnAttributionChangedListener attributionChangedListener;

  @Override public void initialize(final Analytics analytics, ValueMap settings)
      throws IllegalStateException {
    String appToken = settings.getString(ADJUST_TOKEN);
    String environment = BuildConfig.DEBUG ? AdjustConfig.ENVIRONMENT_SANDBOX
            : AdjustConfig.ENVIRONMENT_PRODUCTION;
    AdjustConfig adjustConfig = new AdjustConfig(analytics.getApplication(), appToken, environment);
    adjustConfig.setOnAttributionChangedListener(new OnAttributionChangedListener() {
      @Override
      public void onAttributionChanged(AdjustAttribution attribution) {
        if (attributionChangedListener != null) {
          attributionChangedListener.onAttributionChanged(attribution);
        }
      }
    });
    adjustConfig.setLogLevel(convertLogLevel(analytics.getLogLevel()));
    Adjust.onCreate(adjustConfig);
  }

  @Override public String key() {
    return ADJUST_KEY;
  }

  @Override public void track(TrackPayload track) {
    super.track(track);

    AdjustEvent event = new AdjustEvent(track.event());
    Properties properties = track.properties();
    double revenue = properties.getDouble("revenue", -1);
    if (revenue != -1) {
      event.setRevenue(revenue, properties.currency());
    }
    Adjust.trackEvent(event);
  }

  @Override public void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    Adjust.onResume();
  }

  @Override public void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
    Adjust.onPause();
  }

  @SuppressWarnings("unused")
  public void setAttributionChangedListener(OnAttributionChangedListener listener) {
    attributionChangedListener = listener;
  }

  private static LogLevel convertLogLevel(Analytics.LogLevel logLevel) {
    switch (logLevel) {
      case VERBOSE: return LogLevel.VERBOSE;
      case INFO: return LogLevel.INFO;
      case BASIC: return LogLevel.DEBUG;
      case NONE: return LogLevel.ERROR;
      default: return LogLevel.ERROR;
    }
  }
}
