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
        <kvp> = (variable | string) ' '* ':' ' '* expr
        expr =  operation | <'(' ' '*> operation <' '* ')'>
        <operator> = '+' | '-' | '*' | '/' | '%' | '=' | '==' | '===' | '||' | '&&' | '>' | '>=' | '<' | '<='
        <prefix-operator> = '+' | '-' | '!'
        <chain> = (variable | function) ('.' chain)?
        <op-chain> = prefixed-chain ' '* operator ' '* prefixed-chain
        <prefixed-chain> = prefix-operator* chain
        <operation> = prefixed-chain | op-chain
        <function> = variable '(' ' '* function-args ' '* ')'
        <function-args> = operation? (<' '*> ',' <' '*> operation)* 
        filter = <' '* '|' ' '* symbol ' '*> (<':' ' '*> expr)?
        <template-expr> = expr (filter)*
        binding-symbols = (symbol | tuple);
        binding-value = template-expr
        <tuple> = <'(' ' '*> symbol <',' ' '*> symbol <' '* ')'>"))
