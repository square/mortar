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

import android.app.Activity;
import android.os.Bundle;
import mortar.Blueprint;
import mortar.HasMortarScope;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;

public abstract class DemoBaseActivity extends Activity implements HasMortarScope {
  private MortarActivityScope activityScope;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    activityScope = Mortar.getActivityScope(getParentScope(), getBlueprint());
    activityScope.onCreate(this, savedInstanceState);
  }

  /**
   * Return the {@link mortar.Blueprint} that defines the {@link MortarScope} for this activity.
   */
  protected abstract Blueprint getBlueprint();

  @Override protected void onResume() {
    super.onResume();
    activityScope.onResume(this);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    activityScope.onSaveInstanceState(outState);
  }

  @Override public void finish() {
    super.finish();
    activityScope.destroy();
    activityScope = null;
  }

  @Override public MortarScope getMortarScope() {
    return activityScope;
  }

  private MortarScope getParentScope() {
    return Mortar.getScope(getApplicationContext());
  }
}
