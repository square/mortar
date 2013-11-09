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

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.example.mortar.model.Message;
import com.example.mortar.screen.ChatScreen;
import javax.inject.Inject;
import mortar.Mortar;
import mortar.MortarScope;
import mortar.PopupPresenter;

public class ChatView extends ListView implements ChatScreen.View {
  private final ConfirmerPopup confirmerPopup;

  @Inject ChatScreen.Presenter presenter;
  @Inject PopupPresenter<Confirmation, Boolean> confirmerPresenter;

  public ChatView(Context context, AttributeSet attrs) {
    super(context, attrs);
    Mortar.inject(context, this);

    setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
    confirmerPopup = new ConfirmerPopup(context, confirmerPresenter);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    presenter.takeView(this);
    confirmerPresenter.takeView(confirmerPopup);
  }

  @Override public MortarScope getMortarScope() {
    return Mortar.getScope(getContext());
  }

  @Override public ArrayAdapter<Message> getItems() {
    @SuppressWarnings("unchecked") ArrayAdapter<Message> adapter =
        (ArrayAdapter<Message>) getAdapter();

    if (adapter == null) {
      adapter = new ArrayAdapter<Message>(getContext(), R.layout.simple_list_item_1);
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

  @Override public void toast(String message) {
    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
  }
}
