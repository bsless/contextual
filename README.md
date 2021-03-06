[![Clojars Project](https://img.shields.io/clojars/v/bsless/contextual.svg)](https://clojars.org/bsless/contextual)
[![cljdoc badge](https://cljdoc.org/badge/bsless/contextual)](https://cljdoc.org/d/bsless/contextual/CURRENT)
[![CircleCI](https://circleci.com/gh/bsless/contextual/tree/master.svg?style=shield)](https://circleci.com/gh/bsless/contextual/tree/master)

# Contextual

A two-phase Clojure interpreter. Write an expression once, run it many
times with good performance.

Deferred evaluation of Clojure expressions with late bindings of input.

## Not A Function

If you read the description above and say, "that's just a function",
you're right. So why not a function? Few reasons:
- Dynamic input: creating functions willy-nilly from out-of-process
  inputs is a potential nightmare.
- Special contexts: By evaluating an expression in a special context, we
  can use it to represent data templates. Example can be rendering HTML,
  or even a predetermined HTTP request.
- Symbolic manipulation.
- Limited Metaspace: Clojure functions are compiled to unique class
  instances. Class metadata is stored in the Metaspace. This space can
  theoretically run out over the life time of a long running
  application.

## Like An Expression

This library allows the user to store and treat expressions as data, and
safely evaluate them in different contexts. Moreover, these expressions
can be safely generated based on user input and run inside your
application with reasonable performance.

## Two Phase Interpreter

- Compile: An expression is compiled to a class hierarchy representing its tree structure
- Invoke: Evaluate the expression with given context. Only method calls, zero interpretations.

## Scope

Currently contextual is not a complete Clojure interpreter, but it
works, it's fast, and can be used.

Adding support for all of Clojure is on the roadmap, but features which
degrade performance might not be added.

## Usage

Using contextual involves two phases: compilation and execution.

Execution is always performed via `(contextual.core/invoke compiled-expr context-map)`

Compilation options:

- lookup: map from symbol to value. Used for resolving symbols during
  compilation. Can contain any value, from primitive to function, i.e.
  `{'foo clojure.core/println 'bar (->path :x y)}` is a valid lookup
  map.
- symbols registry: This is a map of special symbols to be resolved to
  constructors for objects which implement the protocols `IContext` or
  `IStringBuild`, to be used as new units of syntax and execution. An
  extension point for users.

Currently, the following compilations are available:

### Expressions

`(contextual.core/compile expr)`, where `expr` can contain any of the
supported symbols or resolvable symbols.

### HTTP Requests

`(contextual.http/compile request)` takes a template of containing they
keys `url path query-params body form method headers`, any of which
besides `url` is optional, and emits an invokable which would emit a map
with a corresponding structure after invoking all the expressions
contained in it.

Special HTTP options:
- `serialize-body`: when not false-y indicates the request body should
  be serialized with the provided `body-serializer`.
- `body-serializer`: any function which will serialize the emitted
  request body.
- `serialize-form`: when not false-y indicates the request form should
  be serialized with the provided `form-serializer`.
- `form-serializer`: any function which will serialize the emitted
  request form.
- `serialize-query-params`: when truth-y will append the query params to
  the end of the URL instead of emitting them as a map. i.e. `{:a 1 :b 2}` -> `?a=1&b=2`.

## Validation

The templating system can perform best-effort validation.

Use `contextual.validate/validate-expression`, which will report the following validations in a map:

- `unresolvable-symbols`: All the symbols which could not be resolved at
  expression compile time.
- `bad-function-calls`: All instances of expressions with a wrong number
  of arguments, function calls which aren't callable, and unresolved
  symbols. This overlaps slightly with `unresolvable-symbols`.

## Design

### Protocols

#### `IContext`

- `-invoke [this ctx]`: Invoke the given object with context `ctx`.
  Defaults to identity for `Object` and `nil`.

#### `IStringBuild`

- `-invoke-with-builder [this ctx sb]`: Invoke the given object with
  context `ctx` and `StringBuilder` `sb`.
  
### Records

Contextual uses records to describe behaviors. They behave like their
corresponding clojure.core names would, with any difference noted below:

- `Map`: map container which will `-invoke` every key and value with context.
- `OptionalMapWrapper`: like map, but will discard values with
  `:optional` metadata if they are nil.
- `If`: Makes branching possible. Will invoke the predicate, then either
  branch based on the result.
- `Fn`: function container. Will `-invoke` all of a function's arguments
  with context, then apply the function.
- `Path`: a generic getter for a path of keys in `ctx`. `(path :x :y)`
  will evaluate to whatever value is in path `[:x :y]` in the context
  map.
- `Or`/`And`.
- `Str`: Will invoke all its arguments and add their non-nil result to a
  string builder. Nested `Str`s won't create intermediary Strings but
  will use the same StringBuilder.
- `Let`: Works like you'd expect let to work. Lexical environment is
  implemented via attached metadata on the context and environment
  chaining.
  
### Constructors

The defined records aren't meant to be used directly, but are wrapped in
lower case constructor functions.
An underlying optimization will dispatch to a loop-unrolled record when possible.

### Compilation

Given an expression such as

```clojure
(if (path :x :y)
  (let [x (path :a :b)]
    (+ x 2))
  (str (path :y :z) "blah" (path :u :w)))
```

Compilation will produce a tree of records representing its structure after a post-walk.

```clojure
#contextual.core.If{:p #contextual.core.Path2{:k0 :x, :k1 :y}, :t #contextual.core.Let{:bindings #contextual.core.Bindings{:bindings [[x__22910 #contextual.core.Path2{:k0 :a, :k1 :b}]]}, :expr #contextual.core.Fn2{:f #function[clojure.core/+], :a0 #contextual.core.Lookup{:sym x__22910}, :a1 2}}, :e #contextual.core.Str3{:a0 #contextual.core.Path2{:k0 :y, :k1 :z}, :a1 "blah", :a2 #contextual.core.Path2{:k0 :u, :k1 :w}}}
```

#### Symbol resolution

Currently, symbols are resolved via:

- symbols-registry
- namespace resolution
- lookup in a map argument

Otherwise, a symbol will be interpreted as an environment lookup.

## Performance vs. SCI

Since contextual's model is compile-once run-many, invoke is
significantly faster than sci's eval:

```clojure
(require '[sci.core :as sci])

(def scitx (sci/init {}))

(sci/eval-form scitx '(let [x 1 y 2] (+ x y)))

(def c (-compile '(let [x 1 y 2] (+ x y))))

(-invoke c {})

(require '[criterium.core :as cc])

(cc/quick-bench
 (sci/eval-form scitx '(let [x 1 y 2] (+ x y))))

;;; Evaluation count : 20016 in 6 samples of 3336 calls.
;;;              Execution time mean : 31.412883 µs
;;;     Execution time std-deviation : 1.088819 µs
;;;    Execution time lower quantile : 30.478367 µs ( 2.5%)
;;;    Execution time upper quantile : 33.040048 µs (97.5%)
;;;                    Overhead used : 9.329803 ns

(cc/quick-bench
 (-invoke c {}))

;;; Evaluation count : 543534 in 6 samples of 90589 calls.
;;;              Execution time mean : 1.118617 µs
;;;     Execution time std-deviation : 37.443201 ns
;;;    Execution time lower quantile : 1.088897 µs ( 2.5%)
;;;    Execution time upper quantile : 1.179156 µs (97.5%)
;;;                    Overhead used : 9.414056 ns
```

In most cases, compiling + invoking contextual code will also be faster
than sci.

## Status

Experimental, in development

## TODO

- [X] Unit tests
- [ ] Tuple records
- [X] Map* records
- [X] Ensure strings work (as advertised)
- [ ] Generalize `StringBuilder` case to `Appendable`
- [ ] Check option of similarly implementing `OutputStream`. Use `Writer`?
- [X] Bring HTTP request builder up to workable condition.
- [ ] Handle different types of expressions in request better (vector, expr, etc.)
- [X] Faster walk?
- [X] More macros, (cond!)
- [X] Improve / control over resolution mechanism
- [X] Expose only safe functions by default (nothing is exposed by default)
- [ ] add namespaces
- [ ] Basic interop
- [ ] fns
- [ ] POC tagged template.
- [ ] Replace Records with types
- [ ] Have types' string representation be homoiconic.

## License

Copyright © 2020 Ben Sless

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
