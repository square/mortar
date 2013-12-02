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
package com.example.mortar;

import android.os.Bundle;
import com.example.mortar.model.Chat;
import com.example.mortar.model.Chats;
import com.example.mortar.model.User;
import com.example.mortar.screen.ChatListScreen;
import com.example.mortar.screen.FriendListScreen;
import dagger.Module;
import dagger.Provides;
import flow.Backstack;
import flow.Flow;
import flow.HasParent;
import flow.Parcer;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import mortar.ViewPresenter;
import mortar.Blueprint;
import mortar.MortarScope;
import rx.util.functions.Action0;

public class Main implements Blueprint {
  public Object getDaggerModule() {
    return new DaggerModule();
  }

  @Override public String getMortarScopeName() {
    return getClass().getName();
  }

  @Module(injects = { MainActivity.class, Main.Presenter.class, ActionBarOwner.class },
      addsTo = DemoApplication.ApplicationModule.class,
      library = true)
  public static class DaggerModule {

    @Provides @Singleton ActionBarOwner provideActionBar() {
      return new ActionBarOwner();
    }

    @Provides @App Flow provideAppFlow(Presenter presenter) {
      return presenter.flow;
    }

    @Provides List<Chat> provideConversations(Chats chats) {
      return chats.getAll();
    }

    @Provides List<User> provideFriends(Chats chats) {
      return chats.getFriends();
    }
  }

  interface View extends ActionBarOwner.View {
    void finish();

    void displayScreen(Object screen, MortarScope screenScope, Flow.Direction direction);
  }

  @Singleton static class Presenter extends ViewPresenter<View>
      implements Flow.Listener {
    private static final String FLOW_KEY = "flow";
    private static final Blueprint NO_SCREEN = new Blueprint() {
      @Override public String getMortarScopeName() {
        return "no-screen";
      }

      @Override public Object getDaggerModule() {
        throw new UnsupportedOperationException();
      }
    };

    private final Parcer<Object> flowParcer;
    private final ActionBarOwner actionBarOwner;

    private Flow flow;
    private Blueprint currentScreen = NO_SCREEN;

    @Inject Presenter(Parcer<Object> flowParcer, ActionBarOwner actionBarOwner) {
      this.flowParcer = flowParcer;
      this.actionBarOwner = actionBarOwner;
    }

    @Override public void onLoad(Bundle savedInstanceState) {
      super.onLoad(savedInstanceState);

      if (flow == null) {
        Backstack backstack = savedInstanceState == null ? Backstack.single(new ChatListScreen())
            : Backstack.from(savedInstanceState.getParcelable(FLOW_KEY), flowParcer);

        flow = new Flow(backstack, this);
      }

      go(flow.getBackstack(), Flow.Direction.FORWARD);
    }

    @Override public void onSave(Bundle outState) {
      outState.putParcelable(FLOW_KEY, flow.getBackstack().getParcelable(flowParcer));
      // Clear the current screen to force us to show it at the next load.
      currentScreen = NO_SCREEN;
    }

    @Override public void onDestroy() {
    }

    @Override public void go(Backstack backstack, Flow.Direction direction) {
      View view = getView();
      if (view == null) return;

      Blueprint newScreen = (Blueprint) backstack.current().getScreen();
      if (newScreen.getMortarScopeName().equals(currentScreen.getMortarScopeName())) return;

      MortarScope parentScope = view.getMortarScope();
      if (currentScreen != NO_SCREEN) {
        parentScope.findChild(currentScreen.getMortarScopeName()).destroy();
        currentScreen = NO_SCREEN;
      }

      MortarScope screenScope = parentScope.requireChild(newScreen);
      currentScreen = newScreen;

      boolean hasUp = newScreen instanceof HasParent;
      String title = newScreen.getClass().getSimpleName();
      ActionBarOwner.MenuAction menu =
          hasUp ? null : new ActionBarOwner.MenuAction("Friends", new Action0() {
            @Override public void call() {
              onFriendsListPicked();
            }
          });
      actionBarOwner.setConfig(new ActionBarOwner.Config(hasUp, title, menu));

      view.displayScreen(newScreen, screenScope, direction);
    }

    public void onBackPressed() {
      if (!flow.goBack()) getView().finish();
    }

    public void onFriendsListPicked() {
      flow.goTo(new FriendListScreen());
    }

    public boolean onUpPressed() {
      return flow.goUp();
    }
  }
}
