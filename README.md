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

Contextual optimizes for evaluation and string generation, being a template engine.

SCI's analysis is faster than Contextual which has several optimization passes.

Besides let forms, Contextual's evaluation is faster.

### Analysis

| Expression                      | SCI                   | Contextual            |
| (+ 1 2)                         | 964.928006158732 ns   | 2.079940179533958 µs  |
| (let [x 1 y 2] (+ x y))         | 4.053300853147959 µs  | 10.428633451833264 µs |
| (let [x 1] (let [y 2] (+ x y))) | 5.014739082063033 µs  | 15.419620810055868 µs |
| (str 1)                         | 940.5541048261234 ns  | 2.462470343113364 µs  |
| (str 1 2 3)                     | 978.0085949105783 ns  | 3.3992453241441143 µs |
| (str 1 2 3 4 5 6 7 8 9 10)      | 1.1567249302535807 µs | 6.230790269538975 µs  |
| (str 1 (str 2 (str 3)))         | 2.7034542882428667 µs | 6.367253313202483 µs  |

### Evaluation of analyzed expression

| Expression                      | SCI                   | Contextual            |
| (+ 1 2)                         | 35.082971812986024 ns | 8.572977104636808 ns  |
| (let [x 1 y 2] (+ x y))         | 296.1578519129252 ns  | 239.01144451640243 ns |
| (let [x 1] (let [y 2] (+ x y))) | 448.73383802345813 ns | 503.64078619481376 ns |
| (str 1)                         | 25.416337334204073 ns | 19.849291428915592 ns |
| (str 1 2 3)                     | 117.64023555640503 ns | 45.32831079503547 ns  |
| (str 1 2 3 4 5 6 7 8 9 10)      | 409.09662405378765 ns | 124.85953596525712 ns |
| (str 1 (str 2 (str 3)))         | 187.72209701166938 ns | 50.720662555853266 ns |


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
