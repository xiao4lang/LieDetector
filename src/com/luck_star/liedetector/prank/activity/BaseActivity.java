package com.luck_star.liedetector.prank.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.analytics.Tracker;
import com.luck_star.liedetector.prank.MyApp;

public class BaseActivity extends Activity {
	protected Context context;
	protected Tracker tracker;// GA追踪

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayUseLogoEnabled(false);
		getActionBar().setDisplayShowHomeEnabled(false);

		context = this;
		MyApp myApp = (MyApp) getApplication();
		tracker = myApp.tracker;
	}

}
