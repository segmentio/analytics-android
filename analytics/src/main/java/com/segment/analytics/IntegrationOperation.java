package com.segment.analytics;

import android.app.Activity;
import android.os.Bundle;
import com.segment.analytics.integrations.AbstractIntegration;
import com.segment.analytics.integrations.AliasPayload;
import com.segment.analytics.integrations.GroupPayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;

import static com.segment.analytics.Options.ALL_INTEGRATIONS_KEY;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/** Abstraction for a task that a {@link AbstractIntegration} can execute. */
abstract class IntegrationOperation {
  static boolean isIntegrationEnabled(ValueMap integrations, AbstractIntegration integration) {
    if (isNullOrEmpty(integrations)) {
      return true;
    }
    if (SegmentDispatcher.SEGMENT_KEY.equals(integration.key())) {
      // Always send to Segment
      return true;
    }
    boolean enabled = true;
    String key = integration.key();
    if (integrations.containsKey(key)) {
      enabled = integrations.getBoolean(key, true);
    } else if (integrations.containsKey(ALL_INTEGRATIONS_KEY)) {
      enabled = integrations.getBoolean(ALL_INTEGRATIONS_KEY, true);
    }
    return enabled;
  }

  static boolean isIntegrationEnabledInPlan(ValueMap plan, AbstractIntegration integration) {
    boolean eventEnabled = plan.getBoolean("enabled", true);
    if (eventEnabled) {
      // Check if there is an integration specific setting.
      ValueMap integrationPlan = plan.getValueMap("integrations");
      eventEnabled = isIntegrationEnabled(integrationPlan, integration);
    }
    return eventEnabled;
  }

  static IntegrationOperation onActivityCreated(final Activity activity, final Bundle bundle) {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        integration.onActivityCreated(activity, bundle);
      }

      @Override public String toString() {
        return "Activity Created";
      }
    };
  }

  static IntegrationOperation onActivityStarted(final Activity activity) {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        integration.onActivityStarted(activity);
      }

      @Override public String toString() {
        return "Activity Started";
      }
    };
  }

  static IntegrationOperation onActivityResumed(final Activity activity) {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        integration.onActivityResumed(activity);
      }

      @Override public String toString() {
        return "Activity Resumed";
      }
    };
  }

  static IntegrationOperation onActivityPaused(final Activity activity) {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        integration.onActivityPaused(activity);
      }

      @Override public String toString() {
        return "Activity Paused";
      }
    };
  }

  static IntegrationOperation onActivityStopped(final Activity activity) {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        integration.onActivityStopped(activity);
      }

      @Override public String toString() {
        return "Activity Stopped";
      }
    };
  }

  static IntegrationOperation onActivitySaveInstanceState(final Activity activity,
      final Bundle bundle) {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        integration.onActivitySaveInstanceState(activity, bundle);
      }

      @Override public String toString() {
        return "Activity Save Instance";
      }
    };
  }

  static IntegrationOperation onActivityDestroyed(final Activity activity) {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        integration.onActivityDestroyed(activity);
      }

      @Override public String toString() {
        return "Activity Destroyed";
      }
    };
  }

  static IntegrationOperation identify(final IdentifyPayload identifyPayload) {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        integration.identify(identifyPayload);
      }

      @Override public String toString() {
        return identifyPayload.toString();
      }
    };
  }

  static IntegrationOperation group(final GroupPayload groupPayload) {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        integration.group(groupPayload);
      }

      @Override public String toString() {
        return groupPayload.toString();
      }
    };
  }

  static IntegrationOperation track(final TrackPayload trackPayload) {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        ValueMap trackingPlan = projectSettings.trackingPlan();
        boolean trackEnabled = true;

        // If tracking plan is empty, leave the event enabled.
        if (!isNullOrEmpty(trackingPlan)) {
          String event = trackPayload.event();
          // If tracking plan has no settings for the event, leave the event enabled.
          if (trackingPlan.containsKey(event)) {
            ValueMap eventPlan = trackingPlan.getValueMap(event);
            trackEnabled = isIntegrationEnabledInPlan(eventPlan, integration);
          }
        }

        if (trackEnabled) {
          integration.track(trackPayload);
        }
      }

      @Override public String toString() {
        return trackPayload.toString();
      }
    };
  }

  static IntegrationOperation screen(final ScreenPayload screenPayload) {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        integration.screen(screenPayload);
      }

      @Override public String toString() {
        return screenPayload.toString();
      }
    };
  }

  static IntegrationOperation alias(final AliasPayload aliasPayload) {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        integration.alias(aliasPayload);
      }

      @Override public String toString() {
        return aliasPayload.toString();
      }
    };
  }

  static IntegrationOperation flush() {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        integration.flush();
      }

      @Override public String toString() {
        return "Flush";
      }
    };
  }

  static IntegrationOperation reset() {
    return new IntegrationOperation() {
      @Override public void run(AbstractIntegration integration, ProjectSettings projectSettings) {
        integration.reset();
      }

      @Override public String toString() {
        return "Reset";
      }
    };
  }

  private IntegrationOperation() {
  }

  /** Run this operation on the given integration. */
  abstract void run(AbstractIntegration integration, ProjectSettings projectSettings);
}
