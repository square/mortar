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
package mortar;

import android.os.Bundle;
import dagger.Module;
import dagger.ObjectGraph;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static mortar.Mortar.createRootScope;
import static mortar.Mortar.requireActivityScope;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

// Robolectric allows us to use Bundles.
@RunWith(RobolectricTestRunner.class) @Config(manifest = Config.NONE)
public class MortarActivityScopeTest {

  private static class MyBundler implements Bundler {
    final String name;

    boolean loaded;
    Bundle lastLoaded;
    Bundle lastSaved;
    boolean destroyed;

    public MyBundler(String name) {
      this.name = name;
    }

    void reset() {
      lastSaved = lastLoaded = null;
      loaded = destroyed = false;
    }

    @Override public String getMortarBundleKey() {
      return name;
    }

    @Override public void onLoad(Bundle savedInstanceState) {
      loaded = true;
      lastLoaded = savedInstanceState;
      if (savedInstanceState != null) {
        assertThat(savedInstanceState.get("key")).isEqualTo(name);
      }
    }

    @Override public void onSave(Bundle outState) {
      lastSaved = outState;
      outState.putString("key", name);
    }

    @Override public void onDestroy() {
      destroyed = true;
    }
  }

  @Module class ModuleAndBlueprint implements Blueprint {
    private final String name;

    ModuleAndBlueprint(String name) {
      this.name = name;
    }

    @Override public String getMortarScopeName() {
      return name;
    }

    @Override public Object getDaggerModule() {
      return this;
    }
  }

  @Mock Scoped scoped;

  private MortarActivityScope activityScope;

  @Before
  public void setUp() {
    initMocks(this);
    resetScope();
  }

