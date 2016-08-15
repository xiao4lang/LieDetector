package com.luck_star.liedetector.prank;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public class MyApp extends Application {
	public GoogleAnalytics analytics;
	public Tracker tracker;

	@Override
	public void onCreate() {
		// 初始化GA
		analytics = GoogleAnalytics.getInstance(this);
		analytics.setLocalDispatchPeriod(1800);

		tracker = analytics.newTracker(Consts.GA_TRACKINGID);
		tracker.enableExceptionReporting(true);
		tracker.enableAdvertisingIdCollection(true);
		tracker.enableAutoActivityTracking(true);
	}
	// test

}
