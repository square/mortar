/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.mortar;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import com.example.mortar.android.ActionBarOwner;
import com.example.mortar.core.Main;
import com.example.mortar.core.MainView;
import dagger.ObjectGraph;
import javax.inject.Inject;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarContext;
import mortar.MortarScope;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

/**
 * Hooks up the {@link MortarActivityScope}. Loads the {@link com.example.mortar.core.MainView}
 * and lets it know about up button and back button presses. Shares control of the {@link
 * ActionBar} via the {@link com.example.mortar.android.ActionBarOwner}.
 */
// TODO: this is pretty confusing. Maybe move ActionBarOwner.View duties to an inner class.
public class DemoActivity extends Activity implements MortarContext, ActionBarOwner.View {
  private ActionBarOwner.MenuAction actionBarMenuAction;
  private MortarActivityScope rootScope;

  @Inject ActionBarOwner actionBarOwner;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ObjectGraph objectGraph = ObjectGraph.create(new Main.Module());
    rootScope = Mortar.createRootActivityScope(BuildConfig.DEBUG, objectGraph);
    rootScope.onCreate(savedInstanceState);

    Mortar.inject(this, this);

    setContentView(R.layout.activity_main);

    actionBarOwner.takeView(this);
  }

  @Override protected void onResume() {
    super.onResume();
    rootScope.onResume();
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    rootScope.onSaveInstanceState(outState);
  }

  @Override public void finish() {
    super.finish();
    rootScope.destroy();
    rootScope = null;
  }

  /** Inform the view about back events. */
  @Override public void onBackPressed() {
    MainView view = getMainView();
    if (!view.onBackPressed()) super.onBackPressed();
  }

  /** Inform the view about up events. */
  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      MainView view = getMainView();
      return view.onUpPressed();
    }

    return super.onOptionsItemSelected(item);
  }

  /** Configure the action bar menu as required by {@link #setMenu} */
  @Override public boolean onCreateOptionsMenu(Menu menu) {
    if (actionBarMenuAction != null) {
      menu.add(actionBarMenuAction.title)
          .setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS)
          .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem menuItem) {
              actionBarMenuAction.action.call();
              return true;
            }
          });
    }
    return true;
  }

  //
  // ActionBarOwner.View responsibilities
  //

  @Override public MortarScope getMortarScope() {
    return rootScope;
  }

  @Override public void setShowHomeEnabled(boolean enabled) {
    ActionBar actionBar = getActionBar();
    actionBar.setDisplayShowHomeEnabled(false);
  }

  @Override public void setUpButtonEnabled(boolean enabled) {
    ActionBar actionBar = getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(enabled);
    actionBar.setHomeButtonEnabled(enabled);
  }

  @Override public void setMenu(ActionBarOwner.MenuAction action) {
    if (action != actionBarMenuAction) {
      actionBarMenuAction = action;
      invalidateOptionsMenu();
    }
  }

  private MainView getMainView() {
    ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
    return (MainView) root.getChildAt(0);
  }
}
