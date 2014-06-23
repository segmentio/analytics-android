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

import android.content.Context;
import com.segment.android.Logger;
import com.segment.android.errors.InvalidSettingsException;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.utils.AndroidUtils;

public abstract class Integration implements IIntegration {

  private EasyJSONObject settings;
  private IntegrationState state = IntegrationState.NOT_INITIALIZED;
  protected boolean hasPermission = true;

  /**
   * Resets the base integration to a state of NOT_INITIALIZED, and resets the settings
   */
  public void reset() {
    settings = null;
    state = IntegrationState.NOT_INITIALIZED;
  }

  public void initialize(EasyJSONObject settings) throws InvalidSettingsException {
    if (this.settings != null) {
      if (!EasyJSONObject.equals(this.settings, settings)) {
        Logger.w(String.format("integration %s settings changed. %s => %s", getKey(), this.settings,
            settings));
      }
    }

    try {
      // try to validate the settings
      validate(settings);

      this.settings = settings;

      // if we get past validation, then its
      changeState(IntegrationState.INITIALIZED, new IntegrationState[] {
          // if the integration hasn't been initialized yet, then allow it to be initialized
          IntegrationState.NOT_INITIALIZED,
          // if the integration was invalid previously, but is now valid, you
          // can mark it as initialized
          IntegrationState.INVALID
      });
    } catch (InvalidSettingsException e) {
      // if we get past validation, then its
      changeState(IntegrationState.INVALID, new IntegrationState[] {
          // if the integration hasn't been initialized yet, then its settings could be marked
          // invalid
          IntegrationState.NOT_INITIALIZED
      });

      throw e;
    }
  }

  /**
   * Checks whether this integration has permission in this applicatio
   */
  public boolean checkPermission(Context context) {
    String[] permissions = getRequiredPermissions();
    for (String permission : permissions) {
      if (!AndroidUtils.permissionGranted(context, permission)) {
        Logger.w(
            String.format("integration %s requires permission %s but its not granted.", getKey(),
                permission)
        );
        changeState(IntegrationState.INVALID, new IntegrationState[] {
            IntegrationState.NOT_INITIALIZED, IntegrationState.INITIALIZED
        });
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the required permissions for this integration to run.
   */

  public abstract String[] getRequiredPermissions();

  /**
   * Enable this integration if its initialized or disabled.
   *
   * @return Whether or not this integration was enabled.
   */
  public boolean enable() {
    return changeState(IntegrationState.ENABLED, new IntegrationState[] {
        // if the integration already has the settings, it can be enabled
        IntegrationState.INITIALIZED,
        // if the integration is disabled, it can be re-enabled
        IntegrationState.DISABLED,
        // if the integration is already enabled, this is a no-op
        IntegrationState.ENABLED
    });
  }

  /**
   * Disable this integration.
   *
   * @return Whether or not this integration was disabled.
   */
  public boolean disable() {
    return changeState(IntegrationState.DISABLED, new IntegrationState[] {
        // if the integration already has the settings, it can be enabled
        IntegrationState.INITIALIZED,
        // if the integration is disabled, it can be re-enabled
        IntegrationState.DISABLED,
        // if the integration is already enabled, this is a no-op
        IntegrationState.ENABLED,
        // if the integration is already "ready for data", this is a no-op
        IntegrationState.READY
    });
  }

  private boolean changeState(IntegrationState to, IntegrationState[] acceptedFromStates) {

    // if we're not actually changing state, just return early
    if (state == to) return true;

    boolean acceptedState = false;
    for (IntegrationState from : acceptedFromStates) {
      if (state == from) {
        acceptedState = true;
        break;
      }
    }

    if (acceptedState) {
      state = to;
      return true;
    } else {
      Logger.w(
          "Integration " + getKey() + " cant be " + to + " because its in state " + state + ".");
      return false;
    }
  }

  /**
   * Called when the integration is ready to process data.
   *
   * @return Whether the integration was able to be marked as ready (initialized and enabled)
   */
  public boolean ready() {
    return changeState(IntegrationState.READY, new IntegrationState[] {
        // if the integration is enabled, then it can be transitioned to send data
        IntegrationState.ENABLED
    });
  }

  /**
   * Returns the state of this integration
   */
  public IntegrationState getState() {
    return state;
  }

  /**
   * Returns the settings for this integration.
   */
  public EasyJSONObject getSettings() {
    return settings;
  }

  /**
   * Returns the Segment.io key for this integration ("Mixpanel", "KISSMetrics", "Salesforce", etc
   * ..)
   * Check with friends@segment.io if you're not sure what the key is.
   */
  public abstract String getKey();

  /**
   * Validates that the provided settings are enough for this driver to perform its function.
   * Will throw {@link com.segment.android.errors.InvalidSettingsException} if the settings are not
   * enough.
   *
   * @param settings The Segment.io integration settings
   * @throws com.segment.android.errors.InvalidSettingsException An exception that says a field
   * setting is invalid
   */
  public abstract void validate(EasyJSONObject settings) throws InvalidSettingsException;
}
