package mortar.bundler;

import android.content.Context;

public interface BundleService {
  class Finder {
    public BundleService get(Context context) {
      return (BundleService) context.getSystemService(BundleService.class.getName());
    }
  }

  /**
   * <p>Registers {@link Bundler} instances to have {@link Bundler#onLoad} and
   * {@link Bundler#onSave} called from {@link BundleServiceProvider#onCreate} and {@link
   * BundleServiceProvider#onSaveInstanceState},
   * respectively.
   *
   * <p>In addition to the calls from {@link BundleServiceProvider#onCreate}, {@link
   * Bundler#onLoad} is
   * triggered by registration. In most cases that initial {@link Bundler#onLoad} is made
   * synchronously during registration. However, if a {@link Bundler} is registered while an
   * ancestor scope is loading its own {@link Bundler}s, its {@link Bundler#onLoad} will be
   * deferred until all ancestor scopes have completed loading. This ensures that a {@link Bundler}
   * can assume that any dependency registered with a higher-level scope will have been initialized
   * before its own {@link Bundler#onLoad} method fires.
   */
  void register(Bundler s);
}
