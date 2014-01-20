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
import com.example.mortar.model.Chat;
import com.example.mortar.screen.ChatListScreen;
import java.util.List;
import javax.inject.Inject;
import mortar.Mortar;

public class ChatListView extends ListView {
  @Inject ChatListScreen.Presenter presenter;

  public ChatListView(Context context, AttributeSet attrs) {
    super(context, attrs);
    Mortar.inject(context, this);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    presenter.takeView(this);
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    presenter.dropView(this);
  }

  public void showConversations(List<Chat> chats) {
    Adapter adapter = new Adapter(getContext(), chats);

    setAdapter(adapter);
    setOnItemClickListener(new OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        presenter.onConversationSelected(position);
      }
    });
  }

  private static class Adapter extends ArrayAdapter<Chat> {
    public Adapter(Context context, List<Chat> objects) {
      super(context, android.R.layout.simple_list_item_1, objects);
    }
  }
}
