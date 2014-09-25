package com.example.mortar.mortarscreen;

import android.content.res.Resources;
import mortar.Blueprint;
import mortar.dagger1support.Dagger1Blueprint;

/** @see WithModuleFactory */
public abstract class ModuleFactory<T> {
  final Blueprint createBlueprint(final Resources resources, final String name,
      final T screen) {
    return new Dagger1Blueprint() {
      @Override public String getMortarScopeName() {
        return name;
      }

      @Override public Object getDaggerModule() {
        return ModuleFactory.this.createDaggerModule(resources, (T) screen);
      }
    };
  }

  protected abstract Object createDaggerModule(Resources resources, T screen);
}
