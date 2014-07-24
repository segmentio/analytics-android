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

package com.segment.android.utils;

import android.content.Context;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.segment.android.Logger;
import java.io.IOException;

import static com.segment.android.utils.Utils.checkNotMain;
import static com.segment.android.utils.Utils.isOnClasspath;

public final class AdvertisingIdProvider {

  private AdvertisingIdProvider() {
    throw new AssertionError("No instances allowed");
  }

  public static String get(Context context) {
    if (!isOnClasspath("com.google.android.gms.ads.identifier.AdvertisingIdClient")) {
      throw new IllegalStateException(
          "Must be called only if the Google Play Services are available.");
    }
    checkNotMain();

    try {
      AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
      if (adInfo.isLimitAdTrackingEnabled()) {
        return null;
      } else {
        return adInfo.getId();
      }
    } catch (IOException e) {
      Logger.e(e, "Unrecoverable error connecting to Google Play services");
    } catch (GooglePlayServicesNotAvailableException e) {
      Logger.e(e, "Google Play services is not available");
    } catch (GooglePlayServicesRepairableException e) {
      Logger.e(e, "Google Play services recoverable error");
    }
    return null;
  }
}
