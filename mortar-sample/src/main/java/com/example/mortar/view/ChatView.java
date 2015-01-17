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
import android.widget.Toast;
import mortar.dagger1support.ObjectGraphService;
import com.example.mortar.model.Message;
import com.example.mortar.screen.ChatScreen;
import javax.inject.Inject;

public class ChatView extends ListView {
  @Inject ChatScreen.Presenter presenter;

  private final ConfirmerPopup confirmerPopup;

  public ChatView(Context context, AttributeSet attrs) {
    super(context, attrs);
    ObjectGraphService.inject(context, this);
    confirmerPopup = new ConfirmerPopup(context);

    setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    presenter.takeView(this);
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    presenter.dropView(this);
  }

  @Override protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    presenter.visibilityChanged(visibility == VISIBLE);
  }

  public ConfirmerPopup getConfirmerPopup() {
    return confirmerPopup;
  }

  public ArrayAdapter<Message> getItems() {
    @SuppressWarnings("unchecked") ArrayAdapter<Message> adapter =
        (ArrayAdapter<Message>) getAdapter();

    if (adapter == null) {
      adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
      setAdapter(adapter);
      adapter.setNotifyOnChange(true);
      setOnItemClickListener(new OnItemClickListener() {
        @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          presenter.onConversationSelected(position);
        }
      });
    }

    return adapter;
  }

  public void toast(String message) {
    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
  }
}
