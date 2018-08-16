# sieppari.async.deref

Support for Clojure dereffables for [Sieppari](https://github.com/metosin/sieppari) 
interception library.

## Usage

Just add dependency to this library to you project. For example, in Leiningen `project.clj` add:

```clj
[metosin/sieppari.async.deref "0.0.0-SNAPSHOT"]
```

Now your handlers and interceptors can return dereffables, like 
[promise](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/promise) and
[future](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/future). Anything
that implements [clojure.lang.IDeref](https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/IDeref.java)

For more information, see [Sieppari](https://github.com/metosin/sieppari) documentation.

## License

Copyright &copy; 2018 [Metosin Oy](https://www.metosin.fi/)

Distributed under the Eclipse Public License, the same as Clojure.
