Change Log
==========

Version 0.20 *(2016-2-01)*
------------------
 * Detect multi-registered objects in different scopes of the same hierarchy, and throw an IllegalStateException. This ensures that `Scoped#onEnterScope` and `Scoped#onExitScope` calls are paired up.

Version 0.19 *(2015-08-04)*
------------------
 * Fixes ambiguous service lookup behavior of destroyed scopes:
    * `MortarScope.getScope(context).isDead()` returns true when you'd expect it to.
    * `ObjectGraph.inject(context, object)` throws if the backing scope is dead, as opposed to the current behavior where we instead try (and generally fail with a confusing message) to inject from an ancestor scope.
    * The behavior of `MortarScope.hasService(String)` is not changed in destroyed scopes. It always says yes if the service is provided by the receiving scope or an ancestor.

 * Deletes deprecated classes and methods:
    * `Blueprint`
    * `ObjectGraphService#requireActivityScope`
    * `ObjectGraphService#requireChild`

Version 0.18 *(2015-07-14)*
------------------
 * Destroying a scope recursively destroys its children first, like it used to.
   (0.17 API quake incorrectly reversed this.)

 * Now throws (fail fast!) when doing service lookup in a dead scope. 

 * Manually falls back to app context when a service is not found, to work 
   around the interval in a new activity's life where its base context
   is not yet set.

Version 0.17 *(2015-04-27)*
------------------
  **API Quake!**

  * Mortar is now decoupled from dependency injection in general, and from Dagger in particular.

  * Mortar core is now a service provider, meant to back Context#getSystemService, and handles registration of Scoped  objects.

  * MortarActivityScope is gone, replaced by BundleService and BundleServiceRunner. (Presenter is now built on those services, but basically unchanged.)

  * Dagger support has moved to ObjectGraphService. Blueprint moved with it, and is deprecated.

  * Main sample application continues to be overly complicated and confusing, working on it.

Version 0.16 *(2014-06-02)*
------------------
  * Repairs idempotence of MortarScope#destroyChild

  * Adds MortarScope#isDetroyed

Version 0.15 *(2014-05-29)*
------------------
  * API break: Presenter#onDestroy and Scoped#onDestroy are now onExitScope(MortarScope).
    Also adds onEnterScope(MortarScope) to those classes.

  * API break: MortarScope#destroyChild(MortarScope) replaces MortarScope#destroy.

Version 0.14 *(2014-04-18)*
------------------
  * Refine deferral of calls to Bundler#onLoad from MortarActivityScope#onRegister.
    See onRegister javadoc for details.

Version 0.13 *(2014-04-17)*
------------------
  * Fix accidental bundling of dagger-compiler

Version 0.12 *(2014-04-09)*
------------------
  * Guarantees that parent scopes will make their onLoad calls before children.
  * API break: MortarContext has been removed.  Activities must be careful to
    override getSystemService(); see the samples. This change allows
    Mortar to coexist peacefully with other ContextWrappers.

Version 0.11 *(2014-04-03)*
----------------------------
  * Presenter#onDestroy calling dropView was a bad, bad idea. Now it does
    nothing. Drop your own damn views.
  * MortarScopeDevHelper now dumps in alphabetical order, tests pass under 
    Java 8 

Version 0.10 *(2014-04-01)*
----------------------------
  * Fixes PopupPresenter state saving
  * Presenter#onDestroy wasn't calling dropView, does now.

Version 0.9 *(2014-03-28)*
----------------------------
  * Fixes redundant calls to Presenter#onLoad
  * Improved flow owner view in sample code
  * Fixes for redundant Bundler#onLoad calls when registering during onCreate
  * Better diagnostic dumps

Version 0.8 *(2014-03-03)*
----------------------------
  * Fixes bug with bundle key namespacing in presenters.

Version 0.7 *(2014-01-30)*
----------------------------
  * API break: MortarActivityScope#onResume is gone, and as you might expect
    Bundler#onLoad is not called at resume time. It just wasn't useful. See
    ChatScreen in the sample to see how to to handle pausing.

  * API break: Presenter is no longer a Bundler, and its onLoad method
    is never called with a null view.

  * New: Mortar#createRootScope(boolean) for simpler root scope creation.

  * New: Hello Mortar sample app.

Version 0.6 *(2014-01-20)*
----------------------------
  * API break: Mortar#createRootActivityScope is not practical, dropped

  * API break: Presenter#dropView now takes the dropped view as an argument,
    and is expected to be called by outgoing views. Presenter#view
    is no longer a weak reference.

Version 0.5 *(2014-01-21)*
----------------------------
  * API break: Simplified Presenter API, HasMortarScope renamed MortarContext and need not
    be implemented by view classes

  * API break: Root scope can belong to an activity, root ObjectGraph to be passed in.

Version 0.2 *(2013-11-12)*
----------------------------

Initial release.
