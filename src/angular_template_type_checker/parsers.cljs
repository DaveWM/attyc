(ns angular-template-type-checker.parsers
  (:require [instaparse.core :as insta]))

(def angular-expression-ebnf
  "<variable> = symbol | string | array | map
   <symbol> = #'[\\w\\$]+'
   <string> = #'\\'[^\\']*\\'' | #'\\\"[^\\\"]*\\\"'
   <array> = '[' ((' '* variable ' '*) (',' ' '* variable ' '*)*)? ']' 
   <map> = '{' (kvp (',' ' '* kvp)*)? '}'
   <kvp> = (variable | string) ' '* ':' ' '* variable
   expr =  operation | <'(' ' '*> operation <' '* ')'>
   <operator> = '+' | '-' | '*' | '/' | '%' | '=' | '==' | '===' | '||' | '&&' | '>' | '>=' | '<' | '<='
   <prefix-operator> = '+' | '-' | '!'
   <chain> = (variable | function) ('.' chain)?
   <prefixed-chain> = prefix-operator* chain
   <operation> = prefixed-chain (' '* operator ' '* prefixed-chain ' '*)*
   <function> = variable '(' ' '* function-args ' '* ')'
   <function-args> = operation? (<' '* ',' ' '*> operation)*  
   filter = <' '* '|' ' '* symbol ' '*> (<':' ' '*> expr)?
   <template-expr> = expr (filter)*
   binding-symbols = (symbol | tuple);
   binding-value = template-expr
   <tuple> = <'(' ' '*> symbol <',' ' '*> symbol <' '* ')'>")


(def template-expression-parser
  (insta/parser (str "bindings = (<#'.'*> binding <#'.'*>)+ 
                      binding = <' '* '{{' ' '*> template-expr <' '* '}}' ' '*>;"
                     angular-expression-ebnf)))


(def ng-repeat-parser
  (insta/parser (str "binding-expr = <' '*> binding-symbols <' '+ 'in' ' '+> binding-value (track-by | alias)? <' '*>
                     track-by = <' '+ 'track by' ' '+> expr
                     alias = <' '+ 'as' ' '*> expr"
                     angular-expression-ebnf)))

(def ng-options-parser
  (insta/parser (str "binding-expr = <' '*> ((select? <' '*> label) | (label <' '*> (group | disable))) <' '* 'for' ' '*> binding-symbols <' '* 'in' ' '*> binding-value track-by?
                      label = expr
                      select = expr <' '* 'as' ' '*>
                      group = <'group' ' '* 'by' ' '*> expr
                      disable = <'disable' ' '* 'when' ' '*> expr
                      track-by = <' '+ 'track by' ' '+> expr"
                     angular-expression-ebnf)))

(def single-expression-parser
  (insta/parser (str "ng-expr = <' '*> template-expr <' '*>;"
                     angular-expression-ebnf)))
