package io.segment.android.request;

import io.segment.android.models.Batch;

import org.apache.http.HttpResponse;

public interface IRequester {
	
	public HttpResponse send(Batch batch);
	
}
