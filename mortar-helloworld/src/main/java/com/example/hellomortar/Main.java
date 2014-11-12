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

import android.os.Bundle;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.inject.Inject;
import javax.inject.Singleton;
import mortar.Blueprint;
import mortar.dagger2support.Dagger2;
import mortar.ViewPresenter;

public class Main implements Blueprint {
  @Override public String getMortarScopeName() {
    return getClass().getName();
  }

  @Override public Object createSubgraph(Object parentGraph) {
    return Dagger2.buildComponent(Component.class);
  }

  @dagger.Component @Singleton interface Component {
    void inject(HelloActivity t);
    void inject(MainView t);
  }

  @Singleton
  static class Presenter extends ViewPresenter<MainView> {
    private final DateFormat format = new SimpleDateFormat();

    private int serial = -1;

    @Inject Presenter() {
    }

    @Override protected void onLoad(Bundle savedInstanceState) {
      super.onLoad(savedInstanceState);
      if (savedInstanceState != null && serial == -1) serial = savedInstanceState.getInt("serial");

      getView().show("Update #" + ++serial + " at " + format.format(new Date()));
    }

    @Override protected void onSave(Bundle outState) {
      super.onSave(outState);
      outState.putInt("serial", serial);
    }
  }
}
