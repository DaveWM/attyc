(ns angular-template-type-checker.parsers
  (:require [instaparse.core :as insta :refer-macros [defparser]])
  (:require-macros [angular-template-type-checker.macros :refer [with-ng-parsers]]))

(defparser template-expression-parser
  (with-ng-parsers "bindings = (<#'.'*> binding <#'.'*>)+ 
                    binding = <' '* '{{' ' '*> template-expr <' '* '}}' ' '*>"))


(defparser ng-repeat-parser
  (with-ng-parsers "binding-expr = <' '*> binding-symbols <' '+ 'in' ' '+> binding-value (track-by | alias)? <' '*>
                    track-by = <' '+ 'track by' ' '+> expr
                    alias = <' '+ 'as' ' '*> expr"))

(defparser ng-options-parser
  (with-ng-parsers "binding-expr = <' '*> ((select? <' '*> label) | (label <' '*> (group | disable))) <' '* 'for' ' '*> binding-symbols <' '* 'in' ' '*> binding-value track-by?
                    label = expr
                    select = expr <' '* 'as' ' '*>
                    group = <'group' ' '* 'by' ' '*> expr
                    disable = <'disable' ' '* 'when' ' '*> expr
                    track-by = <' '+ 'track by' ' '+> expr"))

(defparser single-expression-parser
  (with-ng-parsers "ng-expr = <' '*> template-expr <' '*>"))
