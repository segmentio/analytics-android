package io.segment.android.request;

import io.segment.android.Analytics;
import io.segment.android.Defaults;
import io.segment.android.Options;
import io.segment.android.models.Batch;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import android.util.Log;

public class BasicRequester implements IRequester {

	private static final String TAG = BasicRequester.class.getName();
	
	@Override
	public HttpResponse send(Batch batch) {
		
		Options options = Analytics.getOptions();
		
		String url = options.getHost() + Defaults.ENDPOINTS.get("import");
		String json = batch.toString();
		
		HttpClient httpclient = new DefaultHttpClient(); 
		HttpPost post = new HttpPost(url);
		post.setHeader("Content-Type", "application/json");

		try {
		      StringEntity se = new StringEntity(json);  
		      se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
		      post.setEntity(se);
		      
		      return httpclient.execute(post);
		}
		catch(Exception e){
		    Log.w(TAG, "Failed to send request. " + 
		    		Log.getStackTraceString(e));
		}
		
		return null;
	}

}
