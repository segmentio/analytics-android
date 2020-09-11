/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.segment.analytics.test;

import static com.segment.analytics.Utils.createContext;
import static com.segment.analytics.Utils.createTraits;

import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Options;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.integrations.TrackPayload;

@Deprecated
public class TrackPayloadBuilder {

    private AnalyticsContext context;
    private Traits traits;
    private String event;
    private Properties properties;
    private Options options;

    public TrackPayloadBuilder context(AnalyticsContext context) {
        this.context = context;
        return this;
    }

    public TrackPayloadBuilder traits(Traits traits) {
        this.traits = traits;
        return this;
    }

    public TrackPayloadBuilder event(String event) {
        this.event = event;
        return this;
    }

    public TrackPayloadBuilder properties(Properties properties) {
        this.properties = properties;
        return this;
    }

    public TrackPayloadBuilder options(Options options) {
        this.options = options;
        return this;
    }

    public TrackPayload build() {
        if (traits == null) {
            traits = createTraits();
        }
        if (event == null) {
            event = "bar";
        }
        if (context == null) {
            context = createContext(traits);
        }
        if (properties == null) {
            properties = new Properties();
        }
        if (options == null) {
            options = new Options();
        }
        return new TrackPayload.Builder()
                .event(event)
                .properties(properties)
                .anonymousId(traits.anonymousId())
                .context(context)
                .integrations(options.integrations())
                .build();
    }
}
