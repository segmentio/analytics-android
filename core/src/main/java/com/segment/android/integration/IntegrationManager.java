/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.segment.android.integration;

import android.app.Activity;
import android.content.Context;
import com.segment.android.Logger;
import com.segment.android.cache.SettingsCache;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.integrations.AmplitudeIntegration;
import com.segment.android.integrations.BugsnagIntegration;
import com.segment.android.integrations.CountlyIntegration;
import com.segment.android.integrations.CrittercismIntegration;
import com.segment.android.integrations.FlurryIntegration;
import com.segment.android.integrations.GoogleAnalyticsIntegration;
import com.segment.android.integrations.LocalyticsIntegration;
import com.segment.android.integrations.MixpanelIntegration;
import com.segment.android.integrations.QuantcastIntegration;
import com.segment.android.integrations.TapstreamIntegration;
import com.segment.android.models.Alias;
import com.segment.android.models.BasePayload;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Group;
import com.segment.android.models.Identify;
import com.segment.android.models.Screen;
import com.segment.android.models.Track;
import com.segment.android.stats.Stopwatch;
import java.util.LinkedList;
import java.util.List;

import static com.segment.android.utils.Utils.isNullOrEmpty;

public class IntegrationManager implements IIntegration {
  private final SettingsCache settingsCache;
  private final List<Integration> integrations;
  private boolean initialized;
  private final Context context;

  public IntegrationManager(SettingsCache settingsCache, Context context) {
    this.settingsCache = settingsCache;
    this.context = context.getApplicationContext();
    integrations = new LinkedList<Integration>();

    /**
     * Add New integrations Here
     */
    try {
      Class.forName("com.amplitude.api.Amplitude");
      this.addIntegration(new AmplitudeIntegration());
    } catch (ClassNotFoundException e) {
    }
    try {
      Class.forName("com.bugsnag.android.Bugsnag");
      this.addIntegration(new BugsnagIntegration());
    } catch (ClassNotFoundException e) {
    }
    try {
      Class.forName("ly.count.android.api.Countly");
      this.addIntegration(new CountlyIntegration());
    } catch (ClassNotFoundException e) {
    }
    try {
      Class.forName("com.crittercism.app.Crittercism");
      this.addIntegration(new CrittercismIntegration());
    } catch (ClassNotFoundException e) {
    }
    try {
      Class.forName("com.flurry.android.FlurryAgent");
      this.addIntegration(new FlurryIntegration());
    } catch (ClassNotFoundException e) {
    }
    try {
      Class.forName("com.google.android.gms.analytics.GoogleAnalytics");
      this.addIntegration(new GoogleAnalyticsIntegration());
    } catch (ClassNotFoundException e) {
    }
    try {
      Class.forName("com.localytics.android.LocalyticsSession");
      this.addIntegration(new LocalyticsIntegration());
    } catch (ClassNotFoundException e) {
    }
    try {
      Class.forName("com.mixpanel.android.mpmetrics.MixpanelAPI");
      this.addIntegration(new MixpanelIntegration());
    } catch (ClassNotFoundException e) {
    }
    try {
      Class.forName("com.quantcast.measurement.service.QuantcastClient");
      this.addIntegration(new QuantcastIntegration());
    } catch (ClassNotFoundException e) {
    }
    try {
      Class.forName("com.tapstream.sdk.Tapstream");
      this.addIntegration(new TapstreamIntegration());
    } catch (ClassNotFoundException e) {
    }
  }

  /**
   * Adds an integration to the integration manager
   */
  public void addIntegration(Integration integration) {
    if (isNullOrEmpty(integration.getKey())) {
      throw new IllegalArgumentException(
          "integration #getKey() " + "must return a non-null non-empty integration key.");
    }

    integrations.add(integration);
  }

  /**
   * Will run refresh if the integration manager hasn't yet
   * been initialized.
   *
   * @return Returns whether the integration manager has been initialized.
   */
  private boolean ensureInitialized() {
    if (!initialized) refresh();
    if (!initialized) {
      // we still haven't gotten any settings
      Logger.d("Integration manager waiting to be initialized ..");
    }
    return initialized;
  }

  public void refresh() {
    EasyJSONObject allSettings = settingsCache.getSettings();

    if (allSettings != null) {
      // we managed to get the settings

      for (Integration integration : integrations) {
        // iterate through all of the integrations we enable
        String integrationKey = integration.getKey();
        if (allSettings.has(integrationKey)) {
          // the settings has info for this integration
          // initialize the integration with those settings
          EasyJSONObject settings = new EasyJSONObject(allSettings.getObject(integrationKey));

          Logger.d("Downloaded settings for integration %s: %s", integration.getKey(),
              settings.toString());

          try {
            integration.initialize(settings);
            // enable the integration
            integration.enable();
            Logger.d("Initialized and enabled integration %s", integration.getKey());
            integration.onCreate(context);
          } catch (InvalidSettingsException e) {
            Logger.e(e, "Could not initialize integration %s", integration);
          }
        } else if (integration.getState().ge(IntegrationState.ENABLED)) {
          // if the setting was previously enabled but is no longer
          // in the settings, that means its been disabled
          integration.disable();
        } else {
          // settings don't mention this integration and its not enabled
          // so do nothing here
        }
      }

      // the integration manager has been initialized
      initialized = true;

      Logger.d("Initialized the Segment.io integration manager.");
    } else {
      Logger.d("Async settings aren't fetched yet, waiting ..");
    }
  }

