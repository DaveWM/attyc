(ns angular-template-type-checker.logging)

(def log-levels [:debug :info :warning :error])

(def ^:dynamic *log-level* :debug)

(defn log [level message]
  (when (>= (.indexOf log-levels level)
            (.indexOf log-levels *log-level*))
    (println message)))

(def debug (partial log :debug))
(def info (partial log :info))
(def warning (partial log :warning))
(def error (comp (partial log :error)
                 (partial str "ERROR: ")))
