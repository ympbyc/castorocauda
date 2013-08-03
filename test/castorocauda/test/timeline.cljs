(ns test.timeline
  (:use [castorocauda.timeline :only [->timeline timeline tl-deref tl-cons! sync-tl
                                      tl-map tl-merge tl-filter tl-apply tl-lift tl-of-take]]
        [castorocauda.util :only [q-select dom-element-events]]
        [test.test :only [prn= de ok prn-log]])
  (:use-macros [test.test-macro :only [qtest]]))


(js/module "castorocauda.timeline")

(qtest
 "(->timeline seq)"
 (prn= (tl-deref (->timeline '(1 2 3))) '(1 2 3) "converts seq into timeline")

 (prn= (tl-deref (->timeline [1 2 3])) '(1 2 3) "vector is also allowed"))

(qtest
 "(timeline & xs)"

 (prn= (tl-deref (timeline)) '() "(timeline) creates empty timeline")

 (prn= (tl-deref (timeline 1 2 3)) '(1 2 3) "(timeline & xs) creates timeline of xs")

 (prn= (tl-deref (timeline [1 2 3])) '([1 2 3]) "(timeline [& xs]) is no exception"))


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


(qtest
 "(sync-tl f src-tl dst-tl)"

 (let [tl1 (timeline)
       tl2 (timeline)]
   (prn= (sync-tl first tl1 tl2) nil "sync-tl yields nil")
   (tl-cons! 2 tl1)
   (tl-cons! "hello" tl1)
   (prn= (tl-deref tl1) '("hello" 2) "tl1 is modified")
   (prn= (tl-deref tl2) '("hello" 2) "Change to tl1 is synchronized to tl2"))


 (let [tl1 (timeline)
       tl2 (timeline)]
   (sync-tl first tl1 tl2 even?)
   (->> tl1 (tl-cons! 1) (tl-cons! 2) (tl-cons! 3) (tl-cons! 4))
   (prn= (tl-deref tl1) '(4 3 2 1) "tl1 has all four items")
   (prn= (tl-deref tl2) '(4 2) "tl2 is synchronized and filtered"))


 (let [tl1 (timeline)
       tl2 (timeline)]
   (sync-tl (comp inc first) tl1 tl2)
   (->> tl1 (tl-cons! 1) (tl-cons! 2) (tl-cons! 3) (tl-cons! 4))
   (prn= (tl-deref tl1) '(4 3 2 1) "tl1 has all four items unmodified")
   (prn= (tl-deref tl2) '(5 4 3 2) "tl2 is synchronized and f is applied to every item"))

 (let [tl1 (timeline)
       tl2 (timeline)]
   (sync-tl (comp inc first) tl1 tl2 odd?)
   (->> tl1 (tl-cons! 1) (tl-cons! 2) (tl-cons! 3) (tl-cons! 4))
   (prn= (tl-deref tl1) '(4 3 2 1) "tl1 has all four items unmodified")
   (prn= (tl-deref tl2) '(4 2)
         "tl2 is synchronized, filtered and f is applied to every item")))


(qtest
 "tl-map"
 (let [tl1 (timeline 1 2 3 4 5)
       tl2 (tl-map odd? tl1)]
   (prn= (tl-deref tl2) '(true false true false true)
         "tl-map returns a timeline. fn is applied to every item.")
   (tl-cons! 2 tl1)
   (tl-cons! 9 tl1)
   (prn= (tl-deref tl2) '(true false true false true false true)
         "synchronized and still fn is applied")))


(qtest
 "tl-merge"
 (let [tl1 (timeline 1 2 3 4)
       tl2 (timeline 4 3 2 1)
       tl3 (tl-merge + tl1 tl2)]
   (prn= (tl-deref tl3) '(5 5 5 5)
         "tl-merge returns a timeline. items are merged using the provided fn")
   (tl-cons! 2 tl1)
   (prn= (tl-deref tl3) '(6 5 5 5 5)
         "new item is added. merge fn use other tl's first val - 1")
   (tl-cons! 9 tl1)
   (prn= (tl-deref tl3) '(13 6 5 5 5 5)
         "new item is added. merge fn use other tl's first val - 2"))

  (let [tl1 (timeline 1 2 3 4)
        tl2 (timeline 5 6 7 8)
        tl3 (timeline 9 0 1 2)
        tl4 (tl-merge (fn [& xs] (vec xs)) tl1 tl2 tl3)]
   (prn= (tl-deref tl4) '([1 5 9] [2 6 0] [3 7 1] [4 8 2])
         "tl-merge returns a timeline. items are merged using the provided fn")
   (tl-cons! 2 tl1)
   (prn= (tl-deref tl4) '([2 5 9] [1 5 9] [2 6 0] [3 7 1] [4 8 2])
         "new item is added. merge fn use other tl's first val")
   (tl-cons! 9 tl2)
   (prn= (tl-deref tl4) '([2 9 9] [2 5 9] [1 5 9] [2 6 0] [3 7 1] [4 8 2])
         "new item is added. merge fn use other tl's first val")))



(qtest
 "tl-filter"
 (let [tl1 (timeline 1 2 3 4 5 6 7 8)
       tl2 (tl-filter even? tl1)]
   (prn= (tl-deref tl2) '(2 4 6 8)
         "tl-filter returns a timeline. items are filtered using the provided fn")
   (tl-cons! 2 tl1)
   (prn= (tl-deref tl2) '(2 2 4 6 8)
         "new item gets added if it passes filter fn")
   (tl-cons! 9 tl1)
   (prn= (tl-deref tl2) '(2 2 4 6 8)
         "don't change if the value fail filter fn")))


(defn test-lift-take
  "Run the same set of tests with entangled timelines created in various ways."
  [tl1 tl2]
  (prn= (->> tl2 tl-deref) '((5 4 3))
        "tl-take returns a timeline with `take` applied on its stream")
  (tl-cons! 6 tl1)
  (prn= (->> tl2 tl-deref first) '(6 5 4) "`take` gets applied to new stream in tl")
  (tl-cons! 7 tl1)
  (tl-cons! 8 tl1)
  (prn= (->> tl2 tl-deref first) '(8 7 6) "`take` gets applied to new stream in tl"))


(qtest
 "tl-apply"
 (let [tl1 (timeline 5 4 3 2 1)
       tl2 (tl-apply (partial take 3) tl1)]
   (test-lift-take tl1 tl2)))

(qtest
 "tl-lift"
 (let [f (tl-lift take)
       tl1 (timeline 5 4 3 2 1)
       tl2 (f 3 tl1)]
   (test-lift-take tl1 tl2)))

(qtest
 "lifted functions"
 (let [tl1 (timeline 5 4 3 2 1)
       tl2 (tl-of-take 3 tl1)]
   (test-lift-take tl1 tl2)))




(qtest
 "dom-events"

 (let [tl1
       (tl-map #(.-innerHTML (.-target %))
               (dom-element-events (q-select "#click-test") "click"))]
   (.click (q-select "#click-test"))
   (.click (q-select "#click-test"))
   (.click (q-select "#click-test"))
   (prn= (tl-deref tl1) '("fmm" "fmm" "fmm")
         "DOM events are collected and fn is applied")))
