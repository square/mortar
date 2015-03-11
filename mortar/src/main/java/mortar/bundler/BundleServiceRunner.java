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

public class BundleServiceRunner {
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

  BundleService requireBundleService(MortarScope scope) {
    // TODO(ray) assert that the given scope is a child of the one this service runner occupies.
    // Maybe give MortarScope.Builder a getPath method, and
    // if (!scope.getPath().beginsWith(myPath + MortarScope.DIVIDER)) {
    //   throw new IllegalArgumentException()
    // }

    BundleService service = scopedServices.get(scope.getPath());
    if (service == null) {
      service = new BundleService(this, scope);
      service.init();
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
}
