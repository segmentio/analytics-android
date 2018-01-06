package com.segment.analytics.runscope;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RunscopeService {
  @GET("buckets/{bucket}/messages")
  Call<MessagesResponse> messages(@Path("bucket") String bucket);

  @GET("buckets/{bucket}/messages/{message}")
  Call<MessageResponse> message(@Path("bucket") String bucket,
      @Path("message") String message);
}
