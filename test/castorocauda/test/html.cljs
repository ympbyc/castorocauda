(ns test.html
  (:use [castorocauda.html :only [pad attr-diffs]]
        [test.test :only [se de]])
  (:use-macros [test.test-macro :only [qtest]]))


(qtest "pad"
       (de (pad [1 2 3] 5 :x) [1 2 3 :x :x] "n greater than length")
       (de (pad [1 2 3] 2 :x) [1 2 3] "ignore if n is less tha length")
       (de (pad [] 5 nil) [nil nil nil nil nil] "empty coll and nil padding")
       (de (pad [1 2 3] -2 :x) [1 2 3] "negative n"))


(qtest "attr-diffs"
       (de (attr-diffs {:text "hello" :arr [123]} {:text "rrr" :arr [2 3 4]} [:x])
           [[:att [:x] :text "rrr"]
            [:att [:x] :arr [2 3 4]]]
           ""))
