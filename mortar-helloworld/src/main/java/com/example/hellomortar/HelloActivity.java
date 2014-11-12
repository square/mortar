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
import android.os.Bundle;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;
import mortar.dagger2support.Dagger2;

public class HelloActivity extends Activity {
  private MortarActivityScope activityScope;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    MortarScope parentScope = ((HelloApplication) getApplication()).getRootScope();
    activityScope = Mortar.requireActivityScope(parentScope, new Main());
    Dagger2.<Main.Component>get(this).inject(this);
    activityScope.onCreate(savedInstanceState);

    setContentView(R.layout.main_view);
  }

  @Override public Object getSystemService(String name) {
    if (Mortar.isScopeSystemService(name)) {
      return activityScope;
    }
    return super.getSystemService(name);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    activityScope.onSaveInstanceState(outState);
  }

  @Override protected void onDestroy() {
    super.onDestroy();

    if (isFinishing() && activityScope != null) {
      MortarScope parentScope = ((HelloApplication) getApplication()).getRootScope();
      parentScope.destroyChild(activityScope);
      activityScope = null;
    }
  }
}
