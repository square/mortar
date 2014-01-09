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
import android.widget.ArrayAdapter;
import com.example.mortar.android.ActionBarOwner;
import com.example.mortar.core.Main;
import com.example.mortar.core.MainScope;
import com.example.mortar.model.Chat;
import com.example.mortar.model.Chats;
import com.example.mortar.model.Message;
import com.example.mortar.view.ChatView;
import com.example.mortar.view.Confirmation;
import com.example.mortar.view.ConfirmerPopup;
import dagger.Provides;
import flow.Flow;
import flow.HasParent;
import flow.Screen;
import javax.inject.Inject;
import javax.inject.Singleton;
import mortar.Blueprint;
import mortar.ViewPresenter;
import mortar.HasContext;
import mortar.PopupPresenter;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

@Screen(ChatView.class) //
public class ChatScreen implements HasParent<ChatListScreen>, Blueprint {
  private final int conversationIndex;

  public ChatScreen(int conversationIndex) {
    this.conversationIndex = conversationIndex;
  }

  @Override public String getMortarScopeName() {
    return "ChatScreen{" + "conversationIndex=" + conversationIndex + '}';
  }

  @Override public Object getDaggerModule() {
    return new Module();
  }

  @Override public ChatListScreen getParent() {
    return new ChatListScreen();
  }

  @dagger.Module(injects = ChatView.class, addsTo = Main.Module.class)
  public class Module {
    @Provides Chat provideConversation(Chats chats) {
      return chats.getChat(conversationIndex);
    }
  }

  public interface View extends HasContext {
    ConfirmerPopup getConfirmerPopup();

    ArrayAdapter<Message> getItems();

    void toast(String message);
  }

  @Singleton
  public static class Presenter extends ViewPresenter<View> {
    private final Chat chat;
    private final Flow flow;
    private final ActionBarOwner actionBar;
    private final PopupPresenter<Confirmation, Boolean> confirmer;

    private Subscription running;

    @Inject
    public Presenter(Chat chat, @MainScope Flow flow, ActionBarOwner actionBar) {
      this.chat = chat;
      this.flow = flow;
      this.actionBar = actionBar;
      this.confirmer = new PopupPresenter<Confirmation, Boolean>() {
        @Override protected void onPopupResult(Boolean confirmed) {
          if (confirmed) Presenter.this.getView().toast("Haven't implemented that, friend.");
        }
      };
    }

    @Override public void onLoad(Bundle savedInstanceState) {
      super.onLoad(savedInstanceState);
      View v = getView();
      if (v == null) return;

      ActionBarOwner.Config actionBarConfig = actionBar.getConfig();

      actionBarConfig =
          actionBarConfig.withAction(new ActionBarOwner.MenuAction("End", new Action0() {
            @Override public void call() {
              confirmer.show(
                  new Confirmation("End Chat", "Do you really want to leave this chat?", "Yes",
                      "I guess not"));
            }
          }));

      actionBar.setConfig(actionBarConfig);

      ensureRunning();
    }

    @Override public void takeView(View view) {
      super.takeView(view);
      confirmer.takeView(view.getConfirmerPopup());
    }

    @Override public void onSave(Bundle outState) {
      super.onSave(outState);
      ensureStopped();
    }

    @Override public void onDestroy() {
      ensureStopped();
      super.onDestroy();
    }

    public void onConversationSelected(int position) {
      flow.goTo(new MessageScreen(chat.getId(), position));
    }

    private void ensureRunning() {
      if (running == null) {
        running = chat.getMessages().subscribe(new Action1<Message>() {
          @Override public void call(Message message) {
            View view = getView();
            if (view == null) return;
            view.getItems().addAll(message);
          }
        });
      }
    }

    private void ensureStopped() {
      if (running != null) {
        running.unsubscribe();
        running = null;
      }
    }
  }
}
