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

## Usage

TODO

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

## Status

Experimental, in development

## TODO

- [X] Unit tests
- [ ] Map* and Tuple records
- [ ] Ensure strings work (as advertised)
- [ ] Generalize `StringBuilder` case to `Appendable`
- [ ] Check option of similarly implementing `OutputStream`. Use `Writer`?
- [ ] Bring HTTP request builder up to workable condition.
- [ ] POC tagged template.

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
