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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.example.mortar.view.ContainerView;
import flow.Flow;
import flow.Screens;
import javax.inject.Inject;
import mortar.Mortar;
import mortar.MortarScope;

public class MainView extends ContainerView implements Main.View {
  @Inject Main.Presenter presenter;

  public MainView(Context context, AttributeSet attrs) {
    super(context, attrs);
    Mortar.inject(context, this);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    presenter.takeView(this);
  }

  @Override
  public void displayScreen(Object screen, MortarScope screenScope, Flow.Direction direction) {
    View screenView = Screens.createView(screenScope.createContext(getContext()), screen);
    displayView(screenView, direction);
  }

  public boolean onBackPressed() {
    return presenter.onRetreatSelected();
  }

  public boolean onUpPressed() {
    return presenter.onUpPressed();
  }

  @Override public MortarScope getMortarScope() {
    return Mortar.getScope(getContext());
  }
}
