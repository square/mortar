# Mortar

Mortar eases the use of [Dagger](http://square.github.io/dagger/) to divide
Android apps into composable modules. It is not a framework: there are no
abstract Application or Activity classes here. Mortar eschews magic.

Rather, Mortar is a simple library that makes it easy to pair thin views with
dedicated controllers (ViewPresenters), isolated from most of the vagaries of
the Activity life cycle. The patterns it encourages  have evolved in
parallel across several Android teams at Square.

Mortar relies on Dagger, and of course the Android runtime, but those are its
only direct dependencies. That said, it works very well with
[Retrofit](http://square.github.io/retrofit/),
[Flow](https://github.com/square/flow) and
[RxJava](https://github.com/Netflix/RxJava).
[Butterknife](http://jakewharton.github.io/butterknife/) can be a fun partner
too. (Use of all of these libraries is illustrated in the sample app.)

## Moving Parts

_This writeup presumes you are pretty familiar with
[Dagger](http://square.github.io/dagger/)._

A Mortar app has only a handful of Activity classes, maybe only a single one.
It does not have Fragments or Loaders, at least not at Square.
Instead its UI is built of Activities and Views, which inject whatever
services they need. Typically, though this is not required, these Activities
and Views are thin things that delegate most of their interesting work to
injected `ViewPresenters`, which simplify life by surviving configuration
changes.

### The scope tree

An app is structured as a tree of `MortarScope`s, each associated with a
Dagger ObjectGraph. A scope is defined by a `Blueprint`, which provides its
name and its Dagger
[Module](http://square.github.io/dagger/javadoc/dagger/Module.html). Typically
a Blueprint also declares the interface to be implemented by its main Activity
or View, as well as the ViewPresenter that drives it, but this is just a
convention.

For example, an app might have a top level global scope that allows the
injection of fundamental services; a child of that which manages objects that
require an authenticated user; children of them for each Activity; and
children of the activity scopes for their various screens and regions.

### Lifecycle

A MortarScope provides a simple life cycle: it can tell interested parties
when it is destroyed if they implement the `Scoped` interface. Scopes created
at the Activity level or below can also manage instances of the `Bundler`
interface, which give access to the host Activity's persistence Bundle. And
that's the entire lifecyle that Mortar apps need to deal with:

  * `Bundler#onLoad(Bundle)`
  * `Bundler#onSave(Bundle)`
  * `Scoped#onDestroy()`

Note in particular that an activity's scope is not destroyed when a particular
instance of that activity is destroyed. It sticks around until someone calls
`MortarScope#destroy()`, typically in the `Activity#finish` method. Any
objects registered at or below the activity scope will survive any number of
onLoad and onSave calls as the phone is rotated, as the app pauses, etc. Of
course process death and resurrection can strike at any time, so each onSave()
call should archive as if it were the last, and each onLoad() should check to
see if it's really a reload. But that's a lot simpler than the usual
gymnastics.

### Singletons where you want them

Under the hood, scopes take advantage of one of Dagger's most interesting
features, the ability for an object graph to spawn a child graph (see
[ObjectGraph#plus](https://github.com/square/dagger/blob/dagger-
parent-1.1.0/core/src/main/java/dagger/ObjectGraph.java#L96)). Singletons and
other bindings defined in a parent graph can be injected by all of its
offspring, but the parent graph itself is not modified by them. In practice,
this means that a `@Singleton FooService` defined in the root scope is
available for injection to all parts of the app. Any singletons defined in an
activity's scope, say an `@Singleton FooEditor`, can be injected by the
activity and any of its views, but is not accessible to the rest of the
application.

When a scope is destroyed, like when `MortarScope#destroy()` is called from an `Activity#finish()` method, it drops all references to its own object graph and makes its children do the same, freeing up everything to be GC'd. Each portion of the app has the convenience of global singletons, with no concerns that precious RAM is being consumed by things the user doesn't care about at the moment.

### Tiny…fragments…of scopes

MortarScopes can be much finer grained than one-per-activity. It's common to
define a scope to live only as long as an individual view occupies the screen.
At the moment this is a fairly manual process, but conceptually simple. We'll
probably get around to writing higher level support classes for this kind of
thing. In the meantime it might go something like the following.

This example presumes an activity is charge of display and that you're using
[Flow](https://github.com/square/flow)'s [@Screen](https://github.com/square/f
low/blob/master/flow/src/main/java/com/squareup/flow/Screens.java) utilty to
handle view creation.

```java
/**
 * @param nextScreen blueprint of the screen to show, must have Flow
 * annotation like {@literal @}Screen(R.layout.foo_screen)
 */
showScreen(Blueprint nextScreen) {
  View currentView = findViewById(android.R.id.content); // ick
  Mortar.getMortarScope(currentView).destroy();

  MortarScope newScope = Mortar.getMortarScope(this).requireChild(nextScreen);
  Context newContext = new MortarContextWrapper(this, newScope);
  View screenView = Screens.createView(newContext, nextScreen);

  setContentView(screenView);
}
```

It doesn't take a lot of imagination to see how you'd specify animations for
the transition between these two views. You'd just…do it.

### Take control

This view-centered approach to composition doesn't mean that Mortar apps are
untestable. It's trivial to move all the interesting parts of a view or
activity over to a `ViewPresenter` controller. And because presenters survive
config changes like rotation, they can be a lot easier to work with than code
trapped over in Context-land. To do this a view injects its presenter, and
lets it know when the view is ready to roll, typically from
`onAttachedToWindow()` (or `onCreate()` in an activity).

Here's an example, with one small conceit—we're using
[Flow](https://github.com/square/flow)'s [@Screen](https://github.com/square/f
low/blob/master/flow/src/main/java/com/squareup/flow/Screen.java) annotation
for consistency with the previous example.

Notice how naturally this presenter copes with the possiblity that the view
that made  it start some asynchronous process might no longer be available
when the result eventually comes.

```java
public class MyView extends View implements MyScreen.View {
  @Inject MyScreen.Presenter presenter;
  private final EditText someText;

  public MyView(Context context) {
    super(context);
    Mortar.inject(getContext(), this);

    someText = (TextView) findById(R.id.some_text);

    findById(R.id.some_button).setOnClickListener(new OnClickListener() {
      public void onClick(View view) {
       presenter.someButtonClicked();
      }
    });
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    presenter.takeView(this);
  }

  public String getSomeText() {
    return someText.getText().toString();
  }

  public void showResult(SomeResult result) {
    ...
  }
 }
```

```java

@Screen(MyView.class)
public class MyScreen implements Blueprint {
  @Override public String getMortarScopeName() {
    return getClass().getName();
  }

  @Override public Object getDaggerModule() {
    return new DaggerModule();
  }

  @Module(injects = { Presenter.class, MyView.class },
      addsTo = MyActivity.Module.class)
  public class DaggerModule {
  }

  public interface View extends HasGrenadeScope {
    String getSomeText();
    void showResult(SomeResult r);
  }

  @Singleton
  public class Presenter extends AbstractViewPresenter<View> {
    private final SomeAsyncService service;

    private SomeResult lastResult;

    @Inject
    MyPresenter(SomeAsyncService service) {
      this.service = service;
    }

    @Override public void onLoad(Bundle savedInstanceState) {
      super.onLoad(savedInstanceState);
      if (lastResult == null && savedInstanceState != null) {
        lastResult = savedInstanceState.getParcelable("last");
      }
      updateView();
    }

    @Override public void onSave(Bundle outState) {
      super.onSave(outState);
      if (lastResult != null) outState.putParcelable("last", lastResult);
    }

    public void someButtonClicked() {
      someAsyncService.doSomethingAsync(getView().getSomeText(),
          new AsyncCallback<SomeResult>() {
            public void onResult(SomeResult result)
              lastResult = result;
              updateView();
            }
          });
    }

    private void updateView() {
      View view = getView();
      if (view == null) return;

      view.showResult(lastResult);
    }
  }
}
```

## Bootstrapping

Mortar requires a bit of wiring to do its job. Its main trick is to require a
custom `Application` subclass and all participating activities to implement
the `HasMortarScope` interface. This allows every activity and view to find
its controlling scope via its `Context`.

So, your app must have a custom Application subclass…

```xml
<application
    android:label="My App"
    android:name=".MyApplication"
    >
```
…that hosts the root scope:

```java
public class MyApplication extends Application implements HasMortarScope {
  private MortarScope applicationScope;

  @Override public void onCreate() {
    super.onCreate();

    applicationScope =
        Mortar.createRootScope(BuildConfig.DEBUG, new ApplicationModule());
  }

  @Override public MortarScope getMortarScope() {
    return applicationScope;
  }
}
```

Participating activities, or a common superclass they all extend, need to
create or restore a `MortarActivityScope` specialized to broker the services
of the bundle.

```java
public abstract class MyBaseActivity extends Activity implements HasMortarScope {
  private MortarActivityScope activityScope;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    activityScope = Mortar.getActivityScope(getParentScope(), getBlueprint());
    activityScope.onCreate(this, savedInstanceState);
  }

  /**
   * Return the {@link Blueprint} that defines the {@link MortarScope} for this activity.
   */
  protected abstract Blueprint getBlueprint();

  @Override protected void onResume() {
    super.onResume();
    activityScope.onResume(this);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    activityScope.onSaveInstanceState(outState);
  }

  @Override public void finish() {
    super.finish();
    activityScope.destroy();
    activityScope = null;
  }

  @Override public MortarScope getMortarScope() {
    return activityScope;
  }

  private MortarScope getParentScope() {
    return Mortar.getScope(getApplicationContext());
  }
}

```
