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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import javax.inject.Inject;
import mortar.dagger2support.Dagger2;

public class MainView extends LinearLayout {
  @Inject Main.Presenter presenter;

  private TextView textView;

  public MainView(Context context, AttributeSet attrs) {
    super(context, attrs);
    Dagger2.<Main.Component>get(context).inject(this);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    textView = (TextView) findViewById(R.id.text);
    presenter.takeView(this);
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    presenter.dropView(this);
  }

  public void show(CharSequence stuff) {
    textView.setText(stuff);
  }
}
