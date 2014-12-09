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
import com.example.mortar.core.Main;
import com.example.mortar.core.MainScope;
import com.example.mortar.model.Chats;
import com.example.mortar.model.User;
import com.example.mortar.view.FriendListView;
import dagger.Provides;
import flow.Flow;
import flow.HasParent;
import flow.Layout;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import mortar.Blueprint;
import mortar.ViewPresenter;

@Layout(R.layout.friend_list_view) //
public class FriendListScreen implements HasParent<ChatListScreen>, Blueprint {

  @Override public String getMortarScopeName() {
    return getClass().getName();
  }

  @Override public Object getDaggerModule() {
    return new Module();
  }

  @dagger.Module(injects = FriendListView.class, addsTo = Main.Module.class)
  public static class Module {
    @Provides List<User> provideFriends(Chats chats) {
      return chats.getFriends();
    }
  }

  @Singleton
  public static class Presenter extends ViewPresenter<FriendListView> {
    private final List<User> friends;
    private final Flow flow;

    @Inject Presenter(List<User> friends, @MainScope Flow flow) {
      this.friends = friends;
      this.flow = flow;
    }

    @Override public void onLoad(Bundle savedInstanceState) {
      super.onLoad(savedInstanceState);
      if (!hasView()) return;
      getView().showFriends(friends);
    }

    public void onFriendSelected(int position) {
      flow.goTo(new FriendScreen(position));
    }
  }

  @Override public ChatListScreen getParent() {
    return new ChatListScreen();
  }
}
