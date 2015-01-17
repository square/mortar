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
import retrofit.RetrofitError;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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
    messages = new CopyOnWriteArrayList<>(seed);
  }

  public int getId() {
    return id;
  }

  public Observable<Message> getMessage(int index) {
    return Observable.just(messages.get(index));
  }

  public Observable<Message> getMessages() {
    return Observable.create(new Observable.OnSubscribe<Message>() {
      @Override public void call(final Subscriber<? super Message> subscriber) {
        final Random random = new Random();
        while (true) {
          if (random.nextInt(PROBABILITY) == 0) {
            try {
              User from = users.get(random.nextInt(users.size()));
              Message next = new Message(from, chats.service.getQuote().quote);
              messages.add(next);
              if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(next);
              }
            } catch (RetrofitError e) {
              if (!subscriber.isUnsubscribed()) {
                subscriber.onError(e);
                break;
              }
            }
          }

          try {
            // Hijacking the thread like this is sleazey, but you get the idea.
            Thread.sleep(SLEEP_MILLIS);
          } catch (InterruptedException e) {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onError(e);
            }
            break;
          }
        }
      }
    })
    .startWith(messages) //
    .subscribeOn(Schedulers.io()) //
    .observeOn(AndroidSchedulers.mainThread());
  }

  @Override public String toString() {
    return TextUtils.join(", ", users.toArray(new User[users.size()]));
  }
}
