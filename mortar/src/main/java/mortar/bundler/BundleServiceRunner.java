package mortar.bundler;

import android.content.Context;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import mortar.MortarScope;
import mortar.Presenter;
import mortar.Scoped;

public class BundleServiceRunner implements Scoped {
  public static final String SERVICE_NAME = BundleServiceRunner.class.getName();

  public static BundleServiceRunner getBundleServiceRunner(Context context) {
    return (BundleServiceRunner) context.getSystemService(SERVICE_NAME);
  }

  public static BundleServiceRunner getBundleServiceRunner(MortarScope scope) {
    return scope.getService(SERVICE_NAME);
  }

  final Map<String, BundleService> scopedServices = new LinkedHashMap<>();
  final NavigableSet<BundleService> servicesToBeLoaded =
      new TreeSet<>(new BundleServiceComparator());

  Bundle rootBundle;

  enum State {
    IDLE, LOADING, SAVING
  }

  State state = State.IDLE;

  private String rootScopePath;

  BundleService requireBundleService(MortarScope scope) {
    BundleService service = scopedServices.get(bundleKey(scope));
    if (service == null) {
      service = new BundleService(this, scope);
      service.init();
    }
    return service;
  }

  @Override public void onEnterScope(MortarScope scope) {
    if (rootScopePath != null) throw new IllegalStateException("Cannot double register");
    rootScopePath = scope.getPath();
  }

  @Override public void onExitScope() {
    // Nothing to do.
  }

  /**
   * To be called from the host {@link android.app.Activity}'s {@link
   * android.app.Activity#onCreate}. Calls the registered {@link Bundler}'s {@link Bundler#onLoad}
   * methods. To avoid redundant calls to {@link Presenter#onLoad} it's best to call this before
   * {@link android.app.Activity#setContentView}.
   */
  public void onCreate(Bundle savedInstanceState) {
    rootBundle = savedInstanceState;

    for (Map.Entry<String, BundleService> entry : scopedServices.entrySet()) {
      BundleService scopedService = entry.getValue();
      if (scopedService.updateScopedBundleOnCreate(rootBundle)) {
        servicesToBeLoaded.add(scopedService);
      }
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

    // Make a dwindling copy of the services, in case one is deleted as a side effect
    // of another's onSave.
    List<Map.Entry<String, BundleService>> servicesToBeSaved =
        new ArrayList<>(scopedServices.entrySet());

    while (!servicesToBeSaved.isEmpty()) {
      Map.Entry<String, BundleService> entry = servicesToBeSaved.remove(0);
      if (scopedServices.containsKey(entry.getKey())) entry.getValue().saveToRootBundle(rootBundle);
    }

    state = State.IDLE;
  }

  void finishLoading() {
    if (state != State.IDLE) throw new AssertionError("Unexpected state " + state);
    state = State.LOADING;

    while (!servicesToBeLoaded.isEmpty()) {
      BundleService next = servicesToBeLoaded.first();
      next.loadOne();
      if (!next.needsLoading()) servicesToBeLoaded.remove(next);
    }

    state = State.IDLE;
  }

  String bundleKey(MortarScope scope) {
    if (rootScopePath == null) throw new IllegalStateException("Was this service not registered?");
    String path = scope.getPath();
    if (!path.startsWith(rootScopePath)) {
      throw new IllegalArgumentException(String.format("\"%s\" is not under \"%s\"", scope,
          rootScopePath));
    }

    return path.substring(rootScopePath.length());
  }
}
