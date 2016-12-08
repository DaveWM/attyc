(ns angular-template-type-checker.specs
  (:require [clojure.spec :as s])
  #?(:cljs (:require-macros [angular-template-type-checker.specs :refer [single]])))

#?(:clj (defmacro single [predicate]
          `(s/cat :single/before  (s/* (complement ~predicate))
                  :single/value   (s/? ~predicate)
                  :single/after   (s/* (complement ~predicate)))))

(s/def :metadata/name string?)
(s/def :metadata/type string?)
(s/def :metadata/import string?)
(s/def :metadata/variable (s/keys
                           :req-un [:metadata/name :metadata/type]
                           :opt-un [:metadata/import]))
(def metadata-spec (s/+ :metadata/variable))

(s/def :cla/name string?)
(s/def :cla/alias string?)
(s/def :cla/multiple boolean?)
(s/def :cla/defaultOption boolean?)
(s/def :cla/defaultValue (s/or :multiple vector?
                               :single (complement nil?)))
(s/def :clu/description string?)
(s/def :clu/typeLabel string?)
(s/def :cla/option (s/keys :req-un [:cla/name :cla/alias :clu/description]
                           :opt-un [:cla/multiple :cla/defaultOption :cla/defaultValue :clu/typeLabel]))
(def optionsListSpec (s/and (s/+ :cla/option)
                            (single #(:defaultOption %))))
(s/def :cla/optionList optionsListSpec)

(s/def :clu/header string?)
(s/def :clu/content string?)
(s/def :clu/section (s/keys :req-un [:clu/header (or :clu/content :cla/optionList)]))
(s/def :clu/all (s/+ :clu/section))
