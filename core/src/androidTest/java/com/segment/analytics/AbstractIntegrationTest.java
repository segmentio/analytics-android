package com.segment.analytics;

import android.app.Application;
import org.junit.Ignore;

import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

@Ignore public abstract class AbstractIntegrationTest {
  @Mock Application context;

  public void setUp() {
    initMocks(this);
  }

  public abstract void initialize() throws IllegalStateException;

  public abstract void activityCreate();

  public abstract void activityStart();

  public abstract void activityResume();

  public abstract void activityPause();

  public abstract void activityStop();

  public abstract void activitySaveInstance();

  public abstract void activityDestroy();

  public abstract void identify();

  public abstract void group();

  public abstract void track();

  public abstract void alias();

  public abstract void screen();

  public abstract void flush();
}
