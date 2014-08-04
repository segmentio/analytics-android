package com.segment.android.cache;

import android.content.Context;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.segment.android.Logger;
import java.io.IOException;

/** Abstraction so we don't try to load the required classes if unavailable. */
class AdvertisingId {
  static String get(Context context) {
    try {
      AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(context);
      if (info.isLimitAdTrackingEnabled()) {
        Logger.d("User has limited ad tracking, skipping advertising ID.");
        return null;
      }
      return info.getId();
    } catch (GooglePlayServicesRepairableException e) {
      Logger.e(e, "Encountered an error connecting to Google Play Services.");
    } catch (GooglePlayServicesNotAvailableException e) {
      Logger.e(e, "Google Play Services not installed.");
    } catch (IOException e) {
      Logger.e(e, "Encountered an error connecting to Google Play Services.");
    } catch (Exception e) {
      Logger.e(e, "Unknown exception!");
    }
    return null;
  }
}
