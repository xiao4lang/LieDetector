package com.luck_star.liedetector.prank.activity;

import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.analytics.HitBuilders;
import com.luck_star.liedetector.prank.Consts;
import com.luck_star.liedetector.prank.R;

public class MainActivity extends BaseActivity implements OnTouchListener {
	private TextView txtResult;
	private ImageView imgBtn, imgLine;
	private ProgressBar pb;
	private int status;
	private int truthOrLie;
	private final int READY = 0;
	private final int SCANNING = 1;
	private final int ABORT = 2;
	private final int ANALYZING = 3;
	private final int COMPLETED = 4;
	private InterstitialAd mInterstitialAd;
	private MediaPlayer player;
	private Vibrator vibrator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 初始化界面
		initView();

		ready();

	}

	private void initView() {
		// AdMob
		AdView mAdView = (AdView) findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest.Builder().build();
		mAdView.loadAd(adRequest);
		mInterstitialAd = new InterstitialAd(this);
		mInterstitialAd.setAdUnitId(getString(R.string.ad_id));
		mInterstitialAd.setAdListener(new AdListener() {
			@Override
			public void onAdClosed() {
				requestNewAd();
			}
		});
		requestNewAd();
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		txtResult = (TextView) findViewById(R.id.txtResult);
		pb = (ProgressBar) findViewById(R.id.pb);
		pb.setMax(100);
		imgBtn = (ImageView) findViewById(R.id.imgBtn);
		imgLine = (ImageView) findViewById(R.id.imgLine);
		imgBtn.setOnTouchListener(this);
		// imgBtn.setOnClickListener(this);
	}

	private void requestNewAd() {
		AdRequest adRequest = new AdRequest.Builder().addTestDevice(
				"YOUR_DEVICE_HASH").build();

		mInterstitialAd.loadAd(adRequest);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	class AnalayzTimer extends AsyncTask<Void, Integer, Boolean> {

		@Override
		protected void onPreExecute() {
			// 显示进度条
			pb.setVisibility(View.VISIBLE);
			txtResult.setText(getString(R.string.analyzing));
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			for (int i = 1; i <= 100; i++) {
				publishProgress(i);
				try {
					if (i % 20 == 0) {
						Thread.sleep(new Random().nextInt(9) * 50);
					} else {
						Thread.sleep(30);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (0 == truthOrLie) {// 没有设置，默认返回随机结果
				if (Math.random() < 0.5)
					return false;
				return true;

			} else {
				if (1 == truthOrLie)
					return true;
				return false;
			}

		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			int value = (Integer) values[0];
			pb.setProgress(value);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			status = COMPLETED;
			pb.setVisibility(View.INVISIBLE);
			long[] pattern = { 0, 3000 }; // 停止 开启 停止 开启
			vibrator.vibrate(pattern, -1);
			if (result) {// 真话
				player = MediaPlayer.create(context, R.raw.sound_truth);
				player.start();
				imgBtn.setImageResource(R.drawable.btn_truth);
				txtResult.setText(getString(R.string.truth));
				txtResult.setTextColor(Color.GREEN);
			} else {// 假话
				player = MediaPlayer.create(context, R.raw.sound_lie);
				player.start();
				imgBtn.setImageResource(R.drawable.btn_lie);
				txtResult.setText(getString(R.string.lie));
				txtResult.setTextColor(Color.RED);
			}

		}

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN
				&& (status == READY || status == ABORT)) {// 按下，开始扫描
			status = SCANNING;
			player = MediaPlayer.create(context, R.raw.scanning);
			player.start();
			// 开始震动
//			long[] pattern = { 200, 400, 200, 400 }; // 停止 开启 停止 开启
//			vibrator.vibrate(pattern, 2);
			txtResult.setText(getString(R.string.scanning));
			imgBtn.setImageResource(R.drawable.btn_pressed);
			Animation scanAnim = AnimationUtils.loadAnimation(context,
					R.anim.scan);
			scanAnim.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					if (status != ABORT) {// 扫描完成，接下来进行分析
						status = ANALYZING;
						vibrator.cancel();
						new AnalayzTimer().execute();
					}
				}
			});
			imgLine.startAnimation(scanAnim);
			tracker.send(new HitBuilders.EventBuilder().setCategory("Click")
					.setAction("Touch").setLabel("Begin").build());
		} else if (event.getAction() == MotionEvent.ACTION_UP) {// 松开，扫描完成或中断
			v.performClick();
			if (status == COMPLETED) {// 重置状态为ready
				ready();
			} else if (status == SCANNING) {// 只有状态为Scanning的时候取消才设置为Aborted状态
				status = ABORT;
				player.stop();
				long[] pattern = { 0, 200 }; // 停止 开启 停止 开启
				vibrator.vibrate(pattern, -1);
//				vibrator.cancel();
				txtResult.setText(getString(R.string.aborted));
				imgBtn.setImageResource(R.drawable.btn_aborted);
				imgLine.clearAnimation();
			}
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_help) {// 帮助界面
			startActivity(new Intent(context, HelpActivity.class));
		} else if (item.getItemId() == R.id.menu_rating) {
			try {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				Uri uri = Uri.parse("market://details?id=" + getPackageName());
				intent.setData(uri);
				startActivity(intent);
				tracker.send(new HitBuilders.EventBuilder().setCategory("Click")
						.setAction("Rating").setLabel("RatingUs").build());
			} catch (Exception e) {

			}
		} else if (item.getItemId() == R.id.menu_share) {// 分享
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.share_app));
			intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app));
			intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text)
					+ Consts.DOWNLOAD_URL);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(Intent.createChooser(intent, getTitle()));
			tracker.send(new HitBuilders.EventBuilder().setCategory("Click")
					.setAction("Share").setLabel("ShareApp").build());
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
			audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
					AudioManager.ADJUST_LOWER, 0);
			truthOrLie = 2;
			tracker.send(new HitBuilders.EventBuilder().setCategory("Click")
					.setAction("Set").setLabel("Truth").build());
			return true;
		} else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
			audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
					AudioManager.ADJUST_RAISE, 0);
			truthOrLie = 1;
			tracker.send(new HitBuilders.EventBuilder().setCategory("Click")
					.setAction("Set").setLabel("Lie").build());
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	// 设置扫描器为ready状态
	private void ready() {
		if (mInterstitialAd.isLoaded()) {
			mInterstitialAd.show();
		}
		truthOrLie = 0;
		status = READY;
		pb.setVisibility(View.INVISIBLE);
		imgBtn.setImageResource(R.drawable.btn_normal);
		txtResult.setText("");
		txtResult.setTextColor(Color.WHITE);
	}

}
