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
import com.example.mortar.core.Main;
import com.example.mortar.model.Chats;
import com.example.mortar.model.User;
import com.example.mortar.view.FriendView;
import dagger.Provides;
import flow.HasParent;
import flow.Screen;
import javax.inject.Inject;
import javax.inject.Singleton;
import mortar.Blueprint;
import mortar.ViewPresenter;
import mortar.HasContext;

@Screen(FriendView.class) //
public class FriendScreen implements HasParent<FriendListScreen>, Blueprint {
  private final int index;

  public FriendScreen(int index) {
    this.index = index;
  }

  @Override public String getMortarScopeName() {
    return "FriendScreen{" + "index=" + index + '}';
  }

  @Override public Object getDaggerModule() {
    return new Module();
  }

  @Override public FriendListScreen getParent() {
    return new FriendListScreen();
  }

  @dagger.Module(injects = FriendView.class, addsTo = Main.Module.class)
  public class Module {
    @Provides User provideFriend(Chats chats) {
      return chats.getFriend(index);
    }
  }

  public interface View extends HasContext {
    void setText(CharSequence text);
  }

  @Singleton
  public static class Presenter extends ViewPresenter<View> {
    private final User friend;

    @Inject Presenter(User friend) {
      this.friend = friend;
    }

    @Override public void onLoad(Bundle savedInstanceState) {
      super.onLoad(savedInstanceState);
      View view = getView();
      if (view == null) return;

      view.setText(friend.name);
    }
  }
}
