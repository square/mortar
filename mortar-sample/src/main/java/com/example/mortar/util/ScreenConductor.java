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
package com.example.mortar.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import com.example.mortar.R;
import flow.Flow;
import flow.Layouts;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarScope;

import static android.view.animation.AnimationUtils.loadAnimation;

/**
 * A conductor that can swap subviews within a container view.
 * <p/>
 *
 * @param <S> the type of the screens that serve as a {@link Blueprint} for subview. Must
 * be annotated with {@link flow.Layout}, suitable for use with {@link flow.Layouts#createView}.
 */
public class ScreenConductor<S extends Blueprint> implements CanShowScreen<S> {

  private final Context context;
  private final ViewGroup container;
  private MortarScope scopeToDestroy;
  private boolean myScopeDestroyed;

  /**
   * @param container the container used to host child views. Typically this is a {@link
   * android.widget.FrameLayout} under the action bar.
   */
  public ScreenConductor(Context context, ViewGroup container) {
    this.context = context;
    this.container = container;
  }

  public void showScreen(S screen, Flow.Direction direction) {
    MortarScope myScope = Mortar.getScope(context);
    MortarScope newChildScope = myScope.requireChild(screen);

    View oldChild = getChildView();
    View newChild;

    if (oldChild != null) {
      //ensure there are no old scopes that need to be destroyed
      MortarScope oldScope = Mortar.getScope(oldChild.getContext());
      if (oldScope.getName().equals(screen.getMortarScopeName())) {
        // If it's already showing, short circuit.
        return;
      }
      // Destroy a pending old scope if it exists, and track this old scope for destruction
      destroyOldScope();
      scopeToDestroy = oldScope;
    }

    // Create the new child.
    Context childContext = newChildScope.createContext(context);
    newChild = Layouts.createView(childContext, screen);

    setAnimation(direction, oldChild, newChild);

    // Out with the old, in with the new.
    if (oldChild != null) {
      container.removeView(oldChild);
    }
    container.addView(newChild);
  }

  protected void setAnimation(Flow.Direction direction, final View oldChild, View newChild) {
    if (oldChild == null) return;

    int out = direction == Flow.Direction.FORWARD ? R.anim.slide_out_left : R.anim.slide_out_right;
    int in = direction == Flow.Direction.FORWARD ? R.anim.slide_in_right : R.anim.slide_in_left;

    Animation oldOutAnimation = loadAnimation(context, out);

    oldOutAnimation.setAnimationListener(new Animation.AnimationListener() {
      @Override public void onAnimationStart(
          Animation animation) {
      }

      @Override public void onAnimationEnd(Animation animation) {
        //Destroy scope when the animation finishes to synchronize with view destruction
        destroyOldScope();
      }

      @Override public void onAnimationRepeat(Animation animation) {
      }
    });

    oldChild.setAnimation(oldOutAnimation);
    newChild.setAnimation(loadAnimation(context, in));
  }

  private View getChildView() {
    return container.getChildAt(0);
  }

  /**
   * Called from container's View#onDetachFromWindow() to inform this view a rotation may be
   * occuring
   */
  public void onContainerDetached() {
    destroyOldScope();
  }

  private void destroyOldScope() {
    if (scopeToDestroy == null || myScopeDestroyed) {
      return;
    }

    MortarScope myScope = Mortar.getScope(context);
    myScope.destroyChild(scopeToDestroy);
    scopeToDestroy = null;
  }

  public void onScopeDestroyed() {
    myScopeDestroyed = true;
  }
}
