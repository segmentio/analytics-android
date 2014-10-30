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

package com.segment.analytics.sample;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.localytics.android.LocalyticsSession;
import com.segment.analytics.Analytics;
import com.segment.analytics.Options;
import com.segment.analytics.Properties;
import com.segment.analytics.StatsSnapshot;

public class MainActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    initViews();

    Analytics.with(this).onIntegrationReady(new Analytics.OnIntegrationReadyListener() {
      @Override public void onIntegrationReady(String key, Object integration) {
        if ("Localytics".equals(key)) {
          LocalyticsSession.setLoggingEnabled(true);
        }
      }
    });
  }

  private void initViews() {
    findViewById(R.id.action_track_a).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Analytics.with(MainActivity.this).track("Button A clicked");
      }
    });
    findViewById(R.id.action_track_b).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Analytics.with(MainActivity.this).track("Button B clicked");
      }
    });
    findViewById(R.id.action_track_c).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Analytics.with(MainActivity.this).track("Button C clicked");
      }
    });
    findViewById(R.id.action_track_custom_event).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        String event =
            ((EditText) findViewById(R.id.action_custom_event_name)).getText().toString();
        if (isNullOrEmpty(event)) {
          Toast.makeText(MainActivity.this, R.string.name_required, Toast.LENGTH_LONG).show();
        } else {
          Analytics.with(MainActivity.this).track(event);
        }
      }
    });
    findViewById(R.id.action_identify).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        String id = ((EditText) findViewById(R.id.action_identify_id)).getText().toString();
        if (isNullOrEmpty(id)) {
          Toast.makeText(MainActivity.this, R.string.id_required, Toast.LENGTH_LONG).show();
        } else {
          Analytics.with(MainActivity.this).identify(id);
        }
      }
    });
    findViewById(R.id.action_flush).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Analytics.with(MainActivity.this).flush();
      }
    });
    findViewById(R.id.action_test_sequence).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Analytics.with(MainActivity.this)
            .track("Not for native Amplitude", new Properties(),
                new Options().setIntegration("Amplitude", false));
      }
    });
    findViewById(R.id.action_update_stats).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        StatsSnapshot snapshot = Analytics.with(MainActivity.this).getSnapshot();
        ((TextView) findViewById(R.id.stat_flushed_events)).setText(
            "Number of events flushed: " + (snapshot.flushEventCount));
        ((TextView) findViewById(R.id.stat_flush_count)).setText(
            "Number of times flushed: " + snapshot.flushCount);
        ((TextView) findViewById(R.id.stat_flush_count)).setText(
            "Total operations sent to bundled integrations (this is the total "
                + "analytics events, flush events, and activity lifecycle events): "
                + snapshot.integrationOperationCount
        );
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_view_docs) {
      Intent intent = new Intent(Intent.ACTION_VIEW,
          Uri.parse("https://segment.io/docs/tutorials/quickstart-android/"));
      try {
        startActivity(intent);
      } catch (ActivityNotFoundException e) {
        Toast.makeText(this, R.string.no_browser_available, Toast.LENGTH_LONG).show();
      }
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /** Returns true if the string is null, or empty (when trimmed). */
  public static boolean isNullOrEmpty(String text) {
    // Rather than using text.trim().length() == 0, use getTrimmedLength to avoid allocating an
    // extra string object
    return TextUtils.isEmpty(text) || TextUtils.getTrimmedLength(text) == 0;
  }
}
