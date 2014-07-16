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

package com.segment.android.sample;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.segment.android.Analytics;
import com.segment.android.TrackedActivity;

public class MainActivity extends TrackedActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    initializeTrackers();
  }

  private void initializeTrackers() {
    findViewById(R.id.action_track_a).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Analytics.track("Button A clicked");
        Analytics.flush(true);
      }
    });
    findViewById(R.id.action_track_b).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Analytics.track("Button B clicked");
        Analytics.flush(true);
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
    if (id == R.id.action_docs) {
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
}
