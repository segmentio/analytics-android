package io.segment.android.utils.test;

import io.segment.android.models.EasyJSONObject;
import io.segment.android.utils.Parameters;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import android.test.AndroidTestCase;

public class ParametersTest extends AndroidTestCase {

	@Test
	public void testMoveNoop() {
		
		EasyJSONObject json = new EasyJSONObject("hello", 123);
		
		EasyJSONObject moved = Parameters.move(json, new HashMap<String, String>());
		
		Assert.assertTrue(EasyJSONObject.equals(json, moved));
	}


	@Test
	public void testSimpleMove() {
		
		EasyJSONObject json = new EasyJSONObject("hello", 123, "firstName", "Ilya");
		EasyJSONObject expected = new EasyJSONObject("hello", 123, "$first_name", "Ilya");
		
		Map<String, String> map = new HashMap<String, String>();
		map.put("firstName", "$first_name");
		
		EasyJSONObject moved = Parameters.move(json, map);
		
		Assert.assertTrue(EasyJSONObject.equals(moved, expected));
	}
	
}
