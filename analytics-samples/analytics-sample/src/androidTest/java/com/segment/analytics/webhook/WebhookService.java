package com.segment.analytics.webhook;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface WebhookService {
  @GET("buckets/{bucket}")
  Call<List<String>> messages(@Path("bucket") String bucket, @Query("limit") int limit);
}
