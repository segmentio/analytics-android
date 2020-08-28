package com.segment.analytics;

import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSMiddleware {
    protected List<Middleware> sourceMiddleware;
    protected Map<String, List<Middleware>> destinationMiddleware;
    protected Context context;
    protected HashMap<String, Object> settings;

    public JSMiddleware(Context context) {
        this.context = context;
    }
}
