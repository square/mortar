package mortar.bundler;

import android.content.Context;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mortar.MortarScope;
import mortar.MortarServiceProvider;
import mortar.Presenter;
import mortar.Scoped;

import static java.lang.String.format;

public class BundleServiceProvider implements MortarServiceProvider {
  public static class Finder {
    public BundleServiceProvider get(MortarScope scope) {
      return (BundleServiceProvider) scope.getServiceProvider(BundleService.class.getName());
    }
  }

  private final Map<String, ScopedBundleService> scopedServices = new LinkedHashMap<>();

  private Bundle rootBundle;

  private enum State {
    IDLE, LOADING, SAVING
  }

  private State state = State.IDLE;

  @Override public String getName() {
    return BundleService.class.getName();
  }

  @Override public Object getService(MortarScope scope) {
    ScopedBundleService service = scopedServices.get(scope.getPath());
    if (service == null) {
      service = new ScopedBundleService(scope);
      scopedServices.put(scope.getPath(), service);
      scope.register(service);
    }
    return service;
  }

  /**
   * To be called from the host {@link android.app.Activity}'s {@link
   * android.app.Activity#onCreate}. Calls the registered {@link Bundler}'s {@link Bundler#onLoad}
   * methods. To avoid redundant calls to {@link Presenter#onLoad} it's best to call this before
   * {@link android.app.Activity#setContentView}.
   */
  public void onCreate(Bundle savedInstanceState) {
    rootBundle = savedInstanceState;

    for (Map.Entry<String, ScopedBundleService> entry : scopedServices.entrySet()) {
      ScopedBundleService scopedService = entry.getValue();
      scopedService.loadFromRootBundleOnCreate();
    }
    finishLoading();
  }

  /**
   * To be called from the host {@link android.app.Activity}'s {@link
   * android.app.Activity#onSaveInstanceState}. Calls the registrants' {@link Bundler#onSave}
   * methods.
   */
  public void onSaveInstanceState(Bundle outState) {
    if (state != State.IDLE) {
      throw new IllegalStateException("Cannot handle onSaveInstanceState while " + state);
    }
    rootBundle = outState;

    state = State.SAVING;
    for (Map.Entry<String, ScopedBundleService> entry : scopedServices.entrySet()) {
      entry.getValue().saveToRootBundle();
    }
    state = State.IDLE;
  }

  private void finishLoading() {
    if (state != State.IDLE) throw new AssertionError("Unexpected state " + state);
    state = State.LOADING;

    boolean someoneLoaded;
    do {
      someoneLoaded = false;
      for (ScopedBundleService scopedService : scopedServices.values()) {
        someoneLoaded |= scopedService.doLoading();
      }
    } while (someoneLoaded);

    state = State.IDLE;
  }

  private class ScopedBundleService implements BundleService, Scoped {
    final MortarScope scope;
    final Set<Bundler> bundlers = new LinkedHashSet<>();

    Bundle scopeBundle;
    private List<Bundler> toBeLoaded = new ArrayList<>();

    ScopedBundleService(MortarScope scope) {
      this.scope = scope;
    }

    @Override public void onEnterScope(MortarScope scope) {
      // Nothing to do, we were just created and can't have any registrants yet.
    }

    @Override public void register(Bundler bundler) {
      if (bundler == null) throw new NullPointerException("Cannot register null bundler.");

      if (state == State.SAVING) {
        throw new IllegalStateException("Cannot register during onSave");
      }

      if (bundlers.add(bundler)) bundler.onEnterScope(scope);
      String mortarBundleKey = bundler.getMortarBundleKey();
      if (mortarBundleKey == null || mortarBundleKey.trim().equals("")) {
        throw new IllegalArgumentException(format("%s has null or empty bundle key", bundler));
      }

      switch (state) {
        case IDLE:
          toBeLoaded.add(bundler);
          finishLoading();
          break;
        case LOADING:
          if (!toBeLoaded.contains(bundler)) toBeLoaded.add(bundler);
          break;

        default:
          throw new AssertionError("Unexpected state " + state);
      }
    }

    @Override public void onExitScope() {
      for (Bundler b : bundlers) b.onExitScope();
      scopedServices.remove(scope.getPath());
    }

    /**
     * Load any {@link Bundler}s that still need it.
     *
     * @return true if we did some loading
     */
    boolean doLoading() {
      if (toBeLoaded.isEmpty()) return false;
      while (!toBeLoaded.isEmpty()) {
        Bundler next = toBeLoaded.remove(0);
        Bundle leafBundle =
            scopeBundle == null ? null : scopeBundle.getBundle(next.getMortarBundleKey());
        next.onLoad(leafBundle);
      }
      return true;
    }

    void loadFromRootBundleOnCreate() {
      scopeBundle = rootBundle == null ? null : rootBundle.getBundle(scope.getPath());
      toBeLoaded.addAll(bundlers);
    }

    void saveToRootBundle() {
      scopeBundle = new Bundle();
      rootBundle.putBundle(scope.getPath(), scopeBundle);

      for (Bundler bundler : bundlers) {
        Bundle childBundle = new Bundle();
        scopeBundle.putBundle(bundler.getMortarBundleKey(), childBundle);
        bundler.onSave(childBundle);
      }
    }
  }
}
