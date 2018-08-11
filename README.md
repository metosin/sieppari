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
with automatic dependency sorting and applicability filtering.

If you are new to interceptors, check the
[http://pedestal.io/reference/interceptors](Pedestal Interceptors documentation)
first.

## Dependency sorting

Building an execution pipeline (typically a HTTP request processing)
with interceptors allows programmers to split functionality into
simple, single purpose, easily testable interceptors. Processing 
pipeline is created by stacking multiple interceptors into a stack.

Interceptors depend on other interceptors to do their work. For
example, a session interceptor that attaches current user information
to request could depend on other interceptors to parse cookies and
to provide database connection.

Maintaining the proper order of interceptors can easily become very
difficult task.

This library allows interceptors to declare dependant interceptors
and it automatically orders interceptors to an order where
interceptors are stacked in correct order.

## Applicability filtering

The execution of interceptors can consume computing resources. If
the interceptor is not required, it should bot be on the stack
at all. 

This library allows interceptors to declare a predicate to
determine if the handler requires the interceptor.

For example, if the handler does not need current users information
the session interceptor can be omitted from the interceptor stack.
 
## Other differences to Pedestal

### Manipulation of the interceptor chain

Pedestal allows interceptors to manipulate the interceptor stack.
This library does not allow that.

### Terminating the **enter** stage

In _Pedestal_, to terminate the execution of an interceptor chain in 
**enter** stage you call the [terminate](http://pedestal.io/api/pedestal.interceptor/io.pedestal.interceptor.chain.html#var-terminate)
function. This clears the execution stack and begins the **leave**
stage.

In _Sieppari_, if the `enter` function returns a `ctx` with 
non-nil value under `:response` key, the **enter** stage is 
terminated and the **leave** stage begins.

### The **error** handler

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

## Usage

FIXME

## Thanks

Idea stolen from [Pedestal Interceptors](https://github.com/pedestal/pedestal/tree/master/interceptor).

## License

Copyright &copy; 2018 [Metosin Oy](https://www.metosin.fi/)

Distributed under the Eclipse Public License, the same as Clojure.
