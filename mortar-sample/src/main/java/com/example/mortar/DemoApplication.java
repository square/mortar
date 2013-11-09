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

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import com.example.mortar.model.Chats;
import com.example.mortar.model.QuoteService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.flow.Parcer;
import dagger.Module;
import dagger.Provides;
import java.util.concurrent.Executor;
import javax.inject.Singleton;
import mortar.HasMortarScope;
import mortar.Mortar;
import mortar.MortarScope;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import rx.Scheduler;
import rx.concurrency.ExecutorScheduler;
import rx.plugins.RxJavaErrorHandler;
import rx.plugins.RxJavaPlugins;

public class DemoApplication extends Application implements HasMortarScope {
  private MortarScope applicationScope;

  @Override public void onCreate() {
    super.onCreate();

    applicationScope =
        Mortar.createRootScope(BuildConfig.DEBUG, new ApplicationModule());

    // So that exceptions thrown in RxJava onError methods don't have their stack traces swallowed.
    RxJavaPlugins.getInstance().registerErrorHandler(new RxJavaErrorHandler() {
      @Override public void handleError(Throwable e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override public MortarScope getMortarScope() {
    return applicationScope;
  }

  @Module(includes = Chats.Module.class, library = true)
  static class ApplicationModule {
    @Provides @Singleton @MainThread Scheduler provideMainThread() {
      final Handler handler = new Handler(Looper.getMainLooper());
      return new ExecutorScheduler(new Executor() {
        @Override public void execute(Runnable command) {
          handler.post(command);
        }
      });
    }

    @Provides @Singleton Gson provideGson() {
      return new GsonBuilder().create();
    }

    @Provides @Singleton Parcer<Object> provideParcer(Gson gson) {
      return new GsonParcer<Object>(gson);
    }

    @Provides @Singleton QuoteService provideQuoteService() {
      RestAdapter restAdapter =
          new RestAdapter.Builder().setServer("http://www.iheartquotes.com/api/v1/")
              .setConverter(new GsonConverter(new Gson()))
              .build();
      return restAdapter.create(QuoteService.class);
    }
  }
}
