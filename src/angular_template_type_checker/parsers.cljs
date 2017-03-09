(ns angular-template-type-checker.parsers
  (:require [instaparse.core :as insta]))

(def angular-expression-ebnf
  "<variable> = symbol | string | array | map
   <symbol> = #'[\\w\\$]+'
   <string> = #'\\'[^\\']*\\'' | #'\\\"[^\\\"]*\\\"'
   <array> = '[' ((' '* variable ' '*) (',' ' '* variable ' '*)*)? ']' 
   <map> = '{' (kvp (',' ' '* kvp)*)? '}'
   <kvp> = (variable | string) ' '* ':' ' '* variable
   <tuple> = <'(' ' '*> variable <',' ' '*> variable <' '* ')'>
   expr =  operation | <'(' ' '*> operation <' '* ')'>
   <chain> = (variable | function) ('.' chain)?
   <operator> = '+' | '-' | '*' | '/' | '%' | '=' | '==' | '===' | '||' | '&&'
   <op-chain> = chain ' '* operator ' '* chain
   <operation> = chain | op-chain
   <function> = variable '(' ' '* function-args ' '* ')'
   <function-args> = operation? (<' '* ',' ' '*> operation)*  
   filter = <' '* '|' ' '* symbol ' '* ':' ' '*> expr
   <template-expr> = expr (filter | expr)*")


(def template-expression-parser
  (insta/parser (str "bindings = (<#'.'*> binding <#'.'*>)+ 
                      binding = <' '* '{{' ' '*> template-expr <' '* '}}' ' '*>;"
                     angular-expression-ebnf)))


(def ng-repeat-parser
  (insta/parser (str "ng-repeat = binding-symbols <' '+ 'in' ' '+> binding-expr (track-by | alias)?
                     track-by = <' '+ 'track by' ' '+> expr
                     alias = <' '+ 'as' ' '*> expr
                     binding-symbols = (variable | tuple);
                     binding-expr = template-expr"
                     angular-expression-ebnf)))

(def single-expression-parser
  (insta/parser (str "ng-attr = template-expr;"
                     angular-expression-ebnf)))
