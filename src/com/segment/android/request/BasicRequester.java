package com.segment.android.request;

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

import com.segment.android.Analytics;
import com.segment.android.Defaults;
import com.segment.android.Logger;
import com.segment.android.Config;
import com.segment.android.models.Batch;
import com.segment.android.models.EasyJSONObject;

import android.util.Base64;
import android.util.Log;

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

		Config options = Analytics.getOptions();

		String url = options.getHost()
				+ Defaults.getSettingsEndpoint(Analytics.getWriteKey());

		HttpClient httpclient = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		
		// Basic Authentication
		// https://segment.io/docs/tracking-api/reference/#authentication
		get.addHeader("Authorization", basicAuthHeader());

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

	private String basicAuthHeader() {
		return "Basic " + Base64.encodeToString(
				(Analytics.getWriteKey()+":").getBytes(), Base64.DEFAULT);
	}
	
}
