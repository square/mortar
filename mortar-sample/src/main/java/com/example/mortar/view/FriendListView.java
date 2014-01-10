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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.example.mortar.screen.FriendListScreen;
import com.example.mortar.model.User;
import java.util.List;
import javax.inject.Inject;
import mortar.Mortar;
import mortar.MortarScope;

public class FriendListView extends ListView implements FriendListScreen.View {
  @Inject FriendListScreen.Presenter presenter;

  public FriendListView(Context context, AttributeSet attrs) {
    super(context, attrs);
    Mortar.inject(context, this);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    presenter.takeView(this);
  }

  @Override
  public void showFriends(List<User> friends) {
    Adapter adapter = new Adapter(getContext(), friends);

    setAdapter(adapter);
    setOnItemClickListener(new OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        presenter.onFriendSelected(position);
      }
    });
  }

  private static class Adapter extends ArrayAdapter<User> {
    public Adapter(Context context, List<User> objects) {
      super(context, android.R.layout.simple_list_item_1, objects);
    }
  }
}
