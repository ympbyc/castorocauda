(ns catest.timeline
  (:use [castorocauda.timeline :only [->timeline timeline tl-deref tl-cons! sync-tl
                                      tl-map tl-merge tl-filter tl-apply tl-lift tl-of-take]]
        [castorocauda.util :only [q-select dom-element-events]]
        [catest.test :only [prn= de ok prn-log]]
        [cljs.core.async :only [>! <! timeout chan]])
  (:use-macros [catest.test-macro :only [qtest async-test go-resume]]
               [cljs.core.async.macros :only [go]]))


(js/module "castorocauda.timeline")

(qtest
 "(->timeline seq)"
 (prn= (tl-deref (->timeline '(1 2 3))) '(1 2 3) "converts seq into timeline")

 (prn= (tl-deref (->timeline [1 2 3])) '(1 2 3) "vector is also allowed")

 (prn= (:size-limit (->timeline [1 2 3] :size 5)) 5 "size limit can be set")

 (prn= (:resetable (->timeline [1 2 3])) true "resetablity defaults to false")

 (prn= (:resetable (->timeline [1 2 3] :resetable false)) false "resetablity can be set")

 (let [{:keys [stream size-limit resetable count]} (->timeline [1 2 3] :size 8 :resetable false)]
   (prn= @stream '(1 2 3) "setting everything :: stream")
   (prn= size-limit 8 "setting everything :: size")
   (prn= resetable false "setting everything :: resetable")
   (prn= @count 3 "setting everything :: count")))

(qtest
 "(timeline & xs)"

 (prn= (seq (tl-deref (timeline))) nil "(timeline) creates empty timeline")

 (prn= (tl-deref (timeline 1 2 3)) '(1 2 3) "(timeline & xs) creates timeline of xs")

 (prn= (tl-deref (timeline [1 2 3])) '([1 2 3]) "(timeline [& xs]) is no exception")

 (prn= (:size-limit (timeline 1 2 3)) 100 "default limit size"))


(qtest
 "(tl-cons! x tl)"

 (prn= (->> (timeline) (tl-cons! 1) tl-deref) '(1) "cons one item onto an empty tl")

 (prn= (->> (timeline 1 2) (tl-cons! 3) tl-deref) '(3 1 2) "item inserted at tail position")

 (prn= (->> (timeline 1)
            (tl-cons! "hello")
            (tl-cons! 3)
            (tl-cons! "www")
            tl-deref)
       '("www" 3 "hello" 1) "repetitive application")

 (let [tl (timeline)]
   (tl-cons! "c" tl)
   (tl-cons! "b" tl)
   (tl-cons! "a" tl)
   (prn= (tl-deref tl)  '("a" "b" "c") "tl-cons! is a destructive operation")))


(async-test
 "tl-cons! adjustment 1" 3
 (let [tl (->timeline '(1 2 3 4) :size 4)]
   (tl-cons! 8 tl)
   (prn= (->> tl :count deref) 5 "counter is increased")
   (go-resume
    (<! (timeout 0))
    (prn= (tl-deref tl) '(8)
          "stream is shrinked leaving only one element")
    (prn= (->> tl :count deref) 1
          "counter is set to one"))))

(async-test
 "tl-cons! asjustment 2" 3
 (let [tl (->timeline '(1 2 3 4) :size 4 :resetable false)]
   (tl-cons! 8 tl)
   (prn= (->> tl :count deref) 5 "counter is increased")
   (go-resume
    (<! (timeout 0))
    (prn= (tl-deref tl) '(8 1 2 3) "stream's tail is cut to maintain the size")
    (prn= (->> tl :count deref) 4
          "counter is in sync with the size of the stream"))))


(async-test
 "sync-tl :: basic" 3
 (let [{:keys [size-limit] :as tl1} (timeline)
       tl2 (timeline)]
   (prn= (sync-tl first tl1 tl2) nil "sync-tl yields nil")
   (go-resume
    (tl-cons! 2 tl1)
    (tl-cons! "hello" tl1)
    (<! (timeout 0))
    (prn= (tl-deref tl1) '("hello" 2) "tl1 is modified")
    (prn= (tl-deref tl2) '("hello" 2) "Change to tl1 is synchronized to tl2"))))



(async-test
 "sync-tl :: providing predicate fn" 3
 (let [tl1 (timeline)
       tl2 (timeline)]
   (sync-tl first tl1 tl2 even?)
   (->> tl1 (tl-cons! 1) (tl-cons! 2) (tl-cons! 3) (tl-cons! 4))
   (prn= (tl-deref tl1) '(4 3 2 1) "tl1 has all four items before sync")
   (go-resume
    (<! (timeout 0))
    (prn= (tl-deref tl1) '(4 3 2 1) "tl1 has all four items after sync")
    (prn= (tl-deref tl2) '(4 2) "tl2 is synchronized and filtered"))))


(async-test
 "sync-tl :: providing mapper fn" 2
  (let [tl1 (timeline)
        tl2 (timeline)]
    (sync-tl (comp inc first) tl1 tl2)
    (->> tl1 (tl-cons! 1) (tl-cons! 2) (tl-cons! 3) (tl-cons! 4))
    (go-resume
     (<! (timeout 0))
     (prn= (tl-deref tl1) '(4 3 2 1) "tl1 has all four items unmodified")
     (prn= (tl-deref tl2) '(5 4 3 2) "tl2 is synchronized and f is applied to every item"))))


(async-test
 "sync-tl :: providing both predicate and mapper" 2
 (let [tl1 (timeline)
       tl2 (timeline)]
   (sync-tl (comp inc first) tl1 tl2 odd?)
   (->> tl1 (tl-cons! 1) (tl-cons! 2) (tl-cons! 3) (tl-cons! 4))
   (go-resume
    (<! (timeout 0))
    (prn= (tl-deref tl1) '(4 3 2 1) "tl1 has all four items unmodified")
    (prn= (tl-deref tl2) '(4 2)
          "tl2 is synchronized, filtered and f is applied to every item"))))


(async-test
 "tl-map" 2
 (let [tl1 (timeline 1 2 3 4 5)
       tl2 (tl-map odd? tl1)]
   (prn= (tl-deref tl2) '(true false true false true)
         "tl-map returns a timeline. fn is applied to every item.")
   (tl-cons! 2 tl1)
   (tl-cons! 9 tl1)
   (go-resume
    (<! (timeout 0))
    (prn= (tl-deref tl2) '(true false true false true false true)
          "synchronized and still fn is applied"))))


(async-test
 "tl-merge :: merge with associative function +" 3
 (let [tl1 (timeline 1 2 3 4)
       tl2 (timeline 4 3 2 1)
       tl3 (tl-merge + tl1 tl2)]
   (prn= (tl-deref tl3) '(5 5 5 5)
         "tl-merge returns a timeline. items are merged using the provided fn")
   (tl-cons! 2 tl1)
   (go-resume
    (<! (timeout 0))
    (prn= (tl-deref tl3) '(6 5 5 5 5)
          "new item is added. merge fn use other tl's first val - 1")
    (tl-cons! 9 tl1)
    (<! (timeout 0))
    (prn= (tl-deref tl3) '(13 6 5 5 5 5)
          "new item is added. merge fn use other tl's first val - 2"))))


(async-test
 "tl-merge :: merge with non-associative function vec" 3
  (let [tl1 (timeline 1 2 3 4)
       tl2 (timeline 5 6 7 8)
       tl3 (timeline 9 0 1 2)
       tl4 (tl-merge (fn [& xs] (vec xs)) tl1 tl2 tl3)]
   (prn= (tl-deref tl4) '([1 5 9] [2 6 0] [3 7 1] [4 8 2])
         "tl-merge returns a timeline. items are merged using the provided fn")
   (go-resume
    (tl-cons! 2 tl1)
    (<! (timeout 0))
    (prn= (tl-deref tl4) '([2 5 9] [1 5 9] [2 6 0] [3 7 1] [4 8 2])
          "new item is added. merge fn use other tl's first val")
    (tl-cons! 9 tl2)
    (<! (timeout 0))
    (prn= (tl-deref tl4) '([2 9 9] [2 5 9] [1 5 9] [2 6 0] [3 7 1] [4 8 2])
          "new item is added. merge fn use other tl's first val"))))



(async-test
 "tl-filter" 3
 (let [tl1 (timeline 1 2 3 4 5 6 7 8)
       tl2 (tl-filter even? tl1)]
   (prn= (tl-deref tl2) '(2 4 6 8)
         "tl-filter returns a timeline. items are filtered using the provided fn")
   (go-resume
    (tl-cons! 2 tl1)
    (<! (timeout 0))
    (prn= (tl-deref tl2) '(2 2 4 6 8)
          "new item gets added if it passes filter fn")
    (tl-cons! 9 tl1)
    (<! (timeout 0))
    (prn= (tl-deref tl2) '(2 2 4 6 8)
          "don't change if the value fail filter fn"))))


(defn test-lift-take
  "Run the same set of tests with entangled timelines created in various ways."
  [tl1 tl2]
  (prn= (->> tl2 tl-deref) '((5 4 3))
        "tl-take returns a timeline with `take` applied on its stream")
  (go-resume
   (tl-cons! 6 tl1)
   (<! (timeout 0))
   (prn= (->> tl2 tl-deref first) '(6 5 4) "`take` gets applied to new stream in tl")
   (tl-cons! 7 tl1)
   (tl-cons! 8 tl1)
   (<! (timeout 0))
   (prn= (->> tl2 tl-deref first) '(8 7 6) "`take` gets applied to new stream in tl")))


(async-test
 "tl-apply" 3
 (let [tl1 (timeline 5 4 3 2 1)
       tl2 (tl-apply (partial take 3) tl1)]
   (test-lift-take tl1 tl2)))

(async-test
 "tl-lift" 3
 (let [f (tl-lift take)
       tl1 (timeline 5 4 3 2 1)
       tl2 (f 3 tl1)]
   (test-lift-take tl1 tl2)))

(async-test
 "lifted functions" 3
 (let [tl1 (timeline 5 4 3 2 1)
       tl2 (tl-of-take 3 tl1)]
   (test-lift-take tl1 tl2)))




(async-test
 "dom-events" 1

 (let [tl1
       (tl-map #(.-innerHTML (.-target %))
               (dom-element-events "click" (q-select "#click-test")))]
   (.click (q-select "#click-test"))
   (.click (q-select "#click-test"))
   (.click (q-select "#click-test"))
   (go-resume
    (<! (timeout 0))
    (prn= (tl-deref tl1) '("fmm" "fmm" "fmm")
          "DOM events are collected and fn is applied"))))
