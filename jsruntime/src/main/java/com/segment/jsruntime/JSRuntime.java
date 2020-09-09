package com.segment.jsruntime;

import android.util.Log;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.content.ContentValues.TAG;

public class JSRuntime {
    public V8 runtime = V8.createV8Runtime();

    class Console {
        public void log(final String message) {
            System.out.println("[INFO] " + message);
        }
        public void error(final String message) {
            System.out.println("[ERROR] " + message);
        }
    }

    public JSRuntime(InputStream bundleStream) {
        this.setupRuntime(bundleStream);
    }

    private String loadScript(InputStream fileStream) {
        BufferedReader in = null;
        try {
            StringBuilder buf = new StringBuilder();
            InputStream is = fileStream;
            in = new BufferedReader(new InputStreamReader(is));

            String str;
            boolean isFirst = true;
            while ( (str = in.readLine()) != null ) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append('\n');
                buf.append(str);
            }
            return buf.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error loading JS bundle.");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing JS bundle.");
                }
            }
        }

        return null;
    }

    private void setupRuntime(InputStream bundleStream) {
        // setup the runtime to handle errors and logging.
        /*Console console = new Console();
        V8Object v8Console = new V8Object(runtime);
        runtime.add("console", v8Console);
        v8Console.registerJavaMethod(console, "log", "log", new Class[] { String.class });
        v8Console.registerJavaMethod(console, "err", "err", new Class[] { String.class });*/

        // load the JS bundle script.
        String script = loadScript(bundleStream);
        runtime.executeScript(script);
    }

    public V8Object getObject(String key) {
        V8Object result = runtime.getObject(key);
        if (result.isUndefined()) {
            result = runtime.executeObjectScript(key);
        }
        return result;
    }

    public V8Array getArray(String key) {
        V8Array result = runtime.getArray(key);
        if (result.isUndefined()) {
            result = runtime.executeArrayScript(key);
        }
        return result;
    }
}
