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
import com.example.mortar.App;
import com.example.mortar.Main;
import com.example.mortar.model.User;
import com.example.mortar.view.FriendListView;
import com.squareup.flow.Flow;
import com.squareup.flow.HasParent;
import com.squareup.flow.Screen;
import dagger.Module;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import mortar.AbstractViewPresenter;
import mortar.BluePrint;
import mortar.HasMortarScope;

@Screen(FriendListView.class) //
public class FriendListScreen implements HasParent<ChatListScreen>, BluePrint {

  @Override public String getMortarScopeName() {
    return getClass().getName();
  }

  @Override public Object getDaggerModule() {
    return new DaggerModule();
  }

  @Module(injects = { FriendListView.class, FriendListScreen.Presenter.class },
      addsTo = Main.DaggerModule.class)
  public static class DaggerModule {
  }

  public interface View extends HasMortarScope {
    void showFriends(List<User> friends);
  }

  @Singleton
  public static class Presenter extends AbstractViewPresenter<View> {
    private final List<User> friends;
    private final Flow flow;

    @Inject Presenter(List<User> friends, @App Flow flow) {
      this.friends = friends;
      this.flow = flow;
    }

    @Override public void onLoad(Bundle savedInstanceState) {
      super.onLoad(savedInstanceState);
      View view = getView();
      if (view == null) return;

      view.showFriends(friends);
    }

    public void onFriendSelected(int position) {
      flow.goTo(new FriendScreen(position));
    }
  }

  @Override public ChatListScreen getParent() {
    return new ChatListScreen();
  }
}
