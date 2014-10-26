package com.segment.analytics;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import java.util.UUID;
import org.robolectric.Robolectric;

import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

// Named Exam so that Junit and Robolectric don't try to run this
public class IntegrationExam {
  String anonymousId;
  String userId;
  AnalyticsContext analyticsContext;
  Traits traits;
  Options options;
  Properties properties;
  Bundle bundle;

  @Mock Activity activity;
  @Mock Context context;

  public void setUp() {
    anonymousId = UUID.randomUUID().toString();
    userId = "segment";
    options = new Options();
    traits = new Traits();
    analyticsContext = new AnalyticsContext(Robolectric.application, traits);
    properties = new Properties();
    bundle = new Bundle();
    initMocks(this);
  }

  TrackPayload trackPayload(String event) {
    return new TrackPayload(anonymousId, analyticsContext, userId, event, properties, options);
  }

  ScreenPayload screenPayload(String category, String name) {
    return new ScreenPayload(anonymousId, analyticsContext, userId, category, name, properties,
        options);
  }
}
