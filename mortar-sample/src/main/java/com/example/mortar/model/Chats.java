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
package com.example.mortar.model;

import dagger.Provides;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;

import static java.util.Arrays.asList;

@Singleton
public class Chats {
  private final List<Chat> all;
  private final List<User> friends;

  final User me = new User(-1, "Me");

  final Executor messagePollThread;
  final QuoteService service;

  @Inject
  Chats(Executor messagePollThread, QuoteService service) {
    this.messagePollThread = messagePollThread;
    this.service = service;

    User alex = new User(0, "Alex");
    User chris = new User(1, "Chris");

    friends = asList(alex, chris);

    all = Collections.unmodifiableList(asList(//
        new Chat(this, 0, asList(alex, chris), //
            asList(new Message(me, "What's up?"), //
                new Message(alex, "Not much."), //
                new Message(chris, "Wanna hang out?"), //
                new Message(me, "Sure."), //
                new Message(alex, "Let's do it.") //
            )), //
        new Chat(this, 1, asList(chris), //
            asList(new Message(me, "You there bro?") //
            ))) //
    );
  }

  public List<User> getFriends() {
    return friends;
  }

  public User getFriend(int id) {
    return friends.get(id);
  }

  public List<Chat> getAll() {
    return all;
  }

  public Chat getChat(int id) {
    return all.get(id);
  }

  @dagger.Module(injects = Chats.class, library = true, complete = false)
  public static class Module {

    @Provides @Singleton Executor provideMessagePollThread() {
      return Executors.newSingleThreadExecutor();
    }
  }
}
