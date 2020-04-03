
package com.snowplowanalytics.react.tracker;

import java.util.UUID;
import java.util.Map;

import javax.security.auth.Subject;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.snowplowanalytics.react.util.EventUtil;
import com.snowplowanalytics.snowplow.tracker.Emitter;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.events.SelfDescribing;
import com.snowplowanalytics.snowplow.tracker.events.Structured;
import com.snowplowanalytics.snowplow.tracker.events.ScreenView;
import com.snowplowanalytics.snowplow.tracker.events.PageView;
import com.snowplowanalytics.snowplow.tracker.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;

public class RNSnowplowTrackerModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private Tracker tracker;
    private Emitter emitter;

    public RNSnowplowTrackerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNSnowplowTracker";
    }

    @ReactMethod
    public void initialize(String endpoint, String method, String protocol, String namespace, String appId,
            ReadableMap options) {
        this.emitter = new Emitter.EmitterBuilder(endpoint, this.reactContext)
                .method(method.equalsIgnoreCase("post") ? HttpMethod.POST : HttpMethod.GET)
                .security(protocol.equalsIgnoreCase("https") ? RequestSecurity.HTTPS : RequestSecurity.HTTP)
                .option(BufferOption.Single).build();
        this.emitter.waitForEventStore();
        com.snowplowanalytics.snowplow.tracker.Subject subject = new com.snowplowanalytics.snowplow.tracker.Subject.SubjectBuilder()
                .build();
        if (options.hasKey("userId") && options.getString("userId") != null && !options.getString("userId").isEmpty()) {
            subject.setUserId(options.getString("userId"));
        }
        if (options.hasKey("screenWidth") && options.hasKey("screenHeight")) {
            subject.setScreenResolution(options.getInt("screenWidth"), options.getInt("screenHeight"));
        }
        if (options.hasKey("colorDepth")) {
            subject.setColorDepth(options.getInt("colorDepth"));
        }
        if (options.hasKey("timezone") && options.getString("timezone") != null
                && !options.getString("timezone").isEmpty()) {
            subject.setTimezone(options.getString("timezone"));
        }
        if (options.hasKey("language") && options.getString("language") != null
                && !options.getString("language").isEmpty()) {
            subject.setLanguage(options.getString("language"));
        }
        if (options.hasKey("ipAddress") && options.getString("ipAddress") != null
                && !options.getString("ipAddress").isEmpty()) {
            subject.setIpAddress(options.getString("ipAddress"));
        }
        if (options.hasKey("useragent") && options.getString("useragent") != null
                && !options.getString("useragent").isEmpty()) {
            subject.setUseragent(options.getString("useragent"));
        }
        if (options.hasKey("networkUserId") && options.getString("networkUserId") != null
                && !options.getString("networkUserId").isEmpty()) {
            subject.setNetworkUserId(options.getString("networkUserId"));
        }
        if (options.hasKey("domainUserId") && options.getString("domainUserId") != null
                && !options.getString("domainUserId").isEmpty()) {
            subject.setDomainUserId(options.getString("domainUserId"));
        }
        this.tracker = Tracker.init(new Tracker.TrackerBuilder(this.emitter, namespace, appId, this.reactContext)
                // setSubject/UserId
                .subject(subject)
                // setBase64Encoded
                .base64(options.hasKey("setBase64Encoded") ? options.getBoolean("setBase64Encoded") : false)
                // setPlatformContext
                .mobileContext(options.hasKey("setPlatformContext") ? options.getBoolean("setPlatformContext") : false)
                .screenviewEvents(options.hasKey("autoScreenView") ? options.getBoolean("autoScreenView") : false)
                .sessionContext(options.hasKey("setSessionContext") ? options.getBoolean("setSessionContext") : false)
                // setApplicationContext
                .applicationContext(
                        options.hasKey("setApplicationContext") ? options.getBoolean("setApplicationContext") : false)
                // setSessionContext
                .sessionContext(options.hasKey("setSessionContext") ? options.getBoolean("setSessionContext") : false)
                .sessionCheckInterval(options.hasKey("checkInterval") ? options.getInt("checkInterval") : 15)
                .foregroundTimeout(options.hasKey("foregroundTimeout") ? options.getInt("foregroundTimeout") : 600)
                .backgroundTimeout(options.hasKey("backgroundTimeout") ? options.getInt("backgroundTimeout") : 300)
                // setLifecycleEvents
                .lifecycleEvents(
                        options.hasKey("setLifecycleEvents") ? options.getBoolean("setLifecycleEvents") : false)
                // setScreenContext
                .screenContext(options.hasKey("setScreenContext") ? options.getBoolean("setScreenContext") : false)
                // setGeoLocationContext
                .geoLocationContext(
                        options.hasKey("setGeoLocationContext") ? options.getBoolean("setGeoLocationContext") : false)
                // setInstallEvent
                .installTracking(options.hasKey("setInstallEvent") ? options.getBoolean("setInstallEvent") : false)
                // setExceptionEvents
                .applicationCrash(
                        options.hasKey("setExceptionEvents") ? options.getBoolean("setExceptionEvents") : false)
                .build());

    }

    @ReactMethod
    public void trackSelfDescribingEvent(ReadableMap event,
                                        ReadableArray contexts) {

        SelfDescribing trackerEvent = EventUtil.getSelfDescribingEvent(event, contexts);
        if (trackerEvent != null) {
            tracker.track(trackerEvent);
        }
    }

    @ReactMethod
    public void trackStructuredEvent(String category, String action, String label, String property, Float value,
            ReadableArray contexts) {
        Structured trackerEvent = EventUtil.getStructuredEvent(category, action, label, property, value, contexts);
        if (trackerEvent != null) {
            tracker.track(trackerEvent);
        }
    }

    @ReactMethod
    public void trackScreenViewEvent(String screenName, String screenId, String screenType, String previousScreenName,
            String previousScreenType, String previousScreenId, String transitionType, ReadableArray contexts) {
        if (screenId == null) {
            screenId = UUID.randomUUID().toString();
        }
        ScreenView trackerEvent = EventUtil.getScreenViewEvent(screenName, screenId, screenType, previousScreenName,
                previousScreenId, previousScreenType, transitionType, contexts);
        if (trackerEvent != null) {
            tracker.track(trackerEvent);
        }
    }

    @ReactMethod
    public void trackPageView(String pageUrl, String pageTitle, String referrer, ReadableArray contexts) {
        PageView trackerEvent = EventUtil.getPageViewEvent(pageUrl, pageTitle, referrer, contexts);
        if (trackerEvent != null) {
            tracker.track(trackerEvent);

        }
    }

    @ReactMethod
    public void setUserId(String userId) {
        tracker.instance().getSubject().setUserId(userId);
    }
    
    @ReactMethod	
    public void getSessionUserId(final Promise promise) {	
        try {	
            Map<String, Object> sessionData = tracker.instance().getSession().getSessionValues();	
            Object result = sessionData.get(Parameters.SESSION_USER_ID);	
            promise.resolve(result);	
        } catch (Exception e) {	
            promise.reject("Cannot obtain SESSION_USER_ID");	
        }	
    }	
    	
    @ReactMethod	
    public void getSessionId(final Promise promise) {	
        try {	
            Map<String, Object> sessionData = tracker.instance().getSession().getSessionValues();	
            Object result = sessionData.get(Parameters.SESSION_ID);	
            promise.resolve(result);	
        } catch (Exception e) {	
            promise.reject("Cannot obtain SESSION_ID");	
        }	
    }	
    	
    @ReactMethod	
    public void getSessionIndex(final Promise promise) {	
        try {	
            Map<String, Object> sessionData = tracker.instance().getSession().getSessionValues();	
            Object result = sessionData.get(Parameters.SESSION_INDEX);	
            promise.resolve(result);	
        } catch (Exception e) {	
            promise.reject("Cannot obtain SESSION_INDEX");	
        }
    }
}
