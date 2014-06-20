package com.segment.android.request;

import com.segment.android.models.Batch;
import com.segment.android.models.EasyJSONObject;
import org.apache.http.HttpResponse;

public interface IRequester {

  public HttpResponse send(Batch batch);

  public EasyJSONObject fetchSettings();
}
