/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment, Inc.
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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.segment.analytics.Analytics;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends Activity {
  @BindView(R.id.user_id) EditText userId;

  /** Returns true if the string is null, or empty (when trimmed). */
  public static boolean isNullOrEmpty(String text) {
    return TextUtils.isEmpty(text) || text.trim().length() == 0;
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
  }

  @OnClick(R.id.action_track_a) void onButtonAClicked() {
    Analytics.with(this).track("Button A Clicked");
  }

  @OnClick(R.id.action_track_b) void onButtonBClicked() {
    Analytics.with(this).track("Button B Clicked");
  }

  @OnClick(R.id.action_identify) void onIdentifyButtonClicked() {
    String id = userId.getText().toString();
    if (isNullOrEmpty(id)) {
      Toast.makeText(this, R.string.id_required, Toast.LENGTH_LONG).show();
    } else {
      Analytics.with(this).identify(id);
    }
  }

  @OnClick(R.id.action_flush) void onFlushButtonClicked() {
    Analytics.with(this).flush();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_view_docs) {
      Intent intent = new Intent(Intent.ACTION_VIEW,
          Uri.parse("https://segment.com/docs/tutorials/quickstart-android/"));
      try {
        startActivity(intent);
      } catch (ActivityNotFoundException e) {
        Toast.makeText(this, R.string.no_browser_available, Toast.LENGTH_LONG).show();
      }
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
  }
}
