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

package com.segment.android.request.test;

import com.segment.android.Logger;
import com.segment.android.models.BasePayload;
import com.segment.android.models.Batch;
import com.segment.android.models.EasyJSONObject;
import com.segment.android.models.Identify;
import com.segment.android.models.Traits;
import com.segment.android.request.BasicRequester;
import com.segment.android.request.IRequester;
import com.segment.android.test.BaseTest;
import com.segment.android.test.Constants;
import com.segment.android.test.TestCases;
import java.util.LinkedList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

// The big question: requester or requestor? : 
// http://english.stackexchange.com/questions/29254/whats-the-difference-between-requester-and-requestor
public class BasicRequesterTest extends BaseTest {

  private IRequester requester;

  @Override
  protected void setUp() {
    super.setUp();

    requester = new BasicRequester();
  }

  @Test
  public void testSimpleBatchRequest() {
    HttpResponse response = requester.send(TestCases.batch(Constants.WRITE_KEY));
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testUTF8Characters() {
    Identify identify = new Identify("some_user",
        new Traits("carrier", "GR COSMOTE", "language", "????????????????", "country",
            "????????????"), new EasyJSONObject(), null
    );

    List<BasePayload> items = new LinkedList<BasePayload>();
    items.add(identify);

    Batch batch = new Batch(Constants.WRITE_KEY, items);
    HttpResponse response = requester.send(batch);
    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testSimpleSettingsRequest() {
    EasyJSONObject settings = requester.fetchSettings();

    assertThat(settings).isNotNull();
    assertThat(settings.length()).isGreaterThan(0);

    String[] keys = { "Segment.io" };

    for (String key : keys) {
      if (settings.getObject(key) == null) {
        Logger.e("%s is not in settings!", key);
      }
      assertThat(settings.getObject(key)).isNotNull();
    }
  }
}
