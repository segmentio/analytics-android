package com.segment.analytics.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class SegmentServiceTest {

  SegmentService segmentService;

  @Before public void setUp() {
    initMocks(this);
  }

  @Test public void upload() throws IOException {
    SegmentService.StreamWriter writer = mock(SegmentService.StreamWriter.class);
    HttpURLConnection urlConnection = mock(HttpsURLConnection.class);
    final UrlConnectionFactory factory = mock(UrlConnectionFactory.class);
    OutputStream outputStream = new ByteArrayOutputStream();
    when(urlConnection.getOutputStream()).thenReturn(outputStream);
    when(urlConnection.getResponseCode()).thenReturn(200);

    segmentService = new SegmentService(Robolectric.application, Cartographer.INSTANCE, "foo") {
      @Override protected HttpURLConnection openConnection(String url) throws IOException {
        return factory.open(url);
      }
    };
    when(factory.open("https://api.segment.io/v1/import")).thenReturn(urlConnection);

    segmentService.upload(writer);

    verify(factory).open("https://api.segment.io/v1/import");
    verify(writer).write(outputStream);
  }

  interface UrlConnectionFactory {
    HttpURLConnection open(String url);
  }
}
