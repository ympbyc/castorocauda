(ns castorocauda.html)


(defn prn-log
  [x]
  (.log js/console (prn-str x))
  x)

(defn pad
  "Widen the collection `xs` upto length `n` and fill the blank with `padd`."
  [xs n padd]
  (let [diff-n (- n (count xs))]
    (concat xs (map (fn [_] padd) (range diff-n)))))


(defn attr-diffs
  "Calcurate the difference between two attribute map."
  [attr1 attr2 path]
  (let [diff (reduce (fn [acc [k v]]
                       (cond
                        (nil? (attr2 k))
                        (cons [:rem-att path k nil] acc)

                        (not= v (attr2 k))
                        (cons [:att path k (attr2 k)] acc)

                        :else
                        acc)) '() attr1)
        diff (reduce (fn [acc [k v]]
                       (if (nil? (attr1 k))
                         (cons [:att path k v] acc)
                         acc)) diff attr2)]
    diff))

(defn mk-path
  "Path is {:tag Keyword :index Int}"
  [tag n]
  {:tag   tag
   :index n})

(declare delta-dive)

(defn html-delta
  "Hiccup * Hiccup * [Path] * Int -> [Delta]
   where type Delta = [typ path att-name val]"
  [old-dom new-dom path n]
  (let [[tg1 at1 & chs1] old-dom
        [tg2 at2 & chs2] new-dom
        new-path (conj path (mk-path tg2 n))

        ;; attributes can be ommited
        [at1 chs1] (if (map? at1) [at1 chs1] [{}  (cons at1 chs1)])
        [at2 chs2] (if (map? at2) [at2 chs2] [{}  (cons at2 chs2)])]
    (cond
     (nil? new-dom)
     '()

     (= new-dom old-dom)
     '()

     (or (not (coll? new-dom))
         (not (coll? new-dom)))
     (list [:html path new-dom nil])

     (empty? new-dom)
     (list [:html path nil nil])

     (empty? old-dom)
     (list [:html path new-dom nil])

     (not= tg1 tg2)
     (list [:html path new-dom nil])

     (not= at1 at2)
     (concat (attr-diffs at1 at2 new-path)
             (delta-dive chs1 chs2 new-path))

     :else
     (delta-dive chs1 chs2 new-path))))


(defn path=
  [[_ path1 _ _] [_ path2 _ _]]
  (= path1 path2))


(defn merge-delta
  [[tg path a _] [_ _ b _]]
  (cond
   (and (seq? a) (seq? b))
   [tg path (concat a b) nil]

   (seq? b)
   [tg path (cons a b) nil]

   (seq? a)
   [tg path (concat a [b]) nil]

   :else
   [tg path (list a b) nil]))


(defn delta-dive
  [chs1 chs2 new-path]
  (let [x (max (count chs1) (count chs2))
        deltas (mapcat (fn [ch1 ch2 idx]
                    (html-delta ch1 ch2 new-path idx))
                  (pad chs1 x nil)
                  (pad chs2 x nil)
                  (range x))
        html-ds (filter (fn [[x & _]] (= x :html)) deltas)
        attr-ds (filter (comp not empty?) (filter (fn [[x & _ :as xs]] (not= x :html)) deltas))
        merged-html-ds (reduce (fn [d1 d2]
                                 (if (path= d1 d2)
                                   (merge-delta d1 d2)
                                   d1))
                               (first html-ds) (rest html-ds))]
    (->>
     (or merged-html-ds '())
     list
     (concat attr-ds))))
