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
import com.example.mortar.ActionBarOwner;
import com.example.mortar.App;
import com.example.mortar.Main;
import com.example.mortar.model.Chat;
import com.example.mortar.model.Chats;
import com.example.mortar.model.Message;
import com.example.mortar.view.ChatView;
import com.example.mortar.view.Confirmation;
import com.example.mortar.view.ConfirmerPopup;
import dagger.Module;
import dagger.Provides;
import flow.Flow;
import flow.HasParent;
import flow.Screen;
import javax.inject.Inject;
import javax.inject.Singleton;
import mortar.AbstractViewPresenter;
import mortar.Blueprint;
import mortar.HasMortarScope;
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
    return new DaggerModule();
  }

  @Override public ChatListScreen getParent() {
    return new ChatListScreen();
  }

  @Module(injects = { ChatScreen.Presenter.class, ChatView.class, ConfirmerPopup.class },
      addsTo = Main.DaggerModule.class)
  public class DaggerModule {
    @Provides Chat provideConversation(Chats chats) {
      return chats.getChat(conversationIndex);
    }

    @Provides
    PopupPresenter<Confirmation, Boolean> provideConfirmer(Presenter presenter) {
      return presenter.confirmer;
    }
  }

  public interface View extends HasMortarScope {
    ArrayAdapter<Message> getItems();

    void toast(String message);
  }

  @Singleton
  public static class Presenter extends AbstractViewPresenter<View> {
    private final Chat chat;
    private final Flow flow;
    private final ActionBarOwner actionBar;
    private final PopupPresenter<Confirmation, Boolean> confirmer;

    private Subscription running;

    @Inject
    public Presenter(Chat chat, @App Flow flow, ActionBarOwner actionBar) {
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
              confirmer.show(new Confirmation("End Chat",
                  "Do you really want to leave this chat?", "Yes", "I guess not"));
            }
          }));

      actionBar.setConfig(actionBarConfig);

      ensureRunning();
    }

    @Override public void onSave(Bundle outState) {
      super.onSave(outState);
      ensureStopped();
      // TODO this should be done for us, and for every presenter
      dropView();
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
