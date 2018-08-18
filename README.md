# sieppari

Small, fast, and complete interceptor library with built-in support
for common async libraries.

> Noun
> **Siepata (Intercept)**
> 
>   sieppari, _someone or something that intercepts_

## What it does

Interceptors, like in [Pedestal](http://pedestal.io/reference/interceptors), but
with minimal implementation and optimal performance.

The core _Sieppari_ depends on Clojure and nothing else.

If you are new to interceptors, check the
[Pedestal Interceptors documentation](http://pedestal.io/reference/interceptors).
If you are familiar with interceptors you might want to jump to `Differences to Pedestal` below.

## First example

```clj
(ns example.simple
  (:require [sieppari.core :as s]
            [sieppari.execute :as se]))

;; Simple interceptor, in enter update value in `[:request :x]` with `inc`:

(def inc-x-interceptor
  {:enter (fn [ctx]
            (update-in ctx [:request :x] inc))})

;; Simple handler, take `:x` from request, apply `inc`, and
;; return an map with `:y`.

(defn handler [request]
  {:y (inc (:x request))})

(def interceptor-chain (s/into-interceptors [inc-x-interceptor
                                             handler]))

(se/execute interceptor-chain {:x 40})
;=> {:y 42}
```

## Async

By default Sieppari has a support for clojure deferrables.

To add a support for one of the supported external async libraries, just add a dependency to them
and you are ready. Currently supported async libraries are:

* [core.async](https://github.com/clojure/core.async)
* [Manifold](https://github.com/ztellman/manifold)

To extend Sieppari async support to other libraries, extend a simple protocol 
[sieppari.async/AsyncContext](https://github.com/metosin/sieppari/blob/develop/modules/sieppari.core/src/sieppari/async.clj).

# Performance

_Sieppari_ aims for minimal functionality and can therefore be
quite fast. Complete example to test performance is 
[included](https://github.com/metosin/sieppari/blob/develop/examples/example/perf_testing.clj).

The example creates a chain of 100 interceptors that have 
`clojure.core/identity` as `:enter` and `:leave` functions and then
executes the chain. The async tests also have 100 interceptors, but
in async case they all return `core.async` channels on enter and leave. 

| Executor          | Execution time lower quantile |
| ----------------- | ----------------------------- |
| Pedestal sync     |  64 µs                        |
| Sieppari sync     |   9 µs                        |
| Pedestal async    | 410 µs                        |
| Sieppari async    | 396 µs                        |

* MacBook Pro (Retina, 15-inch, Mid 2015), 2.5 GHz Intel Core i7, 16 MB RAM
* Java(TM) SE Runtime Environment (build 1.8.0_151-b12)
* Clojure 1.9.0

# Differences to Pedestal

## The **error** handler

In _Pedestal_ the `error` handler takes two arguments, the `ctx` and
the exception.

In _Sieppari_ the `error` handlers takes just one argument, the `ctx`,
and the exception is in the `ctx` under the key `:error`.

In _Pedestal_ the `error` handler resolves the exception by returning
the `ctx`, and continues the **error** stage by re-throwing the exception.

In _Sieppari_ the `error` handler resolves the exception by returning
the `ctx` with the `:error` removed. To continue in the **error** 
stage, just return the `ctx` with the exception still at `:error`. 

In _Pedestal_ the exception are wrapped in other exceptions. 

In _Sieppari_ exceptions are not wrapped.

_Pedestal_ interception execution catches `java.lang.Throwable` for error 
processing. _Sieppari_ catches `java.lang.Exception`. This means that things 
like out of memory or class loader failures are not captured by _Sieppari_.

## Async

_Pedestal_ transfers thread local bindings from call-site into async interceptors.
_Sieppari_ does not support this.

# Thanks

* Original idea from [Pedestal Interceptors](https://github.com/pedestal/pedestal/tree/master/interceptor).
* Motivation @ikitommi
* Topology sorting @nilern 

## License

Copyright &copy; 2018 [Metosin Oy](https://www.metosin.fi/)

Distributed under the Eclipse Public License 2.0.

