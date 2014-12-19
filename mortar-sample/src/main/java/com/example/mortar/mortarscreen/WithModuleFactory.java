package com.example.mortar.mortarscreen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import mortar.MortarScope;

/**
 * Marks a screen as defining a {@link MortarScope}, with a factory class to
 * create its Dagger module.
 *
 * @see WithModule
 * @see ScreenScoper
 */
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
public @interface WithModuleFactory {
  Class<? extends ModuleFactory> value();
}
