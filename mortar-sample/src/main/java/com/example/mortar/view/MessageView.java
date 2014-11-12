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

package com.example.mortar.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import mortar.dagger1support.Dagger1;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.example.mortar.R;
import com.example.mortar.screen.MessageScreen;
import javax.inject.Inject;

public class MessageView extends LinearLayout {
  @Inject MessageScreen.Presenter presenter;

  @InjectView(R.id.user) TextView userView;
  @InjectView(R.id.message) TextView messageView;

  public MessageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
    Dagger1.inject(context, this);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.inject(this);

    presenter.takeView(this);
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    presenter.dropView(this);
  }

  public void setUser(String user) {
    userView.setText(user);
  }

  public void setMessage(String message) {
    messageView.setText(message);
  }

  @OnClick(R.id.user) void userClicked() {
    presenter.onUserSelected();
  }
}
