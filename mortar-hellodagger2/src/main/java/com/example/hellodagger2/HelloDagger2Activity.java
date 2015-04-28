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
package com.example.hellodagger2;

import android.app.Activity;
import android.os.Bundle;
import mortar.MortarScope;
import mortar.bundler.BundleServiceRunner;

import static mortar.MortarScope.buildChild;
import static mortar.MortarScope.findChild;
import static com.example.hellodagger2.DaggerService.createComponent;

public class HelloDagger2Activity extends Activity {
  @Override public Object getSystemService(String name) {
    MortarScope activityScope = findChild(getApplicationContext(), getScopeName());

    if (activityScope == null) {
      activityScope = buildChild(getApplicationContext()) //
          .withService(BundleServiceRunner.SERVICE_NAME, new BundleServiceRunner())
          .withService(DaggerService.SERVICE_NAME, createComponent(Main.Component.class))
          .build(getScopeName());
    }

    return activityScope.hasService(name) ? activityScope.getService(name)
        : super.getSystemService(name);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    BundleServiceRunner.getBundleServiceRunner(this).onCreate(savedInstanceState);
    setContentView(R.layout.main_view);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    BundleServiceRunner.getBundleServiceRunner(this).onSaveInstanceState(outState);
  }

  @Override protected void onDestroy() {
    if (isFinishing()) {
      MortarScope activityScope = findChild(getApplicationContext(), getScopeName());
      if (activityScope != null) activityScope.destroy();
    }

    super.onDestroy();
  }

  private String getScopeName() {
    return getClass().getName();
  }
}
