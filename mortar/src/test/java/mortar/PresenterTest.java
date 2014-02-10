/*
 * Copyright 2014 Square Inc.
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
package mortar;

import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

// Robolectric allows us to use Bundles.
@RunWith(RobolectricTestRunner.class) @Config(manifest = Config.NONE)
public class PresenterTest {

  class SomeView {
    public final MortarActivityScope scope;

    public SomeView(MortarActivityScope scope) {
      this.scope = scope;
    }
  }

  class ChildPresenter extends Presenter<SomeView> {
    final String payload;
    boolean loaded;

    ChildPresenter(String payload) {
      this.payload = payload;
    }

    @Override protected MortarScope extractScope(SomeView view) {
      return view.scope;
    }

    @Override protected void onSave(Bundle savedInstanceState) {
      savedInstanceState.putString("key", payload);
    }

    @Override protected void onLoad(Bundle savedInstanceState) {
      if (savedInstanceState != null) {
        assertThat(savedInstanceState.getString("key")).isEqualTo(payload);
        loaded = true;
      }
    }
  }

  class ParentPresenter extends Presenter<SomeView> {
    @Override protected MortarScope extractScope(SomeView view) {
      return view.scope;
    }

    // The child presenters are anonymous inner classes but of the same
    // type. This is like the case where a presenter manages two
    // popup presenters for two different dialogs.

    ChildPresenter childOne = new ChildPresenter("one") {
    };
    ChildPresenter childTwo = new ChildPresenter("two") {
    };

    @Override protected void onLoad(Bundle savedInstanceState) {
      childOne.takeView(getView());
      childTwo.takeView(getView());
    }
  }

  @Test public void childPresentersGetTheirOwnBundles() {
    MortarActivityScope scope = createScope();
    scope.onCreate(null);

    ParentPresenter presenter = new ParentPresenter();
    presenter.takeView(new SomeView(scope));

    Bundle bundle = new Bundle();
    scope.onSaveInstanceState(bundle);
    scope.onCreate(bundle);

    /**
     * Assertions in {@link ChildPresenter#onLoad(android.os.Bundle)} are the real test,
     * but let's check that the were run
     */

    assertThat(presenter.childOne.loaded).isTrue();
    assertThat(presenter.childTwo.loaded).isTrue();
  }

  @Test public void replacedViewDoesNotDropNewView() {
    MortarActivityScope oldScope = createScope();
    oldScope.onCreate(null);
    ParentPresenter presenter = new ParentPresenter();
    presenter.takeView(new SomeView(oldScope));

    MortarActivityScope newScope = createScope();
    newScope.onCreate(null);
    SomeView newView = new SomeView(newScope);
    presenter.takeView(newView);

    oldScope.destroy();
    assertThat(presenter.getView()).isEqualTo(newView);
  }

  private MortarActivityScope createScope() {
    return Mortar.requireActivityScope(Mortar.createRootScope(false), new Blueprint() {
      @Override public String getMortarScopeName() {
        return "name";
      }

      @Override public Object getDaggerModule() {
        return null;
      }
    });
  }
}
