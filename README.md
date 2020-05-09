# sieppari

Small, fast, and complete interceptor library for Clojure/Script with built-in support
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
Sieppari's `sieppari.core/execute` follows a `:request` / `:response` pattern. For
Pedestal-like behavior, use `sieppari.core/execute-context`.

## First example

```clj
(ns example.simple
  (:require [sieppari.core :as sieppari]))

;; interceptor, in enter update value in `[:request :x]` with `inc`
(def inc-x-interceptor
  {:enter (fn [ctx]
            (update-in ctx [:request :x] inc))})

;; handler, take `:x` from request, apply `inc`, and return an map with `:y`
(defn handler [request]
  {:y (inc (:x request))})

(sieppari/execute
  [inc-x-interceptor handler]
  {:x 40})
;=> {:y 42}
```

## Async

Any step in the execution pipeline (`:enter`, `:leave`, `:error`) can return either a context map (synchronous execution) or an instance of [`AsyncContext`](https://github.com/metosin/sieppari/blob/develop/src/sieppari/async.cljc) - indicating asynchronous execution.

By default, clojure deferrables satisfy the `AsyncContext` protocol.

Using `sieppari.core/execute` with async steps will block:

```clj
;; async interceptor, in enter double value of `[:response :y]`:
(def multiply-y-interceptor
  {:leave (fn [ctx]
            (future
              (Thread/sleep 1000)
              (update-in ctx [:response :y] * 2)))})


(sieppari/execute
  [inc-x-interceptor multiply-y-interceptor handler]
  {:x 40})
; ... 1 second later:
;=> {:y 84}
```

There is also a non-blocking version of `execute`:

```clj
(let [respond (promise)
      raise (promise)]
  (sieppari/execute
    [inc-x-interceptor multiply-y-interceptor handler]
    {:x 40}
    respond
    raise) ; returns nil immediately

  (deref respond 2000 :timeout))
; ... 1 second later:
;=> {:y 84}
```

## External Async Libraries

To add a support for one of the supported external async libraries, just add a dependency to them and `require` the
respective Sieppari namespace. Currently supported async libraries are:

* [core.async](https://github.com/clojure/core.async) - `sieppari.async.core-async`, clj & cljs
* [Manifold](https://github.com/ztellman/manifold) - `sieppari.async.manifold` clj
* [Promesa](http://funcool.github.io/promesa/latest) - `sieppari.async.promesa` clj & cljs

To extend Sieppari async support to other libraries, just extend the `AsyncContext` protocol.

## core.async

Requires dependency to `[org.clojure/core.async "0.4.474"]` or higher.

```clj
(require '[clojure.core.async :as a])

(defn multiply-x-interceptor [n]
  {:enter (fn [ctx]
            (a/go (update-in ctx [:request :x] * n)))})

(sieppari/execute
  [inc-x-interceptor (multiply-x-interceptor 10) handler]
  {:x 40})
;=> {:y 411}
```

## manifold

Requires dependency to `[manifold "0.1.8"]` or higher.

```clj
(require '[manifold.deferred :as d])

(defn minus-x-interceptor [n]
  {:enter (fn [ctx]
            (d/success-deferred (update-in ctx [:request :x] - n)))})

(sieppari/execute
  [inc-x-interceptor (minus-x-interceptor 10) handler]
  {:x 40})
;=> {:y 31}
```

## promesa

Requires dependency to `[funcool/promesa "2.0.0-SNAPSHOT"]` or higher.

```clj
(require '[promesa.core :as p])

(defn divide-x-interceptor [n]
  {:enter (fn [ctx]
            (p/promise (update-in ctx [:request :x] / n)))})

(sieppari/execute
  [inc-x-interceptor (divide-x-interceptor 10) handler]
  {:x 40})
;=> {:y 41/10}
```

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

## Execution

* `io.pedestal.interceptor.chain/execute` executes _Contexts_
* `sieppari.core/execute` executes _Requests_ (which are internally wrapped inside a _Context_ for interceptors)

## Errors

* In _Pedestal_ the `error` handler takes two arguments, the `ctx` and the exception.
* In _Sieppari_ the `error` handlers takes just one argument, the `ctx`, and the exception is in the `ctx` under the key `:error`.
* In _Pedestal_ the `error` handler resolves the exception by returning the `ctx`, and continues the **error** stage by re-throwing the exception.
* In _Sieppari_ the `error` handler resolves the exception by returning the `ctx` with the `:error` removed. To continue in the **error**  stage, just return the `ctx` with the exception still at `:error`.
*  In _Pedestal_ the exception are wrapped in other exceptions.
* In _Sieppari_ exceptions are not wrapped.
* _Pedestal_ interception execution catches `java.lang.Throwable` for error processing. _Sieppari_ catches `java.lang.Exception`. This means that things like out of memory or class loader failures are not captured by _Sieppari_.

## Async

* _Pedestal_ transfers thread local bindings from call-site into async interceptors.
* _Sieppari_ does not support this.

# Thanks

* Original idea from [Pedestal Interceptors](https://github.com/pedestal/pedestal/tree/master/interceptor).

## License

Copyright &copy; 2018-2020 [Metosin Oy](https://www.metosin.fi/)

Distributed under the Eclipse Public License 2.0.

