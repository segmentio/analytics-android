/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.segment.android.request;

import android.util.Base64;
import com.segment.android.Analytics;
import com.segment.android.Config;
import com.segment.android.Defaults;
import com.segment.android.Logger;
import com.segment.android.models.Batch;
import com.segment.android.models.EasyJSONObject;
import java.util.Calendar;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class BasicRequester implements IRequester {

  @Override
  public HttpResponse send(Batch batch) {
    batch.setSentAt(Calendar.getInstance());

    Config options = Analytics.getOptions();

    String url = options.getHost() + Defaults.ENDPOINTS.get("import");
    String json = batch.toString();

    HttpClient httpclient = new DefaultHttpClient();
    HttpPost post = new HttpPost(url);
    post.setHeader("Content-Type", "application/json");

    // Basic Authentication
    // https://segment.io/docs/tracking-api/reference/#authentication
    post.addHeader("Authorization", basicAuthHeader());

    try {
      ByteArrayEntity se = new ByteArrayEntity(json.getBytes());
      se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
      post.setEntity(se);
      return httpclient.execute(post);
    } catch (Exception e) {
      Logger.w(e, "Failed to send request.");
    }

    return null;
  }

  @Override
  public EasyJSONObject fetchSettings() {
    Config options = Analytics.getOptions();

    String url = options.getHost() + Defaults.getSettingsEndpoint(Analytics.getWriteKey());

    HttpClient httpclient = new DefaultHttpClient();
    HttpGet get = new HttpGet(url);

    try {
      HttpResponse response = httpclient.execute(get);
      String json = EntityUtils.toString(response.getEntity());
      JSONObject jsonObject = new JSONObject(json);
      return new EasyJSONObject(jsonObject);
    } catch (Exception e) {
      Logger.w(e, "Failed to send request.");
    }

    return null;
  }

  private String basicAuthHeader() {
    return "Basic " + Base64.encodeToString((Analytics.getWriteKey() + ":").getBytes(),
        Base64.NO_WRAP);
  }
}
