Change Log
==========

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
