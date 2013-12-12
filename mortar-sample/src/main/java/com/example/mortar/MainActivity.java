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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import butterknife.InjectView;
import butterknife.Views;
import com.example.mortar.view.ContainerView;
import flow.Flow;
import flow.Screens;
import javax.inject.Inject;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarScope;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

public class MainActivity extends DemoBaseActivity implements Main.View, ActionBarOwner.View {
  @Inject Main.Presenter presenter;
  @Inject ActionBarOwner actionBarOwner;

  @InjectView(R.id.container) ContainerView containerView;

  private ActionBarOwner.MenuAction actionBarMenuAction;

  @Override protected Blueprint getBluePrint() {
    return new Main();
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Mortar.inject(this, this);

    setContentView(R.layout.activity_main);
    Views.inject(this);

    final ActionBar actionBar = getActionBar();
    actionBar.setDisplayShowHomeEnabled(false);

    presenter.takeView(this);
    actionBarOwner.takeView(this);
  }

  @Override public void onBackPressed() {
    presenter.onBackPressed();
  }

  @Override
  public void displayScreen(Object screen, MortarScope screenScope, Flow.Direction direction) {
    View screenView = Screens.createView(screenScope.createContext(this), screen);
    containerView.displayView(screenView, direction);
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

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) return presenter.onUpPressed();

    return super.onOptionsItemSelected(item);
  }
}
