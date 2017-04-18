(ns peco.core
  (:require [clojure.string :refer [split-lines join]]
            [clojure.set :refer [difference]]
            [clojure.java.io :as io]
            [clj-fuzzy.stemmers :refer [porter lancaster lovins]])
  (:import (java.io FileNotFoundException)))

(def operation-table
  {:tokenize              'peco.core/tokenize
   :lower-case            '(map clojure.string/lower-case)
   :porter-stem           '(map peco.core/porter-stem)
   :lancaster-stem        '(map peco.core/lancaster-stem)
   :lovins-stem           '(map peco.core/lovins-stem)
   :remove-numbers        '(remove peco.core/numeric-string?)
   :remove-alpha-numerics '(remove peco.core/alpha-numeric-string?)
   :remove-dimensions     '(remove peco.core/dimension-string?)
   :remove-stop-words     '(remove (set (peco.core/slurp-words "text/common-stop-words.txt")))
   :remove-days           '(remove (set (peco.core/slurp-words "text/days.txt")))
   :remove-months         '(remove (set (peco.core/slurp-words "text/months.txt")))
   :remove-first-names    '(remove (set (peco.core/slurp-words "text/first-names.txt")))
   :remove-duplicates     '(distinct)
   :concat-singles        '(peco.core/concat-singles)
   :remove-singles        '(remove #(= (count %) 1))})

(defn numeric-string?
  [s]
  (boolean (re-matches #"\d+" s)))

(defn alpha-numeric-string?
  "Matches a string that is a combination of digits and letters. Useful for matching many part numbers and strings
  like 3am etc."
  [s]
  (boolean (re-matches #"(\d+[a-zA-Z]+(\d|[a-zA-Z])*|[a-zA-Z]+\d+(\d|[a-zA-Z])*)" s)))

(defn dimension-string?
  "Matches a string that looks like a dimension e.g. 3cm. The list of unit suffixes can defintely be improved upon."
  [s]
  (boolean (re-matches #"(^|\d+)(mm|cm|m|ml|cl|l)" s)))

(defn porter-stem
  [word]
  (porter word))

(defn lancaster-stem
  [word]
  (lancaster word))

(defn lovins-stem
  [word]
  (lovins word))

(defn tokenize
  [text]
  (re-seq #"\w+" text))

(defn concat-singles
  "Concatenates consecutive single characters. Useful for normalising names etc.
  For example, after tokenization, [A D B Widgets] | [ADB Widgets] -> [ADB Widgets].
  Returns a transducer if no collection of tokens is provided."
  ([]
   (fn [rf]
     (let [singles (volatile! nil)]
       (fn
         ([] (rf))
         ([result]
          (if (nil? @singles)
            (rf result)
            (rf (rf result @singles))))
         ([result next]
          (if (= (count next) 1)
            (do
              (vswap! singles #(str % next))
              result)
            (if (nil? @singles)
              (rf result next)
              (let [s @singles]
                (vreset! singles nil)
                (rf (rf result s) next)))))))))
  ([coll]
   (transduce (concat-singles) conj coll)))

(defn concat-ngrams
  "Repeatedley takes a sequence of length n from terms and passes it to pred. If pred returns a truthy value, the
  sequence is concatenated. Useful for normalising n-grams e.g. 'user name' -> 'username'. Returns a transducer if
  no collection of tokens is supplied."
  ([n pred]
   (assert (> n 0))
   (fn [rf]
     (let [unigrams        (volatile! [])
           flush-unigrams! (fn [result unigram next-unigrams]
                             (vreset! unigrams next-unigrams)
                             (rf result unigram))]
       (fn
         ([] (rf))
         ([result]
          (if (empty? @unigrams)
            (rf result)
            (if (pred @unigrams)
              (rf (rf result (apply str @unigrams)))
              ; Flush the remaining unigrams
              (loop [us @unigrams results result]
                (if (empty? us)
                  (rf results)
                  (recur (rest us) (rf results (first us))))))))
         ([result next]
          (if (< (count @unigrams) n)
            (do
              (vswap! unigrams #(conj % next))
              result)
            (if (pred @unigrams)
              (flush-unigrams! result (apply str @unigrams) [next])
              (flush-unigrams! result (first @unigrams) (conj (subvec @unigrams 1) next)))))))))
  ([n pred coll]
   (transduce (concat-ngrams n pred) conj coll)))

(defn slurp-from-classpath
  "Slurps a file from the classpath. Lifted from https://github.com/krisajenkins/yesql"
  [path]
  (or (some-> path
              io/resource
              slurp)
      (throw (FileNotFoundException. path))))

(defn slurp-words
  ([file-path tokenizer]
   (->> file-path
        (slurp-from-classpath)
        (tokenizer)))
  ([file-path]
   (slurp-words file-path tokenize)))

(defn token-pipeline
  [operation-table operations]
  (let [invalid-ops (difference (set operations) (set (keys operation-table)))]
    (when (not-empty invalid-ops)
      (throw (ex-info "Invalid operations specified" {:available-operations (set (keys operation-table))
                                                      :invalid-operations   invalid-ops}))))
  (->> operations
       (remove :tokenize)
       (map operation-table)
       (map eval)
       (apply comp)))

(defn tokenizer-fn
  [operation-table operations]
  (let [tokenize (eval (:tokenize operation-table))
        pipeline (token-pipeline operation-table operations)]
    (fn [text]
      (->> (tokenize text)
           (transduce pipeline conj)))))

(defn tokenizer
  ([]
   tokenize)
  ([operations]
   (tokenizer-fn operation-table operations))
  ([operations & ud-operations]
   (-> (apply assoc operation-table ud-operations)
       (tokenizer-fn operations))))



