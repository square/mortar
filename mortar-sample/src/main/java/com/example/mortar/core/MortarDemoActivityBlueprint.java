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
package com.example.mortar.core;

import android.app.Activity;
import com.example.mortar.MortarDemoActivity;
import com.example.mortar.android.ActionBarOwner;
import com.example.mortar.screen.FriendListScreen;
import flow.Flow;
import flow.HasParent;
import flow.Path;
import javax.inject.Inject;
import javax.inject.Singleton;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarScope;
import mortar.dagger1support.Dagger1Blueprint;
import rx.functions.Action0;

public class MortarDemoActivityBlueprint extends Dagger1Blueprint {

  private final String scopeName;

  public MortarDemoActivityBlueprint(Activity activity) {
    scopeName = activity.getLocalClassName() + "-task-" + activity.getTaskId();
  }

  @Override public String getMortarScopeName() {
    return scopeName;
  }

  @Override public Object getDaggerModule() {
    return new Module();
  }

  @dagger.Module( //
      addsTo = ApplicationModule.class,
      includes = ActionBarOwner.ActionBarModule.class,
      injects = MortarDemoActivity.class,
      library = true //
  )
  public static class Module {
  }
}
