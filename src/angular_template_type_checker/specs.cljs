(ns angular-template-type-checker.specs
  (:require [cljs.spec :as s]))

(s/def :metadata/name string?)
(s/def :metadata/type string?)
(s/def :metadata/import string?)
(s/def :metadata/variable (s/keys
                           :req-un [:metadata/name :metadata/type]
                           :opt-un [:metadata/import]))
(def metadata-spec (s/+ :metadata/variable))
