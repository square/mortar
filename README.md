# Mortar

## Deprecated

Mortar had a good run and served us well, but new use is strongly discouraged. The app suite at Square that drove its creation is in the process of replacing Mortar with [Square Workflow](https://square.github.io/workflow/).

## What's a Mortar?

Mortar provides a simplified, composable overlay for the Android lifecycle,
to aid in the use of [Views as the modular unit of Android applications][rant].
It leverages [Context#getSystemService][services] to act as an a la carte supplier
of services like dependency injection, bundle persistence, and whatever else
your app needs to provide itself.

One of the most useful services Mortar can provide is its [BundleService][bundle-service],
which gives any View (or any object with access to the Activity context) safe access to
the Activity lifecycle's persistence bundle. For fans of the [Model View Presenter][mvp]
pattern, we provide a persisted [Presenter][presenter] class that builds on BundleService.
Presenters are completely isolated from View concerns. They're particularly good at
surviving configuration changes, weathering the storm as Android destroys your portrait
Activity and Views and replaces them with landscape doppelgangers.

Mortar can similarly make [Dagger][dagger] ObjectGraphs (or [Dagger2][dagger2]
Components) visible as system services. Or not &mdash; these services are
completely decoupled.

Everything is managed by [MortarScope][scope] singletons, typically
backing the top level Application and Activity contexts. You can also spawn
your own shorter lived scopes to manage transient sessions, like the state of
an object being built by a set of wizard screens.

<!-- 
  This example is a little bit confusing. Maybe explain why you would want to have an extended graph for a wizard, then explain how Mortar shadows the parent graph with that extended graph.
-->

These nested scopes can shadow the services provided by higher level scopes.
For example, a [Dagger extension graph][ogplus] specific to your wizard session
can cover the one normally available, transparently to the wizard Views.
Calls like `ObjectGraphService.inject(getContext(), this)` are now possible
without considering which graph will do the injection.

## The Big Picture

An application will typically have a singleton MortarScope instance.
Its job is to serve as a delegate to the app's `getSystemService` method, something like:

```java
public class MyApplication extends Application {
  private MortarScope rootScope;

  @Override public Object getSystemService(String name) {
    if (rootScope == null) rootScope = MortarScope.buildRootScope().build(getScopeName());

    return rootScope.hasService(name) ? rootScope.getService(name) : super.getSystemService(name);
  }
}
```

This exposes a single, core service, the scope itself. From the scope you can
spawn child scopes, and you can register objects that implement the
[Scoped](https://github.com/square/mortar/blob/master/mortar/src/main/java/mortar/Scoped.java#L18)
interface with it for setup and tear-down calls.

  * `Scoped#onEnterScope(MortarScope)`
  * `Scoped#onExitScope(MortarScope)`

To make a scope provide other services, like a [Dagger ObjectGraph][og], 
you register them while building the scope. That would make our Application's
`getSystemService` method look like this:

```java
  @Override public Object getSystemService(String name) {
    if (rootScope == null) {
      rootScope = MortarScope.buildRootScope()
        .with(ObjectGraphService.SERVICE_NAME, ObjectGraph.create(new RootModule()))
        .build(getScopeName());
    }

    return rootScope.hasService(name) ? rootScope.getService(name) : super.getSystemService(name);
  }
```

Now any part of our app that has access to a `Context` can inject itself:

```java
public class MyView extends LinearLayout {
  @Inject SomeService service;

  public MyView(Context context, AttributeSet attrs) {
    super(context, attrs);
    ObjectGraphService.inject(context, this);
  }
}
```

To take advantage of the BundleService describe above, you'll put similar code
into your Activity. If it doesn't exist already, you'll
build a sub-scope to back the Activity's `getSystemService` method, and 
while building it set up the `BundleServiceRunner`. You'll also notify 
the BundleServiceRunner each time `onCreate` and `onSaveInstanceState` are 
called, to make the persistence bundle available to the rest of the app. 

```java
public class MyActivity extends Activity {
  private MortarScope activityScope;

  @Override public Object getSystemService(String name) {
    MortarScope activityScope = MortarScope.findChild(getApplicationContext(), getScopeName());

    if (activityScope == null) {
      activityScope = MortarScope.buildChild(getApplicationContext()) //
          .withService(BundleServiceRunner.SERVICE_NAME, new BundleServiceRunner())
          .withService(HelloPresenter.class.getName(), new HelloPresenter())
          .build(getScopeName());
    }

    return activityScope.hasService(name) ? activityScope.getService(name)
        : super.getSystemService(name);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    BundleServiceRunner.getBundleServiceRunner(this).onCreate(savedInstanceState);
    setContentView(R.layout.main_view);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    BundleServiceRunner.getBundleServiceRunner(this).onSaveInstanceState(outState);
  }
}
```

With that in place, any object in your app can sign up with the `BundleService`
to save and restore its state. This is nice for views, since Bundles are less
of a hassle than the `Parcelable` objects required by `View#onSaveInstanceState`,
and a boon to any business objects in the rest of your app. 

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

Gradle:

```groovy
compile 'com.squareup.mortar:mortar:(latest version)'
```

## Full Disclosure

This stuff has been in "rapid" development over a pretty long gestation period, 
but is finally stabilizing. We don't expect drastic changes before cutting a
1.0 release, but we still cannot promise a stable API from release to release.

Mortar is a key component of multiple Square apps, including our flagship
[Square Register][register] app.

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

[bundle-service]: https://github.com/square/mortar/blob/master/mortar/src/main/java/mortar/bundler/BundleService.java
[mvp]: http://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93presenter
[dagger]: http://square.github.io/dagger/
[dagger2]: http://google.github.io/dagger/
[jar]: http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.squareup.mortar&a=mortar&v=LATEST
[og]: https://square.github.io/dagger/1.x/dagger/dagger/ObjectGraph.html
[ogplus]: https://github.com/square/dagger/blob/dagger-parent-1.1.0/core/src/main/java/dagger/ObjectGraph.java#L96
[presenter]: https://github.com/square/mortar/blob/master/mortar/src/main/java/mortar/Presenter.java
[rant]: http://corner.squareup.com/2014/10/advocating-against-android-fragments.html
[register]: https://play.google.com/store/apps/details?id=com.squareup
[scope]: https://github.com/square/mortar/blob/master/mortar/src/main/java/mortar/MortarScope.java
[services]: http://developer.android.com/reference/android/content/Context.html#getSystemService(java.lang.String)
