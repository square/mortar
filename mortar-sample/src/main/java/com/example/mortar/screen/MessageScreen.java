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
package com.example.mortar.screen;

import android.os.Bundle;
import com.example.mortar.R;
import com.example.mortar.core.RootModule;
import com.example.mortar.model.Chats;
import com.example.mortar.model.Message;
import com.example.mortar.mortarscreen.WithModule;
import com.example.mortar.view.MessageView;
import dagger.Provides;
import flow.Flow;
import flow.path.Path;
import javax.inject.Inject;
import javax.inject.Singleton;
import mortar.ViewPresenter;
import rx.Observable;
import rx.functions.Action1;

@Layout(R.layout.message_view) @WithModule(MessageScreen.Module.class)
public class MessageScreen extends Path {
  private final int chatId;
  private final int messageId;

  public MessageScreen(int chatId, int messageId) {
    this.chatId = chatId;
    this.messageId = messageId;
  }

  @dagger.Module(injects = MessageView.class, addsTo = RootModule.class)
  public class Module {
    @Provides Observable<Message> provideMessage(Chats chats) {
      return chats.getChat(chatId).getMessage(messageId);
    }
  }

  @Singleton
  public static class Presenter extends ViewPresenter<MessageView> {
    private final Observable<Message> messageSource;

    private Message message;

    @Inject Presenter(Observable<Message> messageSource) {
      this.messageSource = messageSource;
    }

    @Override public void onLoad(Bundle savedInstanceState) {
      super.onLoad(savedInstanceState);
      if (!hasView()) return;

      messageSource.subscribe(new Action1<Message>() {
        @Override public void call(Message message) {
          if (!hasView()) return;
          Presenter.this.message = message;
          MessageView view = getView();
          view.setUser(message.from.name);
          view.setMessage(message.body);
        }
      });
    }

    public void onUserSelected() {
      if (message == null) return;
      int position = message.from.id;
      if (position != -1) {
        Flow.get(getView()).set(new FriendScreen(position));
      }
    }
  }
}


