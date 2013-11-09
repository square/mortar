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

import android.text.TextUtils;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit.RetrofitError;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

public class Chat {
  private static final int SLEEP_MILLIS = 500;
  private static final int PROBABILITY = 3;

  private final int id;
  private final List<User> users;
  private final List<Message> messages;

  private Chats chats;

  Chat(Chats chats, int id, List<User> users, List<Message> seed) {
    this.chats = chats;
    this.id = id;
    this.users = users;
    messages = new CopyOnWriteArrayList<Message>(seed);
  }

  public int getId() {
    return id;
  }

  public Observable<Message> getMessage(int index) {
    return Observable.from(messages.get(index));
  }

  public Observable<Message> getMessages() {
    Observable<Message> getMessages = Observable.create(new Observable.OnSubscribeFunc<Message>() {
      @Override public Subscription onSubscribe(final Observer<? super Message> observer) {
        final AtomicBoolean canceled = new AtomicBoolean(false);
        final Random random = new Random();

        chats.messagePollThread.execute(new Runnable() {
          @Override public void run() {
            while (!canceled.get()) {
              if (random.nextInt(PROBABILITY) == 0) {
                try {
                  User from = users.get(random.nextInt(users.size()));
                  Message next = new Message(from, chats.service.getQuote().quote);
                  messages.add(next);
                  observer.onNext(next);
                } catch (RetrofitError e) {
                  // Bad response? Lost connectivity? Who cares, it's a demo.
                }
              }

              try {
                // Hijacking the thread like this is sleazey, but you get the idea.
                Thread.sleep(SLEEP_MILLIS);
              } catch (InterruptedException e) {
                canceled.set(true);
              }
            }
            observer.onCompleted();
          }
        });

        return new Subscription() {
          @Override public void unsubscribe() {
            canceled.set(true);
          }
        };
      }
    });

    return getMessages.startWith(messages).observeOn(chats.mainThread);
  }

  @Override public String toString() {
    return TextUtils.join(", ", users.toArray(new User[users.size()]));
  }
}
