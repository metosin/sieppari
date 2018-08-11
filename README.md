# sieppari

Small [http://pedestal.io/reference/interceptors](interceptor) library with built-in 
dependency sorting and applicability filtering.

> Noun
> **Siepata (Intercept)**
> 
>   sieppari, _someone or something that intercepts_

**This library is still very much under development**

## What it does

Interceptors, like in [http://pedestal.io/](Pedestal), but
with optional filtering and automatic dependency sorting.

## First example

```clj
(ns example.simple
  (:require [sieppari.core :as sc]
            [sieppari.execute.sync :as ses]))

;; Simple interceptor, in enter update value in `[:request :in]` with `inc`:

(def inc-in-interceptor
  {:enter (fn [ctx] 
            (update-in ctx [:request :in] inc))})

;; Simple handler, take `:in` from request, apply `inc`, and
;; return an map with `:out`.

(defn handler [request]
  {:out (inc (:in request))})

(def interceptor-chain (sc/into-interceptors [inc-in-interceptor
                                              handler]))

(ses/execute interceptor-chain {:in 40})
;=> {:out 42}
```

If you are new to interceptors, check the
[http://pedestal.io/reference/interceptors](Pedestal Interceptors documentation).
If you are familiar with interceptors you might want to jump to [Differences to Pedestal].

## More examples

Following examples use the same ns aliases as above.

```clj
(defn make-interceptor [name]
  {:enter (fn [ctx] (println "ENTER:" name) ctx)
   :leave (fn [ctx] (println "LEAVE:" name) ctx)
   :error (fn [ctx] (println "ERROR:" name) ctx)})

(def interceptor-chain
  (sc/into-interceptors
    [(make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     (fn [request]
       (println "HANDLER: request =" (pr-str request))
       "handler response")]))

(ses/execute interceptor-chain "request message")
; Prints:
;  ENTER: :a
;  ENTER: :b
;  ENTER: :c
;  HANDLER: request = "request message"
;  LEAVE: :c
;  LEAVE: :b
;  LEAVE: :a
;=> "handler response"

```

In this example, the handler causes an exception:

```clj
;;
;; Handler causes an exception:
;;

(def interceptor-chain
  (sc/into-interceptors
    [(make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     (fn [request]
       (println "HANDLER: request =" (pr-str request))
       (throw (ex-info "oh no" {})))]))

(ses/execute interceptor-chain "request message")
; Prints:
;  ENTER: :a
;  ENTER: :b
;  ENTER: :c
;  HANDLER: request = "request message"
;  ERROR: :c
;  ERROR: :b
;  ERROR: :a
;=> CompilerException clojure.lang.ExceptionInfo: oh no {}
```

Update the `:b` interceptor so that it handles the exception caused
by the handler:

```clj
;;
;; Handler :b handles the exception:
;;

(def interceptor-chain
  (sc/into-interceptors
    [(make-interceptor :a)
     (-> (make-interceptor :b)
         (assoc :error (fn [ctx]
                         (println "ERROR: :b - this handles the exception")
                         (-> ctx
                             (dissoc :exception)
                             (assoc :response :fixed-by-b)))))
     (make-interceptor :c)
     (fn [request]
       (println "HANDLER: request =" (pr-str request))
       (throw (ex-info "oh no" {})))]))

(ses/execute interceptor-chain "request message")
; Prints:
;  ENTER: :a
;  ENTER: :b
;  ENTER: :c
;  HANDLER: request = "request message"
;  ERROR: :c
;  ERROR: :b - this handles the exception
;  LEAVE: :a
;=> :fixed-by-b
```

Interceptor can terminate the *enter* path by setting the response to
the `ctx`. This cancels the execution of further interceptors and handler and
starts the *leave* path.

```clj
;;
;; Interceptor can terminate execution in `enter` phase:
;;

(def interceptor-chain
  (sc/into-interceptors
    [(make-interceptor :a)
     (-> (make-interceptor :b)
         (assoc :enter (fn [ctx]
                         (println "ENTER: :b - short circuit")
                         (assoc ctx :response :short-circuit-by-b))))
     (make-interceptor :c)
     (fn [request]
       (println "HANDLER: request =" (pr-str request))
       "response from handler")]))

(ses/execute interceptor-chain "request message")
; Prints
;   ENTER: :a
;   ENTER: :b - short circuit
;   LEAVE: :a
;=> :short-circuit-by-b
``` 

Typically the interceptors change the `ctx`. In this example the
interceptors `conj` to `:stack` in `ctx` the stage and name.

```clj
;;
;; Interceptors can (and usually do) modify the `ctx`:
;;

(defn make-interceptor [name]
  {:enter (fn [ctx] (update ctx :stack conj [:enter name]))
   :leave (fn [ctx] (update ctx :stack conj [:leave name]))
   :error (fn [ctx] (update ctx :stack conj [:error name]))})

(def interceptor-chain
  (sc/into-interceptors
    [{:enter (fn [ctx]
               (assoc ctx :stack []))}
     (make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     (fn [request]
       (println "ENTER: request =" (pr-str request))
       {:response :response-from-handler})]))

(ses/execute interceptor-chain {})
; Prints:
;  ENTER: request = {}
;=> {:response :response-from-handler}
``` 

Note that the handler does not see the `:stack`. The handler is applied
with the value `(:request ctx)`. If the interceptor wishes to make something
visible to the handler it must do so by explicitly. 

Here we add a new interceptor just before the handler to take the `:stack` from
`ctx` and make it available to handler.

```clj
;;
;; Publish something from interceptor to handler:
;;

(def interceptor-chain
  (sc/into-interceptors
    [{:enter (fn [ctx]
               (assoc ctx :stack []))}
     (make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     {:enter (fn [ctx]
               (assoc-in ctx [:request :stack-so-far] (:stack ctx)))}
     (fn [request]
       (println "ENTER: stack =" (pr-str (:stack-so-far request)))
       {:response :response-from-handler})]))

(ses/execute interceptor-chain {})
; Prints: ENTER: stack = [[:enter :a] [:enter :b] [:enter :c]]
;=> {:response :response-from-handler}
```

Note how the last interceptor before the handler `assoc`ed the value
from `:stack` to request.

Handlers return value is placed under `:response` in `ctx`. It the
interceptor wished to change and modify the final response, it can do so
by updating `:response`.

This example changes the first interceptor so that it takes the `:stack`
from `ctx` and adds it to response.

```clj
;;
;; Publish something to response:
;;

(def interceptor-chain
  (sc/into-interceptors
    [{:enter (fn [ctx]
               (assoc ctx :stack []))
      :leave (fn [ctx]
               (assoc-in ctx [:response :final-stack] (:stack ctx)))}
     (make-interceptor :a)
     (make-interceptor :b)
     (make-interceptor :c)
     {:enter (fn [ctx]
               (assoc-in ctx [:request :stack-so-far] (:stack ctx)))}
     (fn [request]
       (println "ENTER: stack =" (pr-str (:stack-so-far request)))
       {:response :response-from-handler})]))

(ses/execute interceptor-chain {})
; Prints: ENTER: stack = [[:enter :a] [:enter :b] [:enter :c]]
;=> {:response :response-from-handler
;    :final-stack [[:enter :a]
;                  [:enter :b]
;                  [:enter :c]
;                  [:leave :c]
;                  [:leave :b]
;                  [:leave :a]]}
```

## Dependency sorting

Building an execution pipeline (typically a HTTP request processing)
with interceptors allows programmers to split functionality into
simple, single purpose, easily testable interceptors. Processing 
pipeline is created by stacking multiple interceptors into a stack.

Typically interceptors depend on other interceptors work. For
example, a session interceptor that attaches current user information
to request could depend on other interceptors to parse a session cookie 
and to a connection to the database.

It is important that the interceptors are stacked in correct order.
Maintaining the proper order of interceptors can easily become very
difficult task.

This library allows interceptors to declare dependant interceptors
and it automatically orders interceptors to an order where
interceptors are stacked in correct order.

* TODO: add `sieppari.ordering/dependency-order` example

## Applicability filtering

The execution of interceptors can consume computing resources. If
the interceptor is not required, it should bot be on the stack
at all. For example, if the handler does not need the current user
information, it would be waste to execute interceptors that fetch 
current user information from database.   

This library allows interceptors to declare a predicate to
determine if the handler requires the interceptor.

* TODO: add `sieppari.filtering/applies-to` example
 
# Differences to Pedestal

## Manipulation of the interceptor chain

Pedestal allows interceptors to manipulate the interceptor stack.
This library does not allow that.

## Terminating the **enter** stage

In _Pedestal_, to terminate the execution of an interceptor chain in 
**enter** stage you call the [terminate](http://pedestal.io/api/pedestal.interceptor/io.pedestal.interceptor.chain.html#var-terminate)
function. This clears the execution stack and begins the **leave**
stage.

In _Sieppari_, if the `enter` function returns a `ctx` with 
non-nil value under `:response` key, the **enter** stage is 
terminated and the **leave** stage begins.

## The **error** handler

In _Pedestal_ the `error` handler takes two arguments, the `ctx` and
the exception.

In _Sieppari_ the `error` handlers takes just one argument, the `ctx`,
and the exception is in `ctx` under the key `:exception`.

In _Pedestal_ the `error` handler resolves the exception by returning
the `ctx`, and continues the **error** stage by re-throwing the exception.

In _Sieppari_ the `error` handler resolves the exception by returning
the `ctx` with the `:exception` removed. To continue in the **error** 
stage, just return the `ctx` with the exception still at `:exception`. 

In _Pedestal_ the exception are wrapped in other exceptions. 

In _Sieppari_ exceptions are not wrapped.

## core.async

In _Pedestal_ the `core.async` support is included in all chains.

The async support will be introduced in _Sieppari_ as an optional
executor. It is expected async support in _Sieppari_ will be
an add on, so that the users of this library can choose the async
technology they desire.  

# Thanks

* Original idea from [Pedestal Interceptors](https://github.com/pedestal/pedestal/tree/master/interceptor).
* Motivation @ikitommi
* Topology sorting @nilern 

## License

Copyright &copy; 2018 [Metosin Oy](https://www.metosin.fi/)

Distributed under the Eclipse Public License, the same as Clojure.
