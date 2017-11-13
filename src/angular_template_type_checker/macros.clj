(ns angular-template-type-checker.macros
  (:require [clojure.spec :as s]
            [angular-template-type-checker.specs :as specs :refer [optionsListSpec]]))

(defmacro def-cli-opts [var-name options]
  (if-let [explanation (s/explain-data optionsListSpec options)]
    (throw (Exception. (str "Invalid command line arguments: " explanation)))
    `(def ~var-name ~options)))

(defmacro with-ng-parsers [parser-str]
  (str parser-str ";"
       "<variable> = symbol | string | array | map
        <symbol> = #'[\\w\\$]+'
        <string> = #'\\'[^\\']*\\'' | #'\\\"[^\\\"]*\\\"'
        <array> = '[' ((' '* expr ' '*) (',' ' '* expr ' '*)*)? ']' 
        <map> = '{' <' '*> (kvp <' '*> (',' <' '*> kvp)*)? <' '*> '}'
        <kvp> = ((symbol | string) ' '* ':' ' '* expr) | symbol
        expr =  operation | <'(' ' '*> operation <' '* ')'>
        <operator> = '+' | '-' | '*' | '/' | '%' | '=' | '==' | '===' | '||' | '&&' | '>' | '>=' | '<' | '<='
        <prefix-operator> = '+' | '-' | '!'
        <chain> = (variable | function) ('.' chain)?
        <prefixed-chain> = prefix-operator* chain
        <operation> = prefixed-chain (' '* operator ' '* operation)? 
        <function> = variable '(' ' '* function-args ' '* ')'
        <function-args> = operation? (<' '*> ',' <' '*> operation)* 
        filter = <' '* '|' ' '* symbol ' '*> (<':' ' '*> expr)?
        <template-expr> = expr (filter)*
        binding-symbols = (symbol | tuple);
        binding-value = template-expr
        <tuple> = <'(' ' '*> symbol <',' ' '*> symbol <' '* ')'>"))

(defmacro with-log-level
  "Runs the given *synchronous* code with the specified logging level. It *will not* work with promises/callbacks, see https://groups.google.com/forum/#!topic/clojure/6cmnkHmHBNw"
  [level & body]
  `(with-redefs [angular-template-type-checker.logging/*log-level* ~level]
     ~@body))
