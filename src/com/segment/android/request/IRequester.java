package com.segment.android.request;


import org.apache.http.HttpResponse;

import com.segment.android.models.Batch;
import com.segment.android.models.EasyJSONObject;

public interface IRequester {
	
	public HttpResponse send(Batch batch);
	
	public EasyJSONObject fetchSettings();
	
}
