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
import android.view.ViewGroup;
import com.example.mortar.util.FlowOwner;
import com.example.mortar.util.FlowOwnerView;
import javax.inject.Inject;
import mortar.Blueprint;
import mortar.Mortar;

public class MainView extends FlowOwnerView<Blueprint> {
  @Inject Main.Presenter presenter;

  public MainView(Context context, AttributeSet attrs) {
    super(context, attrs);
    Mortar.inject(context, this);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    presenter.takeView(this);
  }

  @Override protected ViewGroup getContainer() {
    return this;
  }

  @Override protected FlowOwner<Blueprint, MainView> getPresenter() {
    return presenter;
  }
}
