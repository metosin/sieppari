# 0.0.0-alpha9 (2020-05-04)

* Fix performance regressions from previous Alphas. 
* Tested against Promesa 5.*.

# 0.0.0-alpha8 (2019-11-06)

* Support `java.util.concurrent.CompletionStage` by default on the JVM and
  `js/Promise` for ClojureScript.
  * `sieppari.async.promesa` is not needed anymore and has been removed.
* Catch Async Exceptions
* Support Promesa 4.x (thanks to by [Andrea Richiardi](https://github.com/arichiardi) and [Andrey Antukh](https://github.com/niwinz))
* Remove automatic support for 3rd party async libs
  * Reduced `sieppari.core` load time by about 4 seconds.
  * `sieppari.async.*` namespaces now need to be `require`:d explicitly, you probably only need one of them.
* Run tests with Kaocha on JVM and node, then consolidate build tools back to just Leiningen.

# 0.0.0-alpha7 (2018-12-29)

* Initial support for ClojureScript by [Andrea Richiardi](https://github.com/arichiardi)
  * `core.async` & `promesa` (requires `1.10.0-SNAPSHOT`)
