(ns clojure-refactoring.core
  (:use [clojure.contrib str-utils pprint])
  (:use [clojure.contrib.seq-utils :only (find-first)])
  (:use clojure.walk)
  (:import clojure.lang.Named))

(defn is-defn? [node]
  "Returns true if the current node is a function definition"
  (= (first node) 'defn))

(defn format-code [node]
  "Outputs code roughly how a human would format it."
  (with-out-str
    (with-pprint-dispatch *code-dispatch*
      (pprint node))))

(defn find-occurences [args node]
  "Looks for any occurence of each element of args in the node
TODO: doesn't handle destructuring properly"
  (flatten (loop [arg-set (set args) node node]
             (for [sub-node node]
               (if (seq? sub-node)
                 (or (find-occurences arg-set sub-node))
                 (arg-set sub-node))))))

(defn sub-nodes [tree]
  (tree-seq sequential? seq tree))

(def binding-forms
     #{'let 'fn 'binding 'for 'doseq 'dotimes 'defn 'loop})

(defn binding-node? [node]
  "Checks if a node is a binding node"
  (binding-forms (first node)))

(defn evens [coll]
  "Returns every other item of coll"
  (take-nth 2 coll))

(defn fn-args [node]
  "Returns the function arguments from a top-level defn node"
  (find-first #(vector? %) node))

(defn extract-binding-form [node]
  "Returns a vector of bindings iff the node is a binding node. Won't work with multiple arity defns"
  (if (binding-node? node)
    (fn-args node)))

(defn unique-vec [coll]
  "Strips all duplicates from coll and forces it into a vector"
  (vec (apply sorted-set coll)))

(defn bound-symbols [node]
  "Returns a vector of the bound symbols inside node"
  (if (is-defn? node)
    (extract-binding-form node)
    (evens (extract-binding-form node))))

(defn some-true? [coll]
  "Returns true if anything in the collection is true"
  (some #{true} coll))

(defn rec-matches? [f coll]
  "True if the result of applying f on any sub-element of coll is true"
  (filter f (sub-nodes coll)))

(defn rec-contains? [coll obj]
  "True if coll contains obj at some level of nesting"
  (some #{obj} (sub-nodes coll)))

(defn last-binding-form? [node]
  "Returns true if there are no binding nodes inside node"
  (and (binding-node? node)
       (not
        (some identity
              (for [sym binding-forms]
                (rec-contains? (rest node) sym))))))

(defn add-binding-form [node bnd-syms]
  "Returns a new binding form from the root node's binding form"
  (into bnd-syms (bound-symbols node)))

(defn contains-binding-nodes? [node]
  (some #{true} (map #(rec-contains? node %) binding-forms)))

(defn more-than-one [pred coll]
  "True if more than one item of coll matches pred"
  (if (seq? coll)
    (< 1 (count (filter pred coll)))))
