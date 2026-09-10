(ns code-explanation-gen.core-test
  (:require [clojure.spec.test.alpha :as stest]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [code_explanation_gen.core :as sut]))

;; Exercise every s/fdef :args spec while the unit tests run.
(use-fixtures :once
  (fn [f] (stest/instrument) (try (f) (finally (stest/unstrument)))))

(deftest cli-spec-shape
  (testing "every option documents itself"
    (is (every? (comp string? :desc) (vals sut/cli-spec))))
  (testing "defaults"
    (is (= "." (get-in sut/cli-spec [:dir :default])))
    (is (= "text" (get-in sut/cli-spec [:format :default]))))
  (testing "--help is a boolean flag"
    (is (= :boolean (get-in sut/cli-spec [:help :coerce])))))

(deftest parse-args-defaults
  (is (= {:dir "." :format "text"} (sut/parse-args []))))

(deftest parse-args-long-options
  (is (= {:dir "src" :format "json"}
         (sut/parse-args ["--dir" "src" "--format" "json"]))))

(deftest parse-args-aliases
  (is (= {:dir "lib" :format "edn"}
         (sut/parse-args ["-d" "lib" "-f" "edn"]))))

(deftest parse-args-help
  (is (true? (:help (sut/parse-args ["--help"]))))
  (is (true? (:help (sut/parse-args ["-h"]))))
  (is (nil? (:help (sut/parse-args ["--dir" "src"])))))

(deftest banner-names-dir-and-format
  (is (= "code-explanation-gen: scanning ./my-project (format: json)"
         (sut/banner {:dir "./my-project" :format "json"}))))

(deftest main-reports-dir-and-format
  (let [out (with-out-str (sut/-main "--dir" "src" "--format" "json"))]
    (is (str/includes? out "code-explanation-gen: scanning src (format: json)"))
    (is (str/includes? out "Not yet implemented"))))

(deftest main-uses-defaults
  (is (str/includes? (with-out-str (sut/-main))
                     "scanning . (format: text)")))
