# Testing

## CLI

```shell script
lein kaocha-clj unit # JVM
lein kaocha-cljs unit-cljs # cljs on node
```

## REPL

```clojure
(kaocha/run :unit) # `user` ns has alias `kaocha` -> `kaocha.repl`
```