  public boolean isInitialized() {
    return initialized;
  }

  public List<Integration> getIntegrations() {
    return integrations;
  }

  public SettingsCache getSettingsCache() {
    return settingsCache;
  }

  /**
   * A integration operation function
   */
  private interface IntegrationOperation {
    void run(Integration integration);
  }

  /**
   * Run an operation on all the integrations
   *
   * @param name Name of the operation
   * @param minimumState The minimum state that the integration has to be in before running the
   * operation
   * @param operation The actual operation to run on the integration
   */
  private void runOperation(String name, IntegrationState minimumState,
      IntegrationOperation operation) {

    // time the operation
    Stopwatch createOp = new Stopwatch("[All integrations] " + name);

    // make sure that the integration manager has settings from the server first
    if (ensureInitialized()) {

      for (Integration integration : this.integrations) {
        // if the integration is at least in the minimum state
        if (integration.getState().ge(minimumState)) {

          // time this integration's operation
          Stopwatch integrationOp = new Stopwatch("[" + integration.getKey() + "] " + name);

          operation.run(integration);

          integrationOp.end();
        }
      }
    }

    createOp.end();
  }

  public void toggleOptOut(final boolean optedOut) {
    runOperation("optOut", IntegrationState.INITIALIZED, new IntegrationOperation() {

      @Override
      public void run(Integration integration) {
        integration.toggleOptOut(optedOut);
      }
    });
  }

  public void checkPermissions(Context context) {
    for (Integration integration : this.integrations) {
      integration.checkPermission(context);
    }
  }

  @Override
  public void onCreate(final Context context) {
    checkPermissions(context);

    runOperation("onCreate", IntegrationState.INITIALIZED, new IntegrationOperation() {

      @Override
      public void run(Integration integration) {
        integration.onCreate(context);
      }
    });
  }

  @Override
  public void onActivityStart(final Activity activity) {

    runOperation("onActivityStart", IntegrationState.READY, new IntegrationOperation() {

      @Override
      public void run(Integration integration) {
        integration.onActivityStart(activity);
      }
    });
  }

  @Override
  public void onActivityResume(final Activity activity) {

    runOperation("onActivityResume", IntegrationState.READY, new IntegrationOperation() {

      @Override
      public void run(Integration integration) {
        integration.onActivityResume(activity);
      }
    });
  }

  @Override
  public void onActivityPause(final Activity activity) {

    runOperation("onActivityPause", IntegrationState.READY, new IntegrationOperation() {

      @Override
      public void run(Integration integration) {
        integration.onActivityPause(activity);
      }
    });
  }

  @Override
  public void onActivityStop(final Activity activity) {

    runOperation("onActivityStop", IntegrationState.READY, new IntegrationOperation() {

      @Override
      public void run(Integration integration) {
        integration.onActivityStop(activity);
      }
    });
  }

  /**
   * Determines if the integration is enabled for this action
   *
   * @param integration integration
   * @param action The action being processed
   */
  private boolean isIntegrationEnabled(Integration integration, BasePayload action) {
    boolean enabled = true;
    // look in the context.integrations to see which Bundled integrations should be disabled.
    // Payload.integrations is reserved for the server, where all bundled integrations are set to
    // false
    EasyJSONObject integrations = (EasyJSONObject) action.getContext().get("integrations");
    if (integrations != null) {
      String key = integration.getKey();
      if (integrations.has("all")) enabled = integrations.getBoolean("all", true);
      if (integrations.has("All")) enabled = integrations.getBoolean("All", true);
      if (integrations.has(key)) enabled = integrations.getBoolean(key, true);
    }
    return enabled;
  }

  @Override
  public void identify(final Identify identify) {
    runOperation("Identify", IntegrationState.READY, new IntegrationOperation() {

      @Override
      public void run(Integration integration) {
        if (isIntegrationEnabled(integration, identify)) integration.identify(identify);
      }
    });
  }

  @Override
  public void group(final Group group) {
    runOperation("Group", IntegrationState.READY, new IntegrationOperation() {

      @Override
      public void run(Integration integration) {
        if (isIntegrationEnabled(integration, group)) integration.group(group);
      }
    });
  }

  @Override
  public void track(final Track track) {
    runOperation("Track", IntegrationState.READY, new IntegrationOperation() {
      @Override
      public void run(Integration integration) {
        if (isIntegrationEnabled(integration, track)) integration.track(track);
      }
    });
  }

  @Override
  public void screen(final Screen screen) {

    runOperation("Screen", IntegrationState.READY, new IntegrationOperation() {

      @Override
      public void run(Integration integration) {

        if (isIntegrationEnabled(integration, screen)) integration.screen(screen);
      }
    });
  }

  @Override
  public void alias(final Alias alias) {

    runOperation("Alias", IntegrationState.READY, new IntegrationOperation() {

      @Override
      public void run(Integration integration) {

        if (isIntegrationEnabled(integration, alias)) integration.alias(alias);
      }
    });
  }

  public void reset() {

    runOperation("Reset", IntegrationState.READY, new IntegrationOperation() {

      @Override
      public void run(Integration integration) {

        integration.reset();
      }
    });
  }

  public void flush() {

    runOperation("Flush", IntegrationState.READY, new IntegrationOperation() {

      @Override
      public void run(Integration integration) {

        integration.flush();
      }
    });
  }
}
