/*
 * Copyright 2014 Square Inc.
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
package com.example.hellomortar;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;

public class HelloActivity extends Activity {
  private MortarActivityScope activityScope;
  private Context mortarContext;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    MortarScope parentScope = ((HelloApplication) getApplication()).getRootScope();
    activityScope = Mortar.requireActivityScope(parentScope, new Main());
    mortarContext = activityScope.createContext(this);
    Mortar.inject(mortarContext, this);
    activityScope.onCreate(savedInstanceState);

    ViewGroup content = (ViewGroup) findViewById(android.R.id.content);
    View currentView = content.getChildAt(0);
    if (currentView != null) {
      throw new AssertionError("oops");
    }

    setContentView(R.layout.main_view);
  }

  @Override public void setContentView(int layoutResID) {
    LayoutInflater inflater
        = (LayoutInflater) mortarContext.getSystemService(LAYOUT_INFLATER_SERVICE);
    inflater.inflate(layoutResID, (ViewGroup) findViewById(android.R.id.content));
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    activityScope.onSaveInstanceState(outState);
  }

  @Override protected void onDestroy() {
    super.onDestroy();

    if (isFinishing() && activityScope != null) {
      activityScope.destroy();
      activityScope = null;
    }
  }
}
