(ns peco.core-test
  (:require [clojure.test :refer :all]
            [peco.core :as p]))

(deftest tokenizer1
  (let [tokenize    (p/tokenizer [:lower-case :concat-singles :concat-bigrams :remove-numbers :remove-alpha-numerics
                                  :remove-days :remove-stop-words :porter-stem]
                                 :concat-bigrams (p/concat-ngrams 2 #{["lazy" "dogs"]}))
        test-string "The 20 quick brown foxes jumped over the lazy dogs at 3 a.m. on Monday"
        tokens      ["quick" "brown" "fox" "jump" "lazydog"]]
    (testing "Tokenizer"
      (is (= (tokenize test-string) tokens)))))

(deftest tokenizer2
  (let [tokenize    (p/tokenizer [:lower-case :concat-singles :concat-bigrams :remove-numbers :remove-alpha-numerics
                                  :remove-days :remove-stop-words :porter-stem]
                                 :concat-bigrams (p/concat-ngrams 2 #{["lazy" "dogs"]})
                                 :tokenize (fn [text] (re-seq #"\S+" text)))
        test-string "The 20 quick brown foxes jumped over the lazy dogs at 3 a.m. on Monday"
        tokens      ["quick" "brown" "fox" "jump" "lazydog" "a.m."]]
    (testing "Tokenizer"
      (is (= (tokenize test-string) tokens)))))