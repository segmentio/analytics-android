package com.segment.analytics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.net.ssl.HttpsURLConnection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class SegmentHTTPApiRobolectricTest {

  @Mock SegmentHTTPApi.HttpURLConnectionFactory factory;
  SegmentHTTPApi segmentHTTPApi;

  @Before public void setUp() {
    initMocks(this);
    segmentHTTPApi = new SegmentHTTPApi("foo", factory);
  }

  @Test public void streamWriterCalledCorrectly() throws IOException {
    SegmentHTTPApi.StreamWriter writer = mock(SegmentHTTPApi.StreamWriter.class);
    HttpsURLConnection urlConnection = mock(HttpsURLConnection.class);
    OutputStream outputStream = new ByteArrayOutputStream();
    when(urlConnection.getOutputStream()).thenReturn(outputStream);
    when(urlConnection.getResponseCode()).thenReturn(200);

    when(factory.open("v1/import")).thenReturn(urlConnection);

    segmentHTTPApi.upload(writer);
    verify(factory).open("v1/import");
    verify(writer).write(outputStream);
  }
}
