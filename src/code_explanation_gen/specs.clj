(ns code-explanation-gen.specs
  "Data specs for code-explanation-gen (https://clojure.org/guides/spec).
  Function specs (s/fdef) live next to each defn in code_explanation_gen.core."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

;; Generators are built inside fns, never in top-level defs:
;; clojure.spec.gen.alpha loads test.check on first use, and the JVM runtime
;; classpath (deps.edn :deps) has no test.check.

;; --- CLI option table (the babashka.cli :spec map) ---

(s/def ::desc string?)
(s/def ::default string?)
(s/def ::alias simple-keyword?)
(s/def ::coerce #{:boolean :string :long :double :keyword :symbol})
(s/def ::cli-option (s/keys :req-un [::desc] :opt-un [::default ::alias ::coerce]))
(s/def ::cli-spec (s/map-of simple-keyword? ::cli-option))

;; --- Parsed options (what -main acts on) ---

(s/def ::dir string?)
(s/def ::format string?)
(s/def ::help boolean?)
(s/def ::opts (s/keys :req-un [::dir ::format] :opt-un [::help]))

;; --- Command line ---

;; Option values a user types: paths, format names, and all-digit names
;; such as a year-named directory.
(defn- gen-value []
  (gen/one-of [(gen/elements ["." "src" "./my-project" "/tmp/work" "text" "json" "edn"])
               (gen/fmap str (gen/choose 0 3000))
               (gen/not-empty (gen/string-alphanumeric))]))

(defn- option-gens []
  {:dir    (gen/tuple (gen/elements ["--dir" "-d"]) (gen-value))
   :format (gen/tuple (gen/elements ["--format" "-f"]) (gen-value))
   :help   (gen/tuple (gen/elements ["--help" "-h"]))})

;; A well-formed argv: every value-taking flag is followed by its value.
(s/def ::argv
  (s/with-gen (s/coll-of string? :kind sequential?)
    #(gen/fmap (fn [opts] (vec (apply concat opts)))
               (gen/vector (gen/one-of (vec (vals (option-gens)))) 0 5))))

(defn gen-argv-no-repeats
  "An ::argv generator in which no option appears twice. babashka.cli drops
  the options that follow a repeated one (see the TODO(spec) on parse-args
  in specs_test)."
  []
  (let [option-gen (option-gens)]
    (gen/bind (gen/tuple (gen/shuffle (keys option-gen)) (gen/choose 0 3))
              (fn [[kinds n]]
                (if (zero? n)
                  (gen/return [])
                  (gen/fmap (fn [opts] (vec (apply concat opts)))
                            (apply gen/tuple (map option-gen (take n kinds)))))))))

(defn- flag-values [args flags]
  (->> (partition 2 1 args)
       (filter (comp flags first))
       (map second)))

(defn opts-from-args?
  "s/fdef :fn for parse-args: an option absent from argv keeps its default,
  and a given one takes one of the values supplied for it. Which repeat wins
  differs between babashka.cli on bb and on the JVM, so it isn't pinned."
  [{{:keys [args]} :args ret :ret}]
  (every? (fn [[k flags default]]
            (let [vs (flag-values args flags)]
              (if (seq vs)
                (boolean (some #{(get ret k)} vs))
                (= default (get ret k)))))
          [[:dir #{"--dir" "-d"} "."]
           [:format #{"--format" "-f"} "text"]]))
