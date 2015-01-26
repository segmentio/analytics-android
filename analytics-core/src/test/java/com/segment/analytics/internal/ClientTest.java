package com.segment.analytics.internal;

import android.app.Activity;
import android.net.Uri;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import com.squareup.okhttp.mockwebserver.rule.MockWebServerRule;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class ClientTest {

  @Rule public MockWebServerRule server = new MockWebServerRule();
  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private Client client;
  private Client mockClient;
  private HttpURLConnection mockConnection;

  @Before public void setUp() {
    Activity activity = Robolectric.buildActivity(Activity.class).get();
    mockConnection = mock(HttpURLConnection.class);
    client = new Client(activity, "foo") {
      @Override protected HttpURLConnection openConnection(String url) throws IOException {
        String path = Uri.parse(url).getPath();
        return (HttpURLConnection) server.getUrl(path).openConnection();
      }
    };
    mockClient = new Client(activity, "foo") {
      @Override protected HttpURLConnection openConnection(String url) throws IOException {
        return mockConnection;
      }
    };
  }

  @Test public void upload() throws Exception {
    server.enqueue(new MockResponse());

    Client.Response response = client.upload();
    assertThat(response.os).isNotNull();
    assertThat(response.is).isNull();
    assertThat(response.connection.getResponseCode()).isEqualTo(200); // consume the response
    RecordedRequestAssert.assertThat(server.takeRequest())
        .hasRequestLine("POST /v1/import HTTP/1.1")
        .containsHeader("Content-Type", "application/json")
        .containsHeader("Authorization", "Basic Zm9vOg==");
  }

  @Test public void uploadClosesConnections() throws Exception {
    OutputStream os = mock(OutputStream.class);
    when(mockConnection.getOutputStream()).thenReturn(os);
    when(mockConnection.getResponseCode()).thenReturn(200);

    Client.Response response = mockClient.upload();
    verify(mockConnection).setDoOutput(true);
    verify(mockConnection).setChunkedStreamingMode(0);

    response.close();
    verify(mockConnection).disconnect();
    verify(os).close();
  }

  @Test public void uploadClosesConnectionsAndThrowsExceptionOnFailure() throws Exception {
    OutputStream os = mock(OutputStream.class);
    when(mockConnection.getOutputStream()).thenReturn(os);
    when(mockConnection.getResponseCode()).thenReturn(201);
    when(mockConnection.getResponseMessage()).thenReturn("bar");

    Client.Response response = mockClient.upload();
    verify(mockConnection).setDoOutput(true);
    verify(mockConnection).setChunkedStreamingMode(0);

    try {
      response.close();
      fail("Non 200 return code should throw an exception");
    } catch (IOException e) {
      assertThat(e).hasMessage(201 + " bar");
    }
    verify(mockConnection).disconnect();
    verify(os).close();
  }

  @Test public void fetchSettings() throws Exception {
    server.enqueue(new MockResponse());

    Client.Response response = client.fetchSettings();
    assertThat(response.os).isNull();
    assertThat(response.is).isNotNull();
    assertThat(response.connection.getResponseCode()).isEqualTo(200);
    RecordedRequestAssert.assertThat(server.takeRequest())
        .hasRequestLine("GET /project/foo/settings HTTP/1.1")
        .containsHeader("Content-Type", "application/json");
  }

  @Test public void fetchSettingsClosesConnectionsAndThrowsExceptionOnFailure() throws Exception {
    when(mockConnection.getResponseCode()).thenReturn(204);
    when(mockConnection.getResponseMessage()).thenReturn("bar");

    try {
      mockClient.fetchSettings();
      fail("Non 200 return code should throw an exception");
    } catch (IOException e) {
      assertThat(e).hasMessage(204 + " bar");
    }
    verify(mockConnection).disconnect();
  }

  @Test public void fetchSettingsClosesConnections() throws Exception {
    InputStream is = mock(InputStream.class);
    when(mockConnection.getInputStream()).thenReturn(is);
    when(mockConnection.getResponseCode()).thenReturn(200);

    Client.Response response = mockClient.fetchSettings();

    response.close();
    verify(mockConnection).disconnect();
    verify(is).close();
  }

  @Test public void downloadFile() throws Exception {
    server.enqueue(new MockResponse().setBody("foo")); // todo: test with a real file
    File file = new File(folder.getRoot(), "bar.jar");
    assertThat(file).doesNotExist();

    client.downloadFile("http://localhost/bar.jar", file);

    assertThat(file).exists().hasContent("foo");
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
