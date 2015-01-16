package com.segment.analytics.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.net.ssl.HttpsURLConnection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class SegmentClientTest {

  @Mock SegmentClient.HttpURLConnectionFactory factory;
  SegmentClient segmentClient;

  @Before public void setUp() {
    initMocks(this);
    segmentClient =
        new SegmentClient(Robolectric.application, Cartographer.INSTANCE, factory,
            "foo");
  }

  @Test public void upload() throws IOException {
    SegmentClient.StreamWriter writer = mock(SegmentClient.StreamWriter.class);
    HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
    OutputStream outputStream = new ByteArrayOutputStream();
    when(urlConnection.getOutputStream()).thenReturn(outputStream);
    when(urlConnection.getResponseCode()).thenReturn(200);
    when(factory.open("https://api.segment.io/v1/import")).thenReturn(urlConnection);

    segmentClient.upload(writer);

    verify(factory).open("https://api.segment.io/v1/import");
    verify(writer).write(outputStream);
  }
}
