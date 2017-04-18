# Peco

A Clojure library for text tokenization, transformation and normalisation. Transformations are specified as a vector of
operations that are implemented as transducers and performed in the specified order.


## Usage

Available built-in operations are:
           
:lower-case            
:porter-stem           
:lancaster-stem        
:lovins-stem           
:remove-numbers        
:remove-alpha-numerics       
:remove-dimensions         
:remove-stop-words     
:remove-days           
:remove-months         
:remove-first-names    
:remove-duplicates     
:concat-singles        
:remove-singles
        
A basic tokenizer is created like this: 

```` clojure
(require '[peco.core :refer [tokenizer]])

(def tokenize (tokenizer [:lower-case :remove-numbers :porter-stem]))

(tokenize "The quick brown foxes jumped over the 20 lazy dogs at 3 a.m. on Monday")

=> ["the" "quick" "brown" "fox" "jump" "over" "the" "lazi" "dog" "at" "a" "m" "on" "mondai"]

````

You can also specify your own operations or override a built-in implementation:

```` clojure
(require '[peco.core :refer [tokenizer concat-ngrams]])

(def tokenize
  (tokenizer [:lower-case :concat-bigrams]
             :concat-bigrams (concat-ngrams 2 #{["can" "not"] ["log" "in"] ["user" "name"]})))

(tokenize "I can not log in because the system does not recognise my user name")

=> ["i" "cannot" "login" "because" "the" "system" "does" "not" "recognise" "my" "username"]
````

concat-ngrams creates a transducer and takes n, the size of the ngrams to be matched, and a function f which is called
for each sequence of n tokens. If f returns a truthy value for the sequence, it will be concatenated. Using a set
of vectors, as above, is a convenient way of expressing this function.

Concatenating ngrams, as above, can be useful for text normalisation. Concatenating consecutive single characters is 
another useful normalisation operation:

```` clojure
(require '[peco.core :refer [tokenizer concat-ngrams]])

(def tokenize
  (tokenizer [:lower-case :concat-singles]))

(tokenize "ABC widgets manufacture the AP6 robot")
(tokenize "A. B. C. widgets manufacture the A P 6 robot")

=> ["abc" "widgets" "manufacture" "the" "ap6" "robot"]
=> ["abc" "widgets" "manufacture" "the" "ap6" "robot"]
````

The default tokenizer uses (re-seq #"\w+" text) but you may specify your own as an operation:

```` clojure
(require '[peco.core :refer [tokenizer]])

(def tokenize
  (tokenizer [:lower-case]
              :tokenize (fn [text] (re-seq #"\S+" text))))

(tokenize "A.B.C widgets manufacture the AP6 robot")

=> ["a.b.c" "widgets" "manufacture" "the" "ap6" "robot"]

````

## Licence

Copyright © 2017 David Marley

Distributed under the Eclipse Public License, the same as Clojure.
