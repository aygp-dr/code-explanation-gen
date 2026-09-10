(ns code_explanation_gen.core
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [cheshire.core :as json]
            [code-explanation-gen.specs :as specs]))

(def cli-spec
  {:dir {:desc "Directory to scan" :default "." :alias :d :coerce :string}
   :format {:desc "Output format: text, json, edn" :default "text" :alias :f :coerce :string}
   :help {:desc "Show help" :alias :h :coerce :boolean}})

(defn parse-args
  "Parse command-line args into an options map (pure)."
  [args]
  (cli/parse-opts args {:spec cli-spec}))

(s/fdef parse-args
  :args (s/cat :args (s/nilable ::specs/argv))
  :ret ::specs/opts
  :fn specs/opts-from-args?)

(defn banner
  "The one-line status message -main prints for parsed opts (pure)."
  [{:keys [dir format]}]
  (clojure.core/format "code-explanation-gen: scanning %s (format: %s)" dir format))

(s/fdef banner
  :args (s/cat :opts ::specs/opts)
  :ret string?
  :fn (fn [{{:keys [opts]} :args ret :ret}]
        (and (str/includes? ret (:dir opts))
             (str/includes? ret (:format opts)))))

(defn -main [& args]
  (let [opts (parse-args args)]
    (when (:help opts)
      (println "code-explanation-gen — Generate line-by-line code explanations")
      (println)
      (println (cli/format-opts {:spec cli-spec}))
      (System/exit 0))
    ;; TODO: implement scanning logic
    (println (banner opts))
    (println "Not yet implemented — see CLAUDE.md for build order")))

(s/fdef -main
  :args (s/* string?))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
