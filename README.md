# sieppari

Small, fast, and complete interceptor library.

> Noun
> **Siepata (Intercept)**
> 
>   sieppari, _someone or something that intercepts_

**This library is still very much under development**

## What it does

Interceptors, like in [Pedestal](http://pedestal.io/reference/interceptors), but
with minimal implementation and optimal performance.

The core _Sieppari_ depends on Clojure and nothing else.

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

If you are new to interceptors, check the
[Pedestal Interceptors documentation](http://pedestal.io/reference/interceptors).
If you are familiar with interceptors you might want to jump to `Differences to Pedestal` below.

## Async

Add a dependency to one of the Sieppari async libraries (see 
[sieppari.async.core-async](https://github.com/metosin/sieppari/tree/develop/modules/sieppari.async.core-async) and 
[sieppari.async.deref](https://github.com/metosin/sieppari/tree/develop/modules/sieppari.async.deref)) and your done, 
now you interceptors and handlers can return core-async channels or dereffables (like `future` and `promise`).

To extend Sieppari async support to other libraries, see 
[sieppari.async/AsyncContext](https://github.com/metosin/sieppari/blob/develop/modules/sieppari.core/src/sieppari/async.clj)
protocol.

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

_Pedestal_ has built in support for `core.async`.

_Sieppari_ has extendable async support. Support for `core.async` and clojure
dereffables (like `future` and `promise`) is provided in add-on modules.

# Thanks

* Original idea from [Pedestal Interceptors](https://github.com/pedestal/pedestal/tree/master/interceptor).
* Motivation @ikitommi
* Topology sorting @nilern 

## License

Copyright &copy; 2018 [Metosin Oy](https://www.metosin.fi/)

Distributed under the Eclipse Public License, the same as Clojure.
