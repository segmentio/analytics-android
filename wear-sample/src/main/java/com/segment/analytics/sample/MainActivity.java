package com.segment.analytics.sample;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.TextView;
import com.segment.analytics.wear.WearAnalytics;

public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    WearAnalytics.with(this).screen("Viewed Main Activity (Wear)", null, null);

    setContentView(R.layout.activity_main);
    final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
    stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
      @Override
      public void onLayoutInflated(WatchViewStub stub) {
        TextView textView = (TextView) findViewById(R.id.hello);
        textView.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            WearAnalytics.with(MainActivity.this).track("Said Hello!", null);
          }
        });
      }
    });
  }
}
