package com.segment.android.sample;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.segment.android.Analytics;
import com.segment.android.TrackedActivity;

public class MainActivity extends TrackedActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      Analytics.track("settings clicked");
      // todo: don't flush
      // This is just so the action shows up in the debugger right away, otherwise
      // events are batched, and I'll have to click this n times for it to actually send
      // Once we demo more actions in this app, I can get rid of this.
      Analytics.flush(true);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
