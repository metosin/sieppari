# Unreleased

* Bumped Promesa to 2.x (thanks to by [Andrea Richiardi](https://github.com/arichiardi))
  * Promesa allows you to choose the promise implementation, but Sieppari only supports the default `js/Promise` for now.
* Remove automatic support for 3rd party async libs
  * Reduced `sieppari.core` load time by about 4 seconds.
  * `sieppari.async.*` namespaces now need to be `require`:d explicitly, you probably only need one of them.
* Run tests with Kaocha on JVM and node, then consolidate build tools back to just Leiningen.

# 0.0.0-alpha7 (2018-12-29)

* Initial support for ClojureScript by [Andrea Richiardi](https://github.com/arichiardi)
  * `core.async` & `promesa` (requires `1.10.0-SNAPSHOT`)
