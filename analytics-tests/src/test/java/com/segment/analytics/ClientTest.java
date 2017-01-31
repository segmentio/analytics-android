package com.segment.analytics;

import android.net.Uri;
import com.segment.analytics.core.tests.BuildConfig;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import com.squareup.okhttp.mockwebserver.rule.MockWebServerRule;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = NONE)
public class ClientTest {

  @Rule public MockWebServerRule server = new MockWebServerRule();
  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private Client client;
  private Client mockClient;
  HttpURLConnection mockConnection;

  @Before public void setUp() {
    mockConnection = mock(HttpURLConnection.class);

    client = new Client("foo", new ConnectionFactory() {
      @Override protected HttpURLConnection openConnection(String url) throws IOException {
        String path = Uri.parse(url).getPath();
        URL mockServerURL = server.getUrl(path);
        return super.openConnection(mockServerURL.toString());
      }
    });

    mockClient = new Client("foo", new ConnectionFactory() {
      @Override protected HttpURLConnection openConnection(String url) throws IOException {
        return mockConnection;
      }
    });
  }

  @Test public void upload() throws Exception {
    server.enqueue(new MockResponse());

    Client.Connection connection = client.upload();
    assertThat(connection.os).isNotNull();
    assertThat(connection.is).isNull();
    assertThat(connection.connection.getResponseCode()).isEqualTo(200); // consume the response
    RecordedRequestAssert.assertThat(server.takeRequest())
        .hasRequestLine("POST /v1/import HTTP/1.1")
        .containsHeader("Content-Type", "application/json")
        .containsHeader("Content-Encoding", "gzip")
        .containsHeader("Authorization", "Basic Zm9vOg==");
  }

  @Test public void closingUploadConnectionClosesStreams() throws Exception {
    OutputStream os = mock(OutputStream.class);
    when(mockConnection.getOutputStream()).thenReturn(os);
    when(mockConnection.getResponseCode()).thenReturn(200);

    Client.Connection connection = mockClient.upload();
    verify(mockConnection).setDoOutput(true);
    verify(mockConnection).setChunkedStreamingMode(0);

    connection.close();
    verify(mockConnection).disconnect();
    verify(os).close();
  }

  @Test public void closingUploadConnectionClosesStreamsForNon200Response() throws Exception {
    OutputStream os = mock(OutputStream.class);
    when(mockConnection.getOutputStream()).thenReturn(os);
    when(mockConnection.getResponseCode()).thenReturn(202);

    Client.Connection connection = mockClient.upload();
    verify(mockConnection).setDoOutput(true);
    verify(mockConnection).setChunkedStreamingMode(0);

    connection.close();
    verify(mockConnection).disconnect();
    verify(os).close();
  }

  @Test public void uploadFailureClosesStreamsAndThrowsException() throws Exception {
    OutputStream os = mock(OutputStream.class);
    InputStream is = mock(InputStream.class);
    when(mockConnection.getOutputStream()).thenReturn(os);
    when(mockConnection.getResponseCode()).thenReturn(300);
    when(mockConnection.getResponseMessage()).thenReturn("bar");
    when(mockConnection.getInputStream()).thenReturn(is);

    Client.Connection connection = mockClient.upload();
    verify(mockConnection).setDoOutput(true);
    verify(mockConnection).setChunkedStreamingMode(0);

    try {
      connection.close();
      fail(">= 300 return code should throw an exception");
    } catch (Client.HTTPException e) {
      assertThat(e).hasMessage("HTTP 300: bar. "
          + "Response: Could not read response body for rejected message: "
          + "java.io.IOException: Underlying input stream returned zero bytes");
    }
    verify(mockConnection).disconnect();
    verify(os).close();
  }

  @Test public void fetchSettings() throws Exception {
    server.enqueue(new MockResponse());

    Client.Connection connection = client.fetchSettings();
    assertThat(connection.os).isNull();
    assertThat(connection.is).isNotNull();
    assertThat(connection.connection.getResponseCode()).isEqualTo(200);
    RecordedRequestAssert.assertThat(server.takeRequest())
        .hasRequestLine("GET /v1/projects/foo/settings HTTP/1.1")
        .containsHeader("Content-Type", "application/json");
  }

  @Test public void fetchSettingsFailureClosesStreamsAndThrowsException() throws Exception {
    when(mockConnection.getResponseCode()).thenReturn(204);
    when(mockConnection.getResponseMessage()) //
        .thenReturn("no cookies for you http://bit.ly/1EMHBNb");

    try {
      mockClient.fetchSettings();
      fail("Non 200 return code should throw an exception");
    } catch (IOException e) {
      assertThat(e).hasMessage("HTTP " + 204 + ": no cookies for you http://bit.ly/1EMHBNb");
    }
    verify(mockConnection).disconnect();
  }

  @Test public void closingFetchSettingsClosesStreams() throws Exception {
    InputStream is = mock(InputStream.class);
    when(mockConnection.getInputStream()).thenReturn(is);
    when(mockConnection.getResponseCode()).thenReturn(200);

    Client.Connection connection = mockClient.fetchSettings();

    connection.close();
    verify(mockConnection).disconnect();
    verify(is).close();
  }

  static class RecordedRequestAssert
      extends AbstractAssert<RecordedRequestAssert, RecordedRequest> {

    static RecordedRequestAssert assertThat(RecordedRequest recordedRequest) {
      return new RecordedRequestAssert(recordedRequest);
    }

    protected RecordedRequestAssert(RecordedRequest actual) {
      super(actual, RecordedRequestAssert.class);
    }

    public RecordedRequestAssert containsHeader(String name, String expectedHeader) {
      isNotNull();
      String actualHeader = actual.getHeader(name);
      Assertions.assertThat(actualHeader)
          .overridingErrorMessage("Expected header <%s> to be <%s> but was <%s>.", name,
              expectedHeader, actualHeader)
          .isEqualTo(expectedHeader);
      return this;
    }

    public RecordedRequestAssert containsHeader(String name) {
      isNotNull();
      String actualHeader = actual.getHeader(name);
      Assertions.assertThat(actualHeader)
          .overridingErrorMessage("Expected header <%s> to not be empty but was.", name,
              actualHeader)
          .isNotNull()
          .isNotEmpty();
      return this;
    }

    public RecordedRequestAssert hasRequestLine(String requestLine) {
      isNotNull();
      String actualRequestLine = actual.getRequestLine();
      Assertions.assertThat(actualRequestLine)
          .overridingErrorMessage("Expected requestLine <%s> to be <%s> but was not.",
              actualRequestLine, requestLine)
          .isEqualTo(requestLine);
      return this;
    }
  }
}
