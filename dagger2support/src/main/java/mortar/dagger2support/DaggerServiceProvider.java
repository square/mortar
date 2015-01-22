package mortar.dagger2support;

import android.content.Context;
import java.lang.reflect.Method;
import mortar.MortarScope;
import mortar.MortarServiceProvider;

public class DaggerServiceProvider<T> implements MortarServiceProvider {

  // TODO(ray) Use the seed instead. Perhaps we look for a IsDaggerComponent interface to get it.
  // And probably drop dagger from the Hello sample, or make a new one that adds dagger.

  private final T daggerComponent;

  protected DaggerServiceProvider(T daggerComponent) {
    this.daggerComponent = daggerComponent;
  }

  @Override public String getName() {
    return getClass().getName();
  }

  @Override public T getService(MortarScope scope) {
    return daggerComponent;
  }

  /**
   * Caller is required to know the type of the component for this context.
   */
  @SuppressWarnings("unchecked") //
  public static <T> T getDaggerComponent(Context context) {
    return (T) context.getSystemService(DaggerServiceProvider.class.getName());
  }

  /**
   * Magic method that creates a component with its dependencies set, by reflection. Relies on
   * Dagger2 naming conventions.
   */
  public static <T> DaggerServiceProvider<T> forComponent(Class<T> componentClass,
      Object... dependencies) {
    String fqn = componentClass.getName();

    String packageName = componentClass.getPackage().getName();
    // Accounts for inner classes, ie MyApplication$Component
    String simpleName = fqn.substring(packageName.length() + 1);

    try {
      Class<?> generatedClass = Class.forName(packageName + ".Dagger_" + simpleName);
      Object builder = generatedClass.getMethod("builder").invoke(null);

      for (Method method : builder.getClass().getMethods()) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 1) {
          Class<?> dependencyClass = params[0];
          for (Object dependency : dependencies) {
            if (dependencyClass.isAssignableFrom(dependency.getClass())) {
              method.invoke(builder, dependency);
              break;
            }
          }
        }
      }
      //noinspection unchecked
      T component = (T) builder.getClass().getMethod("build").invoke(builder);
      return new DaggerServiceProvider<>(component);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
