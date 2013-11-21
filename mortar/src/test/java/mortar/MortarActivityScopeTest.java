// Copyright 2013 Square, Inc.
package mortar;

import android.os.Bundle;
import dagger.Module;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

// Robolectric allows us to use Bundles.
@RunWith(RobolectricTestRunner.class) @Config(manifest = Config.NONE)
public class MortarActivityScopeTest {

  public static final String BUNDLER = "bundler";

  private static class MyBundler implements Bundler {
    final String name;

    boolean loaded;
    Bundle lastLoaded;
    Bundle lastSaved;
    boolean destroyed;

    MyBundler() {
      this(BUNDLER);
    }

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

    @Override public String getMortarScopeName() {
      return "scope name";
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
    MortarScope root = Mortar.createRootScope(false, new ModuleAndBlueprint());
    activityScope = Mortar.getActivityScope(root, new ModuleAndBlueprint());
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
    doLifecycleTest(activityScope.requireChild(new ModuleAndBlueprint()));
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
    // Create does not effect bundlers.
    assertThat(able.loaded).isFalse();
    assertThat(baker.loaded).isFalse();

    // Resuming, we load again
    activityScope.onResume();
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

    // Resuming the same activity instance, the out bundle is sent to load()
    activityScope.onResume();
    assertThat(able.lastLoaded).isSameAs(able.lastSaved);
    assertThat(baker.lastLoaded).isSameAs(baker.lastSaved);

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
    activityScope.onResume();
    assertThat(able.lastLoaded).isNotNull();

    verifyNoMoreInteractions(scoped);

    activityScope.destroy();
    assertThat(able.destroyed).isTrue();
    verify(scoped).onDestroy();
  }

  class FauxActivity {
    final MyBundler rootPresenter = new MyBundler("root");

    MortarScope childScope;
    MyBundler childPresenter = new MyBundler("child");

    void create(Bundle bundle) {
      activityScope.onCreate(bundle);
      activityScope.register(rootPresenter);
      childScope = activityScope.requireChild(new ModuleAndBlueprint());
      childScope.register(childPresenter);
    }
  }

  @Test public void childInfoSurvivesProcessDeath() {
    FauxActivity activity = new FauxActivity();
    activity.create(null);
    activityScope.onResume();
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

  @Test public void handlesRegisterFromOnLoadBeforeResume() {
    final MyBundler bundler = new MyBundler("inner");

    activityScope.register(new MyBundler("outer") {
      @Override public void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        activityScope.register(bundler);
      }
    });

    // The recursive register call loaded immediately.
    assertThat(bundler.loaded).isTrue();

    // And it was registered: a resume call reloads it.
    bundler.reset();
    Bundle b = new Bundle();
    activityScope.onSaveInstanceState(b);
    activityScope.onResume();

    assertThat(bundler.loaded).isNotNull();
  }

  @Test public void handlesRegisterFromOnLoadAfterResume() {
    final MyBundler bundler = new MyBundler("inner");

    activityScope.onCreate(null);
    activityScope.onResume();

    activityScope.register(new MyBundler("outer") {
      @Override public void onLoad(Bundle savedInstanceState) {
        activityScope.register(bundler);
      }
    });

    // The recursive register call loaded immediately.
    assertThat(bundler.loaded).isTrue();

    // And it was registered: a resume call reloads it.
    bundler.reset();
    Bundle b = new Bundle();
    activityScope.onSaveInstanceState(b);
    activityScope.onResume();

    assertThat(bundler.loaded).isNotNull();
  }

  @Test public void handlesRegisterFromOnSave() {
    final MyBundler bundler = new MyBundler("inner");

    activityScope.onCreate(null);
    activityScope.onResume();

    activityScope.register(new MyBundler("outer") {
      @Override public void onSave(Bundle outState) {
        super.onSave(outState);
        activityScope.register(bundler);
      }
    });
    assertThat(bundler.loaded).isFalse();

    activityScope.onSaveInstanceState(new Bundle());
    // Nothing should happen until resume
    assertThat(bundler.loaded).isFalse();

    activityScope.onResume();
    assertThat(bundler.loaded).isTrue();
  }

  @Test public void handlesReregistrationBeforeResume() {
    final AtomicInteger i = new AtomicInteger(0);

    activityScope.register(new Bundler() {
      @Override public String getMortarBundleKey() {
        return "key";
      }

      @Override public void onLoad(Bundle savedInstanceState) {
        i.incrementAndGet();
        activityScope.register(this);
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
    activityScope.onResume();
    // Sure, it's a redundant call, but it's not an infinite loop. I think that's fine for
    // a corner case, esp. since onLoad() is requried to be idempotent.
    assertThat(i.get()).isEqualTo(2);
  }

  @Test public void handlesReregistrationAfterResume() {
    Bundle b = new Bundle();
    activityScope.onCreate(b);
    activityScope.onResume();

    final AtomicInteger i = new AtomicInteger(0);

    activityScope.register(new Bundler() {
      @Override public String getMortarBundleKey() {
        return "key";
      }

      @Override public void onLoad(Bundle savedInstanceState) {
        assertThat(i.incrementAndGet()).isEqualTo(1);
        activityScope.register(this);
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
    activityScope.onResume();

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

    activityScope.onSaveInstanceState(new Bundle());

    // No load should happen until resume
    assertThat(saves.get()).isEqualTo(1);
    assertThat(loads.get()).isEqualTo(1);

    activityScope.onResume();
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
    activityScope.onResume();

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
    activityScope.onResume();
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
  public void cannotOnResumeDestroyed() {
    activityScope.destroy();
    activityScope.onResume();
  }

  @Test(expected = IllegalStateException.class)
  public void cannotOnSaveDestroyed() {
    activityScope.destroy();
    activityScope.onSaveInstanceState(new Bundle());
  }
}
