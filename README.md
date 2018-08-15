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

_Sieppari_ depends on Clojure and nothing else.

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
If you are familiar with interceptors you might want to jump to [Differences to Pedestal].

# Performance

_Sieppari_ aims for minimal functionality and can therefore be
quite fast. Complete example to test performance is 
[included](https://github.com/metosin/sieppari/blob/develop/examples/example/perf_testing.clj).

The example creates a chain of 100 interceptors that have 
`clojure.core/identity` as `:enter` and `:leave` functions and then
executes the chain.

| Executor      | Execution time lower quantile |
| ------------- | ----------------------------- |
| Pedestal      | 62.650689 µs                  |
| Sieppari      | 14.231854 µs                  |

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
the `ctx` with the `:exception` removed. To continue in the **error** 
stage, just return the `ctx` with the exception still at `:error`. 

In _Pedestal_ the exception are wrapped in other exceptions. 

In _Sieppari_ exceptions are not wrapped.

_Pedestal_ interception execution catches `java.lang.Throwable` for error 
processing. _Sieppari_ catches `java.lang.Exception`. This means that things 
like out of memory or class loader failures are not captured by _Sieppari_.

## Async

_Pedestal_ has built in support for `core.async`.

_Sieppari_ does not support any async at the moment. Extendable async support
is coming to _Sieppari_ in near future.

# Thanks

* Original idea from [Pedestal Interceptors](https://github.com/pedestal/pedestal/tree/master/interceptor).
* Motivation @ikitommi
* Topology sorting @nilern 

## License

Copyright &copy; 2018 [Metosin Oy](https://www.metosin.fi/)

Distributed under the Eclipse Public License, the same as Clojure.