  private void resetScope() {
    MortarScope root = createRootScope(false, ObjectGraph.create(new ModuleAndBlueprint("root")));
    activityScope = requireActivityScope(root, new ModuleAndBlueprint("activity"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonNullKeyRequired() {
    activityScope.register(mock(Bundler.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonEmptyKeyRequired() {
    Bundler mock = mock(Bundler.class);
    when(mock.getMortarBundleKey()).thenReturn("");
    activityScope.register(mock);
  }

  @Test
  public void lifeCycle() {
    doLifecycleTest(activityScope);
  }

  @Test
  public void childLifeCycle() {
    doLifecycleTest(activityScope.requireChild(new ModuleAndBlueprint("child")));
  }

  private void doLifecycleTest(MortarScope registerScope) {
    MyBundler able = new MyBundler("able");
    MyBundler baker = new MyBundler("baker");

    registerScope.register(scoped);
    registerScope.register(able);
    registerScope.register(baker);

    // Load is called immediately.
    assertThat(able.loaded).isTrue();
    assertThat(able.lastLoaded).isNull();
    able.reset();
    assertThat(baker.loaded).isTrue();
    assertThat(baker.lastLoaded).isNull();
    baker.reset();

    activityScope.onCreate(null);
    // Create loads all registrants.
    assertThat(able.loaded).isTrue();
    assertThat(able.lastLoaded).isNull();
    able.reset();
    assertThat(baker.loaded).isTrue();
    assertThat(baker.lastLoaded).isNull();
    baker.reset();

    // When we save, the bundler gets its own bundle to write to.
    Bundle saved = new Bundle();
    activityScope.onSaveInstanceState(saved);
    assertThat(able.lastSaved).isNotNull();
    assertThat(baker.lastSaved).isNotNull();
    assertThat(able.lastSaved).isNotSameAs(baker.lastSaved);

    // If the bundler is re-registered, it loads again.
    able.lastLoaded = null;
    registerScope.register(able);
    assertThat(able.lastLoaded).isSameAs(able.lastSaved);

    // A new activity instance appears
    able.reset();
    baker.reset();
    activityScope.onSaveInstanceState(saved);
    Bundle fromNewActivity = new Bundle(saved);

    activityScope.onCreate(fromNewActivity);
    assertThat(able.lastLoaded).isNotNull();

    verifyNoMoreInteractions(scoped);

    activityScope.destroy();
    assertThat(able.destroyed).isTrue();
    verify(scoped).onDestroy();
  }

  class FauxActivity {
    final MyBundler rootPresenter = new MyBundler("core");

    MortarScope childScope;
    MyBundler childPresenter = new MyBundler("child");

    void create(Bundle bundle) {
      activityScope.onCreate(bundle);
      activityScope.register(rootPresenter);
      childScope = activityScope.requireChild(new ModuleAndBlueprint("child"));
      childScope.register(childPresenter);
    }
  }

  @Test public void childInfoSurvivesProcessDeath() {
    FauxActivity activity = new FauxActivity();
    activity.create(null);
    Bundle bundle = new Bundle();
    activityScope.onSaveInstanceState(bundle);

    // Process death: new copy of the bundle, new scope and activity instances
    bundle = new Bundle(bundle);
    resetScope();
    activity = new FauxActivity();
    activity.create(bundle);
    assertThat(activity.rootPresenter.lastLoaded).isNotNull();
    assertThat(activity.childPresenter.lastLoaded).isNotNull();
  }

  @Test public void handlesRegisterFromOnLoadBeforeCreate() {
    final MyBundler bundler = new MyBundler("inner");

    activityScope.register(new MyBundler("outer") {
      @Override public void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        activityScope.register(bundler);
      }
    });

    // The recursive register call loaded immediately.
    assertThat(bundler.loaded).isTrue();

    // And it was registered: a create call reloads it.
    bundler.reset();
    activityScope.onCreate(null);

    assertThat(bundler.loaded).isTrue();
  }

  @Test public void handlesRegisterFromOnLoadAfterCreate() {
    final MyBundler bundler = new MyBundler("inner");

    activityScope.onCreate(null);

    activityScope.register(new MyBundler("outer") {
      @Override public void onLoad(Bundle savedInstanceState) {
        activityScope.register(bundler);
      }
    });

    // The recursive register call loaded immediately.
    assertThat(bundler.loaded).isTrue();

    // And it was registered: the next create call reloads it.
    bundler.reset();
    Bundle b = new Bundle();
    activityScope.onSaveInstanceState(b);
    activityScope.onCreate(b);

    assertThat(bundler.loaded).isNotNull();
  }

  @Test public void handlesRegisterFromOnSave() {
    final MyBundler bundler = new MyBundler("inner");

    activityScope.onCreate(null);

    activityScope.register(new MyBundler("outer") {
      @Override public void onSave(Bundle outState) {
        super.onSave(outState);
        activityScope.register(bundler);
      }
    });
    assertThat(bundler.loaded).isFalse();

    Bundle bundle = new Bundle();
    activityScope.onSaveInstanceState(bundle);
    // Nothing should happen until create
    assertThat(bundler.loaded).isFalse();

    activityScope.onCreate(bundle);
    assertThat(bundler.loaded).isNotNull();
  }

  @Test public void handlesReregistrationBeforeCreate() {
    final AtomicInteger i = new AtomicInteger(0);

    activityScope.register(new Bundler() {
      @Override public String getMortarBundleKey() {
        return "key";
      }

      @Override public void onLoad(Bundle savedInstanceState) {
        if (i.incrementAndGet() < 1) activityScope.register(this);
      }

      @Override public void onSave(Bundle outState) {
        throw new UnsupportedOperationException();
      }

      @Override public void onDestroy() {
        throw new UnsupportedOperationException();
      }
    });

    Bundle b = new Bundle();
    activityScope.onCreate(b);

    assertThat(i.get()).isEqualTo(2);
  }

  @Test public void handlesReregistrationAfterCreate() {
    Bundle b = new Bundle();
    activityScope.onCreate(b);

    final AtomicInteger i = new AtomicInteger(0);

    activityScope.register(new Bundler() {
      @Override public String getMortarBundleKey() {
        return "key";
      }

      @Override public void onLoad(Bundle savedInstanceState) {
        if (i.incrementAndGet() < 1) activityScope.register(this);
      }

      @Override public void onSave(Bundle outState) {
        throw new UnsupportedOperationException();
      }

      @Override public void onDestroy() {
        throw new UnsupportedOperationException();
      }
    });

    assertThat(i.get()).isEqualTo(1);
  }

  @Test public void handlesReregistrationFromOnSave() {
    Bundle b = new Bundle();
    activityScope.onCreate(b);

    final AtomicInteger loads = new AtomicInteger(0);
    final AtomicInteger saves = new AtomicInteger(0);

    activityScope.register(new Bundler() {
      @Override public String getMortarBundleKey() {
        return "key";
      }

      @Override public void onLoad(Bundle savedInstanceState) {
        assertThat(loads.incrementAndGet()).isLessThan(3);
      }

      @Override public void onSave(Bundle outState) {
        assertThat(saves.incrementAndGet()).isEqualTo(1);
        activityScope.register(this);
      }

      @Override public void onDestroy() {
        throw new UnsupportedOperationException();
      }
    });
    assertThat(loads.get()).isEqualTo(1);

    Bundle newBundle = new Bundle();
    activityScope.onSaveInstanceState(newBundle);

    // No load should happen until next create
    assertThat(saves.get()).isEqualTo(1);
    assertThat(loads.get()).isEqualTo(1);

    activityScope.onCreate(newBundle);
    assertThat(loads.get()).isEqualTo(2);
  }

  @Test
  public void handleDestroyFromEarlyLoad() {
    final AtomicInteger loads = new AtomicInteger(0);
    final AtomicInteger destroys = new AtomicInteger(0);

    class Destroyer implements Bundler {
      @Override public String getMortarBundleKey() {
        return "k";
      }

      @Override public void onLoad(Bundle savedInstanceState) {
        if (loads.incrementAndGet() > 2) activityScope.destroy();
      }

      @Override public void onSave(Bundle outState) {
        throw new UnsupportedOperationException();
      }

      @Override public void onDestroy() {
        destroys.incrementAndGet();
      }
    }

    activityScope.register(new Destroyer());
    activityScope.register(new Destroyer());

    Bundle b = new Bundle();
    activityScope.onCreate(b);

    assertThat(loads.get()).isEqualTo(3);
    assertThat(destroys.get()).isEqualTo(2);
  }

  @Test
  public void handlesDestroyFromOnSave() {
    final AtomicInteger saves = new AtomicInteger(0);
    final AtomicInteger destroys = new AtomicInteger(0);

    class Destroyer implements Bundler {
      @Override public String getMortarBundleKey() {
        return "k";
      }

      @Override public void onLoad(Bundle savedInstanceState) {
      }

      @Override public void onSave(Bundle outState) {
        saves.incrementAndGet();
        activityScope.destroy();
      }

      @Override public void onDestroy() {
        destroys.incrementAndGet();
      }
    }

    activityScope.register(new Destroyer());
    activityScope.register(new Destroyer());

    Bundle b = new Bundle();
    activityScope.onCreate(b);
    activityScope.onSaveInstanceState(b);

    assertThat(saves.get()).isEqualTo(1);
    assertThat(destroys.get()).isEqualTo(2);
  }

  @Test(expected = IllegalStateException.class)
  public void cannotOnCreateDestroyed() {
    activityScope.destroy();
    activityScope.onCreate(null);
  }

  @Test(expected = IllegalStateException.class)
  public void cannotOnSaveDestroyed() {
    activityScope.destroy();
    activityScope.onSaveInstanceState(new Bundle());
  }

  /** <a href="https://github.com/square/mortar/issues/46">Issue 46</a> */
  @Test
  public void registerWithDescendantScopesCreatedDuringParentOnCreateGetOnlyOneOnLoadCall() {
    final MyBundler childBundler = new MyBundler("child");
    final MyBundler grandChildBundler = new MyBundler("grandChild");

    final AtomicBoolean spawnSubScope = new AtomicBoolean(false);

    activityScope.register(new MyBundler("outer") {
      @Override public void onLoad(Bundle savedInstanceState) {
        if (spawnSubScope.get()) {
          MortarScope childScope =
              activityScope.requireChild(new ModuleAndBlueprint("child scope"));
          childScope.register(childBundler);
          // 1. We're in the middle of loading, so the usual register > load call doesn't happen.
          assertThat(childBundler.loaded).isFalse();

          childScope.requireChild(new ModuleAndBlueprint("grandchild scope"))
              .register(grandChildBundler);
          assertThat(grandChildBundler.loaded).isFalse();
        }
      }
    });

    spawnSubScope.set(true);
    activityScope.onCreate(null);

    // 2. But load is called before the onCreate chain ends.
    assertThat(childBundler.loaded).isTrue();
    assertThat(grandChildBundler.loaded).isTrue();
  }

  /**
   * Happened during first naive fix of
   * <a href="https://github.com/square/mortar/issues/46">Issue 46</a>.
   */
  @Test
  public void descendantScopesCreatedDuringParentOnLoadAreNotStuckInLoadingMode() {
    final ModuleAndBlueprint subscopeBlueprint = new ModuleAndBlueprint("subscope");

    activityScope.register(new MyBundler("outer") {
      @Override public void onLoad(Bundle savedInstanceState) {
        activityScope.requireChild(subscopeBlueprint).requireChild(subscopeBlueprint);
      }
    });

    activityScope.onSaveInstanceState(new Bundle());
    // No crash? Victoire!
  }
}
