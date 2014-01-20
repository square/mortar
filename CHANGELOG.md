Change Log
==========

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
