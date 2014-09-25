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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import mortar.dagger1support.Dagger1;
import android.view.Menu;
import android.view.MenuItem;
import com.example.flow.pathview.HandlesBack;
import com.example.flow.pathview.HandlesUp;
import com.example.flow.util.FlowBundler;
import com.example.mortar.android.ActionBarOwner;
import com.example.mortar.core.MortarDemoActivityBlueprint;
import com.example.mortar.screen.FriendListScreen;
import flow.Flow;
import flow.HasParent;
import flow.Path;
import flow.PathContainerView;
import javax.inject.Inject;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;
import mortar.MortarScopeDevHelper;
import rx.functions.Action0;

import static android.content.Intent.ACTION_MAIN;
import static android.content.Intent.CATEGORY_LAUNCHER;
import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

public class MortarDemoActivity extends android.app.Activity
    implements ActionBarOwner.Activity, Flow.Dispatcher {
  private MortarActivityScope activityScope;
  private ActionBarOwner.MenuAction actionBarMenuAction;

  @Inject ActionBarOwner actionBarOwner;

  private PathContainerView container;
  private HandlesBack containerAsHandlesBack;
  private HandlesUp containerAsHandlesUp;
  private Flow flow;

  @Override public MortarScope getScope() {
    return activityScope;
  }

  @Override public void dispatch(Flow.Traversal traversal, Flow.TraversalCallback callback) {
    Path newScreen = traversal.destination.current();
    boolean hasUp = newScreen instanceof HasParent;
    String title = newScreen.getClass().getSimpleName();
    ActionBarOwner.MenuAction menu =
        hasUp ? null : new ActionBarOwner.MenuAction("Friends", new Action0() {
          @Override public void call() {
            flow.goTo(new FriendListScreen());
          }
        });
    actionBarOwner.setConfig(new ActionBarOwner.Config(false, hasUp, title, menu));

    container.dispatch(traversal, callback);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (isWrongInstance()) {
      finish();
      return;
    }

    flow = getFlowBundler().onCreate(savedInstanceState);

    MortarScope parentScope = Mortar.getScope(getApplication());
    activityScope = Mortar.requireActivityScope(parentScope, new MortarDemoActivityBlueprint(this));
    Dagger1.inject(this, this);

    activityScope.onCreate(savedInstanceState);

    actionBarOwner.takeView(this);

    setContentView(R.layout.root_layout);
    container = (PathContainerView) findViewById(R.id.container);
    containerAsHandlesBack = (HandlesBack) container;
    containerAsHandlesUp = (HandlesUp) container;
  }

  @Override protected void onResume() {
    super.onResume();
    flow.setDispatcher(this);
  }

  @Override protected void onPause() {
    flow.removeDispatcher(this);
    super.onPause();
  }

  @Override public Object getSystemService(String name) {
    if (Flow.isFlowSystemService(name)) return flow;
    if (Mortar.isScopeSystemService(name)) return activityScope;

    return super.getSystemService(name);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    getFlowBundler().onSaveInstanceState(outState);
    activityScope.onSaveInstanceState(outState);
  }

  /** Inform the view about back events. */
  @Override public void onBackPressed() {
    if (!containerAsHandlesBack.onBackPressed()) super.onBackPressed();
  }

  /** Inform the view about up events. */
  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) return containerAsHandlesUp.onUpPressed();
    return super.onOptionsItemSelected(item);
  }

  /** Configure the action bar menu as required by {@link ActionBarOwner.Activity}. */
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
    menu.add("Log Scope Hierarchy")
        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
          @Override public boolean onMenuItemClick(MenuItem item) {
            Log.d("DemoActivity", MortarScopeDevHelper.scopeHierarchyToString(activityScope));
            return true;
          }
        });
    return true;
  }

  @Override protected void onDestroy() {
    actionBarOwner.dropView(this);

    // activityScope may be null in case isWrongInstance() returned true in onCreate()
    if (isFinishing() && activityScope != null) {
      MortarScope parentScope = Mortar.getScope(getApplication());
      parentScope.destroyChild(activityScope);
      activityScope = null;
    }

    super.onDestroy();
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

  @Override public void setTitle(CharSequence title) {
    getActionBar().setTitle(title);
  }

  @Override public void setMenu(ActionBarOwner.MenuAction action) {
    if (action != actionBarMenuAction) {
      actionBarMenuAction = action;
      invalidateOptionsMenu();
    }
  }

  /**
   * Dev tools and the play store (and others?) launch with a different intent, and so
   * lead to a redundant instance of this activity being spawned. <a
   * href="http://stackoverflow.com/questions/17702202/find-out-whether-the-current-activity-will-be-task-root-eventually-after-pendin"
   * >Details</a>.
   */
  private boolean isWrongInstance() {
    if (!isTaskRoot()) {
      Intent intent = getIntent();
      boolean isMainAction = intent.getAction() != null && intent.getAction().equals(ACTION_MAIN);
      return intent.hasCategory(CATEGORY_LAUNCHER) && isMainAction;
    }
    return false;
  }

  private FlowBundler getFlowBundler() {
    return ((MortarDemoApplication) getApplication()).getFlowBundler();
  }
}
