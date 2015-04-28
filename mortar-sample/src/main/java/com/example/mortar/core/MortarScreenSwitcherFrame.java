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

import android.content.Context;
import android.util.AttributeSet;
import com.example.mortar.R;
import com.example.mortar.mortarflow.MortarContextFactory;
import com.example.mortar.screen.FramePathContainerView;
import com.example.mortar.screen.SimplePathContainer;
import flow.path.Path;

public class MortarScreenSwitcherFrame extends FramePathContainerView {
  public MortarScreenSwitcherFrame(Context context, AttributeSet attrs) {
    super(context, attrs, new SimplePathContainer(R.id.screen_switcher_tag,
        Path.contextFactory(new MortarContextFactory())));
  }
}
