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
package com.example.mortar.core;

import com.example.mortar.android.ActionBarOwner;
import com.example.mortar.model.Chats;
import com.example.mortar.model.QuoteService;
import com.example.mortar.screen.GsonParceler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dagger.Module;
import dagger.Provides;
import flow.StateParceler;
import javax.inject.Singleton;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Defines app-wide singletons.
 */
@Module(
    includes = { ActionBarOwner.ActionBarModule.class, Chats.Module.class },
    injects = MortarDemoActivity.class,
    library = true)
public class RootModule {
  @Provides @Singleton Gson provideGson() {
    return new GsonBuilder().create();
  }

  @Provides @Singleton StateParceler provideParcer(Gson gson) {
    return new GsonParceler(gson);
  }

  @Provides @Singleton QuoteService provideQuoteService() {
    RestAdapter restAdapter =
        new RestAdapter.Builder().setEndpoint("http://www.iheartquotes.com/api/v1/")
            .setConverter(new GsonConverter(new Gson()))
            .build();
    return restAdapter.create(QuoteService.class);
  }
}
