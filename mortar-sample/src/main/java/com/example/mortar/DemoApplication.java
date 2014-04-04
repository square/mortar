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

import android.app.Application;
import com.example.mortar.core.ApplicationModule;
import dagger.ObjectGraph;
import mortar.Mortar;
import mortar.MortarScope;
import rx.plugins.RxJavaErrorHandler;
import rx.plugins.RxJavaPlugins;

public class DemoApplication extends Application {
  private MortarScope rootScope;

  @Override public void onCreate() {
    super.onCreate();

    // So that exceptions thrown in RxJava onError methods don't have their stack traces swallowed.
    RxJavaPlugins.getInstance().registerErrorHandler(new RxJavaErrorHandler() {
      @Override public void handleError(Throwable e) {
        throw new RuntimeException(e);
      }
    });

    rootScope =
        Mortar.createRootScope(BuildConfig.DEBUG, ObjectGraph.create(new ApplicationModule()));
  }

  @Override public Object getSystemService(String name) {
    if (Mortar.isScopeSystemService(name)) {
      return rootScope;
    }
    return super.getSystemService(name);
  }
}
