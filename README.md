# Mortar

Mortar eases the use of [Dagger][dagger] to divide Android apps into
composable modules. It is not a framework: there are no abstract Application
or Activity classes here. Mortar eschews magic. Rather, Mortar is a simple
library that makes it easy to pair thin views with dedicated controllers
(Presenters), isolated from most of the vagaries of the Activity life cycle.
The patterns it encourages have evolved in parallel across several Android
teams at Square.

Mortar relies on Dagger, and of course the Android runtime, but those are its
only direct dependencies. That said, it works very well with
[Retrofit][retrofit], [Flow][flow] and [RxJava][rxjava].
[Butterknife][butterknife] can be a fun partner too. (Use of all of these
libraries is illustrated in the sample app.)

## The Big Picture

A Mortar app has only a handful of Activity classes, ideally a single one. It
does not have Fragments or Loaders. Instead its UI is built primarily of plain
old [Views][view], which use [Dagger][dagger] to `@Inject` whatever services
they need.

Typically such a view is a thin thing that delegates all of its interesting
work to an injected controller of type [Presenter][presenter]. A Presenter
simplifies life by surviving configuration changes, and has just enough access
to the Activity lifecycle to be restored after process death.

As your app grows you probably want to divide it up into discrete parts, both
to retain your sanity and to be miserly with the scarce resources available on
a mobile device. You can divide it into a tree of [MortarScopes][scope]. Each
scope is defined by a [Blueprint][blueprint] that can serve as a single point
of reference for what that scope does, and in particular can define objects
visible only to the scope and its children.

Maybe a scope is associated with a particular view, or maybe it isn't.  For
example, an app might have a top level global scope that allows the injection
of fundamental services; a child of that which manages objects that require an
authenticated user; children of them for each Activity; and children of the
activity scopes for their various screens and regions.

### Lifecycle

A MortarScope provides a simple life cycle: it can tell interested parties
when it is destroyed if they implement the [Scoped][scoped] interface.
Scopes created at the Activity level or below can also manage instances of the
[Bundler][bundler] interface, which give access to the host Activity's persistence
Bundle. And that's the entire lifecyle that Mortar apps need to deal with:

  * `Bundler#onLoad(Bundle)`
  * `Bundler#onSave(Bundle)`
  * `Scoped#onDestroy()`

Note in particular that an activity's scope is not destroyed when a particular
instance of that activity is destroyed. It sticks around until someone calls
`MortarScope#destroy()`, typically in the `Activity#onDestroy` method if
`isFinishing()` is true. Any objects registered at or below the activity scope
will survive any number of onLoad and onSave calls as the device is rotated, as
the app pauses, etc. Of course process death and resurrection can strike at
any time, so each onSave() call should archive as if it were the last, and
each onLoad() should check to see if it's really a reload. But that's a lot
simpler than the usual gymnastics.

### Singletons Where You Want Them

