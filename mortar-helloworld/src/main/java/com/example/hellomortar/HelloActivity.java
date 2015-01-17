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
import mortar.MortarScope;
import mortar.bundler.BundleServiceProvider;
import mortar.dagger2support.DaggerServiceProvider;

public class HelloActivity extends Activity {
  private MortarScope activityScope;
  private BundleServiceProvider bundleServiceProvider;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    MortarScope parentScope = ((HelloApplication) getApplication()).getRootScope();
    String scopeName = Main.class.getName();
    activityScope = parentScope.findChild(scopeName);
    if (activityScope == null) {
      bundleServiceProvider = new BundleServiceProvider();

      activityScope = parentScope.buildChild(scopeName)
          .withService(DaggerServiceProvider.forComponent(Main.Component.class))
          .withService(bundleServiceProvider)
          .build();
    } else {
      bundleServiceProvider = new BundleServiceProvider.Finder().get(activityScope) ;
    }
    DaggerServiceProvider.<Main.Component>getDaggerComponent(this).inject(this);
    bundleServiceProvider.onCreate(savedInstanceState);

    setContentView(R.layout.main_view);
  }

  @Override public Object getSystemService(String name) {
    if (activityScope != null) {
      Object serviceFromMortar = activityScope.getService(name);
      if (serviceFromMortar != null) return serviceFromMortar;
    }

    return super.getSystemService(name);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    bundleServiceProvider.onSaveInstanceState(outState);
  }

  @Override protected void onDestroy() {
    super.onDestroy();

    if (isFinishing() && activityScope != null) {
      activityScope.destroy();
    }
  }
}
