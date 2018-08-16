# sieppari.async.core-async

Support for [core.async](https://github.com/clojure/core.async) for [Sieppari](https://github.com/metosin/sieppari) 
interception library.

## Usage

Just add dependency to this library to you project. For example, in Leiningen `project.clj` add:

```clj
[metosin/sieppari.async.core-async "0.0.0-SNAPSHOT"]
```

Now your handlers and interceptors can return `core.async` channels. For example:

```clj
(defn my-handler [request]
  (go
    ; some heavy computation here
    ))
```

For more information, see [Sieppari](https://github.com/metosin/sieppari) documentation.

## License

Copyright &copy; 2018 [Metosin Oy](https://www.metosin.fi/)

Distributed under the Eclipse Public License, the same as Clojure.
