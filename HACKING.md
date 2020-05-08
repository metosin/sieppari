# Testing

## CLI

```shell script
lein kaocha # run clj and cljs tests
```

`kaocha-cljs` [requires](https://github.com/lambdaisland/kaocha-cljs#quickstart) `ws` from npm.
A `package(-lock).json` is provided so you can just `npm install` those.
 
## REPL

```clojure
(kaocha/run :unit) # `user` ns has alias `kaocha` -> `kaocha.repl`
```
