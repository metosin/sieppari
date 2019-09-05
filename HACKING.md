# Testing

## CLI

```shell script
lein kaocha-clj unit # JVM
lein kaocha-cljs unit-cljs # cljs on node
```

`kaocha-cljs` [requires](https://github.com/lambdaisland/kaocha-cljs) `ws` and `isomorphic-ws` from npm.
A `package(-lock).json` is provided so you can just `npm install` those.
 
## REPL

```clojure
(kaocha/run :unit) # `user` ns has alias `kaocha` -> `kaocha.repl`
```
