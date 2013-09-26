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

package us.nineworlds.serenity.ui.browser.tv.seasons;

import java.util.ArrayList;
import java.util.List;

import net.simonvt.menudrawer.MenuDrawer;
import us.nineworlds.serenity.R;
import us.nineworlds.serenity.SerenityApplication;
import us.nineworlds.serenity.core.model.MenuDrawerItem;
import us.nineworlds.serenity.core.model.impl.MenuDrawerItemImpl;
import us.nineworlds.serenity.ui.activity.SerenityVideoActivity;
import us.nineworlds.serenity.ui.adapters.MenuDrawerAdapter;
import us.nineworlds.serenity.ui.browser.tv.TVShowMenuDrawerOnItemClickedListener;
import us.nineworlds.serenity.ui.listeners.MenuDrawerOnClickListener;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.RelativeLayout;

/**
 * @author dcarver
 * 
 */
public class TVShowSeasonBrowserActivity extends SerenityVideoActivity {

	private Gallery tvShowSeasonsGallery;
	private View tvShowSeasonsMainView;
	private boolean restarted_state = false;
	private String key;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		key = getIntent().getExtras().getString("key");
		
		createSideMenu();

		tvShowSeasonsMainView = findViewById(R.id.tvshowSeasonBrowserLayout);
		tvShowSeasonsGallery = (Gallery) findViewById(R.id.tvShowSeasonImageGallery);
		if (SerenityApplication.isRunningOnOUYA()) {
			RelativeLayout mainLayout = (RelativeLayout) findViewById(R.id.tvshowSeasonBrowserLayout);
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)  mainLayout.getLayoutParams();
			params.setMargins(35, 20, 45, 20);
		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
		if (restarted_state == false) {
			setupSeasons();
		}
		restarted_state = false;
	}

	protected void setupSeasons() {

		tvShowSeasonsGallery.setAdapter(new TVShowSeasonImageGalleryAdapter(
				this, key));
		tvShowSeasonsGallery
				.setOnItemSelectedListener(new TVShowSeasonOnItemSelectedListener(
						tvShowSeasonsMainView, this));
		tvShowSeasonsGallery
				.setOnItemClickListener(new TVShowSeasonOnItemClickListener(
						this));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	protected void onRestart() {
		super.onRestart();
		restarted_state = true;
	}

	/* (non-Javadoc)
	 * @see us.nineworlds.serenity.ui.activity.SerenityActivity#createSideMenu()
	 */
	@Override
	protected void createSideMenu() {
		menuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.OVERLAY);
		menuDrawer.setMenuView(R.layout.menu_drawer);
		menuDrawer.setContentView(R.layout.activity_tvbrowser_show_seasons);
		menuDrawer.setDrawerIndicatorEnabled(true);
		
		List<MenuDrawerItem> drawerMenuItem = new ArrayList<MenuDrawerItem>();
		drawerMenuItem.add(new MenuDrawerItemImpl("Play All from Queue", R.drawable.menu_play_all_queue));

		menuOptions = (ListView)menuDrawer.getMenuView().findViewById(R.id.menu_list_options);
		menuOptions.setAdapter(new MenuDrawerAdapter(this, drawerMenuItem));
		menuOptions.setOnItemClickListener(new TVShowSeasonMenuDrawerOnItemClickedListener(menuDrawer));
		
		hideMenuItems();

		View menuButton = findViewById(R.id.menu_button);
		menuButton
				.setOnClickListener(new MenuDrawerOnClickListener(menuDrawer));
		
		
	}
	
	/* (non-Javadoc)
	 * @see us.nineworlds.serenity.ui.activity.SerenityActivity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean menuKeySlidingMenu = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("remote_control_menu",
				true);
		if (menuKeySlidingMenu) {
			if (keyCode == KeyEvent.KEYCODE_MENU) {
				showMenuItems();
				menuDrawer.toggleMenu();
				menuOptions.requestFocusFromTouch();
				return true;
			}
		}
		
		if (keyCode == KeyEvent.KEYCODE_BACK && menuDrawer.isMenuVisible()) {
			hideMenuItems();
			menuDrawer.toggleMenu();
			if (tvShowSeasonsGallery != null) {
				tvShowSeasonsGallery.requestFocusFromTouch();
			}
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}

}
