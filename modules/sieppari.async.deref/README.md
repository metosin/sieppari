# sieppari.async.deref

Support for Clojure dereffables for [Sieppari](https://github.com/metosin/sieppari) 
interception library.

## Usage

Just add dependency to this library to you project. For example, in Leiningen `project.clj` add:

```clj
[metosin/sieppari.async.deref "0.0.0-SNAPSHOT"]
```

Now your handlers and interceptors can return dereffables. For example:

```clj
(defn my-handler [request]
  (future
    ; some heavy computation here
    ))
```

For more information, see [Sieppari](https://github.com/metosin/sieppari) documentation.

## License

Copyright &copy; 2018 [Metosin Oy](https://www.metosin.fi/)

Distributed under the Eclipse Public License, the same as Clojure.
