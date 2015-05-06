package mortar;

import android.content.Context;
import android.os.Bundle;
import java.util.LinkedHashMap;
import java.util.Map;

import static mortar.MortarScope.DIVIDER;

public class NewBundleService implements Scoped {
  public static final String SERVICE_NAME = NewBundleService.class.getName();

  public static NewBundleService getBundleServiceRunner(Context context) {
    return (NewBundleService) context.getSystemService(SERVICE_NAME);
  }

  public static NewBundleService getBundleServiceRunner(MortarScope scope) {
    return scope.getService(SERVICE_NAME);
  }

  private final Bundle createBundle;
  private final Map<String, NewBundler> registrants = new LinkedHashMap<>();

  private String rootScopePath;

  public NewBundleService(Bundle createBundle) {
    this.createBundle = createBundle;
  }

  public void onSaveInstanceState(Bundle outState) {
    for (Map.Entry<String, NewBundler> entry : registrants.entrySet()) {
      Bundle bundle = new Bundle();
      entry.getValue().onSave(bundle);
      outState.putBundle(entry.getKey(), bundle);
    }
  }

  @Override public void onEnterScope(MortarScope scope) {
    if (rootScopePath != null) throw new IllegalStateException("Cannot double register");
    rootScopePath = scope.getPath();
  }

  @Override public void onExitScope() {
    // Nothing to do.
  }

  public void register(final MortarScope scope, final NewBundler bundler) {
    if (scope == null) {
      throw new NullPointerException("scope must not be null");
    }
    if (bundler == null) {
      throw new NullPointerException("bundler must not be null");
    }
    if (bundler.getMortarBundleKey().contains(DIVIDER)) {
      throw new IllegalArgumentException(String.format("Key must not contain \"%s\"", DIVIDER));
    }

    scope.register(new Scoped() {
      final String key = bundleKey(scope, bundler);

      @Override public void onEnterScope(MortarScope scope) {
        if (registrants.put(key, bundler) != null) {
          throw new IllegalArgumentException(
              String.format("Key \"%s\" already used in scope \"%s\"", bundler.getMortarBundleKey(),
                  scope.getPath()));
        }
        bundler.onLoad(createBundle.getBundle(key));
      }

      @Override public void onExitScope() {
        registrants.remove(key);
      }
    });
  }

  String bundleKey(MortarScope scope, NewBundler bundler) {
    if (rootScopePath == null) throw new IllegalStateException("Was this service not registered?");
    String path = scope.getPath();
    if (!path.startsWith(rootScopePath)) {
      throw new IllegalArgumentException(
          String.format("\"%s\" is not under \"%s\"", scope, rootScopePath));
    }

    return path.substring(rootScopePath.length()) + DIVIDER + bundler.getMortarBundleKey();
  }
}
