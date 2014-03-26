package io.segment.android.request;

import java.util.Calendar;

import io.segment.android.Analytics;
import io.segment.android.Defaults;
import io.segment.android.Logger;
import io.segment.android.Options;
import io.segment.android.models.Batch;
import io.segment.android.models.EasyJSONObject;

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

import android.util.Log;

public class BasicRequester implements IRequester {

	@Override
	public HttpResponse send(Batch batch) {
		batch.setRequestTimestamp(Calendar.getInstance());

		Options options = Analytics.getOptions();

		String url = options.getHost() + Defaults.ENDPOINTS.get("import");
		String json = batch.toString();

		HttpClient httpclient = new DefaultHttpClient();
		HttpPost post = new HttpPost(url);
		post.setHeader("Content-Type", "application/json");

		try {
			ByteArrayEntity se = new ByteArrayEntity(json.getBytes());
			se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
					"application/json"));
			post.setEntity(se);

			return httpclient.execute(post);
		} catch (Exception e) {
			Logger.w("Failed to send request. " + Log.getStackTraceString(e));
		}

		return null;
	}

	@Override
	public EasyJSONObject fetchSettings() {

		Options options = Analytics.getOptions();

		String url = options.getHost()
				+ Defaults.getSettingsEndpoint(Analytics.getWriteKey());

		HttpClient httpclient = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);

		try {
			HttpResponse response = httpclient.execute(get);

			String json = EntityUtils.toString(response.getEntity());

			JSONObject jsonObject = new JSONObject(json);

			return new EasyJSONObject(jsonObject);
			
		} catch (Exception e) {
			Logger.w("Failed to send request. " + Log.getStackTraceString(e));
		}

		return null;
	}

}