Under the hood, scopes take advantage of one of Dagger's most interesting
features, the ability for an object graph to spawn a child graph (see
[ObjectGraph#plus][ogplus]). Singletons and other bindings defined in a parent
graph can be injected by all of its offspring, but the parent graph itself is
not modified by them. In practice, this means that a `@Singleton FooService`
defined in the root scope is available for injection to all parts of the app.
Any singletons defined in an activity's scope, say an `@Singleton FooEditor`,
can be injected by the activity and any of its views, but is not accessible to
the rest of the application. 

When a scope is destroyed it drops all references to its own object graph and
makes its children do the same, freeing up everything to be GC'd. Each portion
of the app has the convenience of global singletons, with no concerns that
precious RAM is being consumed by things the user doesn't care about at the
moment.

Another example: suppose you have a set of screens that slide in and out next
to a fixed portion of the screen, like a sidebar or an activity, and each
screen needs to share in the managment of that common region. The parent view
that owns the common ground can be tied to a parent scope, and starts and
stops child scopes for each child view. Children can simply inject the
presenter that drives the shared region. They don't need to know how far
up the view chain that action bar lives, or to somehow find their way to the 
activity. It's simply available.

## Less Talk More Code

Here's how you might actually write this stuff. 

This example presumes an activity is charge of displaying subscreens and that
you're using Flow's [Layouts][layouts] utilty to handle view creation.

```java
/**
 * @param nextScreen blueprint of the screen to show, must have Flow
 * annotation like {@literal @}Layout(R.layout.foo_screen)
 */
void showScreen(Blueprint nextScreen) {
  View currentView = findViewById(android.R.id.content);
  Mortar.getMortarScope(currentView.getContext()).destroy();

  MortarScope newScope = Mortar.getMortarScope(this).requireChild(nextScreen);
  Context newContext = new MortarContextWrapper(this, newScope);
  View screenView = Layouts.createView(newContext, nextScreen);

  setContentView(screenView);
}
```

It doesn't take a lot of imagination to see how you'd specify animations for
the transition between these two views. You'd just…do it.

### Take control

This view-centered approach to composition doesn't mean that Mortar apps are
untestable. It's trivial to move all the interesting parts of a view  over to
a [Presenter][presenter] controller. And because presenters survive config
changes like rotation, they can be a lot easier to work with than code trapped
over in Context-land. To do this a view injects its presenter, lets that
presenter know when it's ready to roll from `onFinishInflate()`, and
surrenders control in `onDetachedFromWindow()`.

Here's an example, with one small conceit—we're using Flow's [@Layout][layout]
annotation for consistency with the previous example.

```xml
<com.example.MyView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
   >
  <EditText
      android:id="@id/some_text"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
    />
</com.example.MyView>
```

```java
public class MyView extends LinearLayout {
  @Inject MyScreen.Presenter presenter;
  private EditText someText;

  public MyView(Context context) {
    super(context);
    Mortar.inject(context, this);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    presenter.takeView(this);

    someText = (TextView) findById(R.id.some_text);

    findById(R.id.some_button).setOnClickListener(new OnClickListener() {
      public void onClick(View view) {
       presenter.someButtonClicked();
      }
    });
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    presenter.dropView(this);
  }

  @Override public String getSomeText() {
    return someText.getText().toString();
  }

  @Override public void showResult(SomeResult result) {
    ...
  }
 }
```

```java
@Layout(R.layout.my_view)
public class MyScreen implements Blueprint {
  @Override public String getMortarScopeName() {
    return getClass().getName();
  }

  @Override public Object getDaggerModule() {
    return new Module();
  }

  @dagger.Module(injects = MyView.class, addsTo = MyActivity.Module.class)
  public class Module {
  }

  @Singleton
  public class Presenter extends ViewPresenter<MyView> {
    private final SomeAsyncService service;

    private SomeResult lastResult;

    @Inject
    Presenter(SomeAsyncService service) {
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
      service.doSomethingAsync(getView().getSomeText(),
          new AsyncCallback<SomeResult>() {
            public void onResult(SomeResult result)
              lastResult = result;
              if (getView() != null) updateView();
            }
          });
    }

    private void updateView() {
      view.showResult(lastResult);
    }
  }
}
```

Notice how naturally this presenter copes with the possiblity that the view
that made it start some asynchronous process might no longer be available when
the result eventually arrives. 

Another subtle but important point: we're assuming that all views are
instantiated as a side effect of inflating a layout file. While not an
enforced requirement, this is certainly a best practice. It ensures that
`onFinishInflate()` will not be alled from the View's constructor (doing work
with partially constructed objects is a classic Java pitfall), and  it means
that Android theming will work as expected.

### Bootstrapping

Mortar requires a little bit of wiring to do its job. Its main trick is to
require all Views' contexts to implement the [MortarContext][mortarcontext]
interface. In practice this means your activities implement it, and when you
create views for subscopes you manufacture their contexts by calling
`MortarScope#createContext`. You'll also need a custom
[Application][application] subclass to hold the root scope. (This is how
scopes survive configuration changes.)

So declare your custom app in `AndroidManifest.xml`:

```xml
<application
    android:label="My App"
    android:name=".MyApplication"
    >
```
```java
public class MyApplication extends Application implements MortarContext {
  private MortarScope applicationScope;

  @Override public void onCreate() {
    super.onCreate();
    applicationScope = Mortar.createRootScope(BuildConfig.DEBUG);
  }

  @Override public MortarScope getMortarScope() {
    return applicationScope;
  }
}
```

Make your activities, or a common superclass they all extend,  create or
restore a `MortarActivityScope`.

```java
public abstract class MyBaseActivity extends Activity implements MortarContext {
  private MortarActivityScope activityScope;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    activityScope = Mortar.getActivityScope(getParentScope(), getBlueprint());
    activityScope.onCreate(savedInstanceState);
  }

  /**
   * Return the {@link Blueprint} that defines the {@link MortarScope} for this activity.
   */
  protected abstract Blueprint getBlueprint();

  @Override protected void onResume() {
    super.onResume();
    activityScope.onResume();
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    activityScope.onSaveInstanceState(outState);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    if (isFinishing()) {
      activityScope.destroy();
      activityScope = null;
    }
  }

  @Override public MortarScope getMortarScope() {
    return activityScope;
  }

  private MortarScope getParentScope() {
    return Mortar.getScope(getApplicationContext());
  }
}

```


Download
--------

Download [the latest JAR][jar] or grab via Maven:

```xml
<dependency>
    <groupId>com.squareup.mortar</groupId>
    <artifactId>mortar</artifactId>
    <version>(insert latest version)</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.squareup.mortar:mortar:(latest version)'
```

## Full Disclosure

This stuff is in rapid development, and has been open sourced at an earlier
time in its life than is typical for Square. While we have a lot of code
written in this style, we are still in the process of migrating to Mortar per
se. As its use broadens bugs will be fixed and apis will be broken. (Ideally
the apis will need to change because of new uses that you find!)


License
--------

    Copyright 2013 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[blueprint]: https://github.com/square/mortar/blob/master/mortar/src/main/java/mortar/Blueprint.java
[bundler]: https://github.com/square/mortar/blob/master/mortar/src/main/java/mortar/Bundler.java
[butterknife]: http://jakewharton.github.io/butterknife/
[dagger]: http://square.github.io/dagger/
[flow]: https://github.com/square/flow
[jar]: http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.squareup.mortar&a=mortar&v=LATEST
[layout]: https://github.com/square/flow/blob/master/flow/src/main/java/flow/Layout.java
[layouts]: https://github.com/square/flow/blob/master/flow/src/main/java/flow/Layouts.java
[ogplus]: https://github.com/square/dagger/blob/dagger-parent-1.1.0/core/src/main/java/dagger/ObjectGraph.java#L96
[mortarcontext]: https://github.com/square/mortar/blob/master/mortar/src/main/java/mortar/MortarContext.java
[presenter]: https://github.com/square/mortar/blob/master/mortar/src/main/java/mortar/Presenter.java
[retrofit]: http://square.github.io/retrofit/
[rxjava]: https://github.com/Netflix/RxJava
[scoped]: https://github.com/square/mortar/blob/master/mortar/src/main/java/mortar/Scoped.java
[scope]: https://github.com/square/mortar/blob/master/mortar/src/main/java/mortar/MortarScope.java
[view]: http://developer.android.com/reference/android/view/View.html
