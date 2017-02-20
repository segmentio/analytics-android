package com.segment.analytics.sample;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import com.segment.analytics.WearAnalytics;

public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    WearAnalytics.with(this).screen("Viewed Main Activity (Wear)", null, null);

    setContentView(R.layout.activity_main);

    final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
    stub.setOnLayoutInflatedListener(
        new WatchViewStub.OnLayoutInflatedListener() {
          @Override
          public void onLayoutInflated(WatchViewStub stub) {
            View view = findViewById(R.id.logo);
            view.setOnClickListener(
                new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {
                    view.animate().rotationBy(360);
                    WearAnalytics.with(MainActivity.this).track("Clicked Logo", null);
                  }
                });
          }
        });
  }
}
