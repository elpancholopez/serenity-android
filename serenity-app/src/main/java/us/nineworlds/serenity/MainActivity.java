/**
 * The MIT License (MIT)
 * Copyright (c) 2012 David Carver
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package us.nineworlds.serenity;

import java.util.ArrayList;
import java.util.List;

import us.nineworlds.serenity.core.ServerConfig;
import us.nineworlds.serenity.core.menus.MenuDrawerItem;
import us.nineworlds.serenity.core.menus.MenuDrawerItemImpl;
import us.nineworlds.serenity.core.model.Server;
import us.nineworlds.serenity.core.services.GDMService;
import us.nineworlds.serenity.ui.activity.SerenityActivity;
import us.nineworlds.serenity.ui.adapters.MenuDrawerAdapter;
import us.nineworlds.serenity.ui.util.DisplayUtils;
import us.nineworlds.serenity.widgets.DrawerLayout;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.Toast;

import com.castillo.dd.DSInterface;
import com.castillo.dd.Download;
import com.castillo.dd.DownloadService;
import com.castillo.dd.PendingDownload;
import com.google.analytics.tracking.android.EasyTracker;

public class MainActivity extends SerenityActivity {

	private class AutoConfigureHandlerRunnable implements Runnable {

		@Override
		public void run() {
			if (SerenityApplication.getPlexMediaServers().isEmpty()) {
				return;
			}
			Server server = SerenityApplication.getPlexMediaServers().values()
					.iterator().next();
			String ipAddress = preferences.getString("server", "");
			if (SerenityApplication.getPlexMediaServers().isEmpty()
					&& "".equals(ipAddress)) {
				Toast.makeText(
						MainActivity.this,
						"No servers discovered or configured. Use settings to configure the ip address manually.",
						Toast.LENGTH_LONG).show();
				return;
			}
			if ("".equals(ipAddress)) {
				Editor edit = preferences.edit();
				edit.putString("server", server.getIPAddress());
				edit.apply();
				Toast.makeText(
						mainContext,
						getResources().getText(
								R.string.auto_configuring_server_using_)
								+ server.getServerName(), Toast.LENGTH_LONG)
						.show();
				mainContext.recreate();
			}
		}
	}

	private static class DownloadHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if ((msg.what == SerenityApplication.PROGRESS)
					&& (!downloadsCancelled)) {
				List<PendingDownload> pendingDownloads = SerenityApplication
						.getPendingDownloads();
				for (int i = 0; i < pendingDownloads.size(); i++) {
					if (i == downloadIndex) {
						try {
							int status = dsInterface.getDownloadStatus(i);
							pendingDownloads.get(i).setStatus(status);
							if (status == Download.START) {
								dsInterface.downloadFile(i);
								notification(pendingDownloads.get(i)
										.getFilename() + " has started.",
										"Downloading "
												+ pendingDownloads.get(i)
														.getFilename());
								pendingDownloads.get(i).setLaunchTime(
										dsInterface.getDownloadLaunchTime(i));

							} else if (status == Download.COMPLETE) {
								Toast.makeText(
										mainContext,
										pendingDownloads.get(i).getFilename()
												+ " has completed.",
										Toast.LENGTH_LONG).show();

								downloadIndex++;
								if (downloadIndex >= pendingDownloads.size()
										|| pendingDownloads.size() == 0) {
									notificationManager.cancel(1);
								}
							}
							if (status != Download.COMPLETE) {
								pendingDownloads.get(i).setProgress(
										dsInterface.getDownloadProgress(i));
								pendingDownloads.get(i).setEllapsedTime(
										dsInterface.getDownloadEllapsedTime(i));
								pendingDownloads
										.get(i)
										.setRemainingTime(
												dsInterface
														.getDownloadRemainingTime(i));
								pendingDownloads.get(i).setSpeed(
										dsInterface.getDownloadSpeed(i));
							} else {
								pendingDownloads.get(i).setProgress(100);
							}
						} catch (Exception e) {
							Log.e(getClass().getName(),
									Log.getStackTraceString(e));
						}
					}
				}
				sendMessageDelayed(obtainMessage(SerenityApplication.PROGRESS),
						50);
			}
		}

		protected void notification(String tickerText, String expandedText) {
			int icon = R.drawable.serenity_bonsai_logo;
			long when = System.currentTimeMillis();
			Notification notification = new Notification(icon, tickerText, when);
			String expandedTitle = "Serenity Download";
			Intent intent = new Intent(mainContext, MainActivity.class);
			PendingIntent launchIntent = PendingIntent.getActivity(mainContext,
					0, intent, 0);
			notification.setLatestEventInfo(mainContext, expandedTitle,
					expandedText, launchIntent);
			int notificationRef = 1;
			notificationManager.notify(notificationRef, notification);
		}
	}

	private static int downloadIndex;
	private static boolean downloadsCancelled = false;
	private static DSInterface dsInterface;

	public static int MAIN_MENU_PREFERENCE_RESULT_CODE = 100;
	private static Activity mainContext;
	private static NotificationManager notificationManager;

	public static DSInterface getDsInterface() {
		return dsInterface;
	}

	protected Handler autoConfigureHandler = new Handler();

	private final ServiceConnection downloadService = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			dsInterface = DSInterface.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			dsInterface = null;
		}
	};

	private final BroadcastReceiver gdmReciver = new GDMReceiver();

	private Gallery mainGallery;

	private View mainGalleryBackgroundView;

	protected Handler mHandler = new DownloadHandler();

	private SharedPreferences preferences;

	private boolean restarted_state = false;

	/**
	 * 
	 */
	@Override
	protected void createSideMenu() {
		mainContext = this;
		mainGalleryBackgroundView = findViewById(R.id.mainGalleryBackground);
		mainGallery = (Gallery) findViewById(R.id.mainGalleryMenu);
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
				R.drawable.menudrawer_selector, R.string.drawer_open,
				R.string.drawer_closed) {
			@Override
			public void onDrawerOpened(View drawerView) {

				super.onDrawerOpened(drawerView);
				getSupportActionBar().setTitle(R.string.app_name);
				drawerList.requestFocusFromTouch();

			}

			@Override
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);
				getSupportActionBar().setTitle(R.string.app_name);
				mainGallery.requestFocusFromTouch();
			}
		};

		drawerLayout.setDrawerListener(drawerToggle);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);

		drawerList = (ListView) findViewById(R.id.left_drawer);
		populateMenuOptions();
		hideMenuItems();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 
	 */
	protected void populateMenuOptions() {
		List<MenuDrawerItem> drawerMenuItem = new ArrayList<MenuDrawerItem>();
		drawerMenuItem
				.add(new MenuDrawerItemImpl(getResources().getString(
						R.string.options_main_about),
						R.drawable.ic_action_action_about));
		drawerMenuItem.add(new MenuDrawerItemImpl(getResources().getString(
				R.string.options_main_clear_image_cache),
				R.drawable.ic_action_content_remove));
		drawerMenuItem.add(new MenuDrawerItemImpl(getResources().getString(
				R.string.clear_queue), R.drawable.ic_action_content_remove));

		drawerList.setAdapter(new MenuDrawerAdapter(this, drawerMenuItem));
		drawerList
				.setOnItemClickListener(new MainMenuDrawerOnItemClickedListener(
						drawerLayout, mainGallery));
	}

	/**
	 * 
	 */
	protected void discoverPlexServers() {
		Intent GDMService = new Intent(this, GDMService.class);
		startService(GDMService);
	}

	/**
	 * 
	 */
	protected void initDownloadService() {
		getApplicationContext().bindService(
				new Intent(this, DownloadService.class), downloadService,
				Context.BIND_AUTO_CREATE);

		mHandler.sendMessage(mHandler
				.obtainMessage(SerenityApplication.PROGRESS));

		String svcName = Context.NOTIFICATION_SERVICE;
		notificationManager = (NotificationManager) getSystemService(svcName);
	}

	/**
	 * 
	 */
	protected void initPreferences() {
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences != null) {
			ServerConfig config = (ServerConfig) ServerConfig.getInstance();
			if (config != null) {
				preferences
						.registerOnSharedPreferenceChangeListener(((ServerConfig) ServerConfig
								.getInstance()).getServerConfigChangeListener());
			}
		}
	}

	/**
	 * Refresh the screen after coming back from the preferences screen.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == MAIN_MENU_PREFERENCE_RESULT_CODE) {
			recreate();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		actionBar = getSupportActionBar();
		actionBar.setCustomView(R.layout.clock_layout);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayUseLogoEnabled(false);
		actionBar.setBackgroundDrawable(new ColorDrawable(
				R.color.fity_percent_transparent));

		setContentView(R.layout.activity_plex_app_main);

		createSideMenu();
		initPreferences();

		initializeDefaultPlayer();
		if (preferences != null) {
			boolean watchedStatusFirstTime = preferences.getBoolean(
					"watched_status_firsttime", true);
			if (watchedStatusFirstTime) {
				SerenityApplication.getImageLoader().clearDiscCache();
				SerenityApplication.getImageLoader().clearMemoryCache();
				Editor editor = preferences.edit();
				editor.putBoolean("watched_status_firsttime", false);
				editor.apply();
			}
		}

		initDownloadService();

		DisplayUtils.overscanCompensation(this, findViewById(R.id.mainLayout),
				findViewById(R.id.menu_drawer_layout));

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * 
	 */
	protected void initializeDefaultPlayer() {
		boolean initialRun = preferences.getBoolean("serenity_first_run", true);
		if (initialRun) {
			SharedPreferences.Editor editor = preferences.edit();
			if (!SerenityApplication.isGoogleTV(this)) {
				editor.putBoolean("external_player", true);
			}
			editor.putBoolean("serenity_first_run", false);
			editor.commit();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mHandler.removeMessages(SerenityApplication.PROGRESS);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(gdmReciver);

		getApplicationContext().unbindService(downloadService);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see us.nineworlds.serenity.ui.activity.SerenityActivity#onKeyDown(int,
	 * android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean menuKeySlidingMenu = preferences.getBoolean(
				"remote_control_menu", true);

		if (menuKeySlidingMenu) {
			if (keyCode == KeyEvent.KEYCODE_MENU) {
				if (drawerLayout.isDrawerOpen(drawerList)) {
					drawerLayout.closeDrawers();
					mainGallery.requestFocusFromTouch();
				} else {
					drawerLayout.openDrawer(drawerList);
					drawerList.requestFocusFromTouch();
				}
				return true;
			}
		}

		if (drawerLayout.isDrawerOpen(drawerList)
				&& keyCode == KeyEvent.KEYCODE_BACK) {
			drawerLayout.closeDrawer(drawerList);
			mainGallery.requestFocusFromTouch();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onRestart() {
		restarted_state = true;
		populateMenuOptions();
		super.onRestart();

	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filters = new IntentFilter();
		filters.addAction(GDMService.MSG_RECEIVED);
		filters.addAction(GDMService.SOCKET_CLOSED);
		LocalBroadcastManager.getInstance(this).registerReceiver(gdmReciver,
				filters);

		// Start the auto-configuration service
		discoverPlexServers();
		mainGallery.requestFocusFromTouch();

	}

	@Override
	protected void onStart() {
		super.onStart();
		if (SerenityApplication.isTrackingEnabled()) {
			EasyTracker.getInstance().activityStart(this);
		}

		autoConfigureHandler.postDelayed(new AutoConfigureHandlerRunnable(),
				2500);
		if (restarted_state == false) {
			setupGallery();
		}
		restarted_state = false;
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (SerenityApplication.isTrackingEnabled()) {
			EasyTracker.getInstance().activityStop(this);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#openOptionsMenu()
	 */
	@Override
	public void openOptionsMenu() {
		menuOptions.setVisibility(View.VISIBLE);
		menuOptions.requestFocusFromTouch();
		menuDrawer.toggleMenu();
	}

	private void setupGallery() {
		mainGallery.setAdapter(new MainMenuTextViewAdapter(this,
				mainGalleryBackgroundView));
		mainGallery
				.setOnItemSelectedListener(new GalleryOnItemSelectedListener(
						mainGalleryBackgroundView));
		mainGallery
				.setOnItemClickListener(new GalleryOnItemClickListener(this));
		mainGallery.setCallbackDuringFling(false);
		mainGallery.requestFocusFromTouch();
	}

}
