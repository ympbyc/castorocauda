# Castorocauda - Client Side Web App Toolkit

![logo](http://d3j5vwomefv46c.cloudfront.net/photos/large/795746565.jpg)

## Introduction

Castorocauda is a descendent of [WebFUI](http://d3j5vwomefv46c.cloudfront.net/photos/large/795746565.jpg) -  a client side web application framework that free us from manual DOM mutation and the scattering of local state. Castorocauda inherits from WebFUI the State->EDN->DOM mechanics with improvements that keep DOM mutation at minimum. Castorocauda dropped the dom-watching plugin mechanics and delegated their roles to FRP streams called timelines.

Castorocauda is purposely not a framework. It is a library, a collection of composable functions. Unlike WebFUI, apps created using Castotocauda don't need to be singletons.

Castorocauda is at this stage experimental. No API is fixed.

## Artifact

Add the following dependency to your project.clj file:

```
[castorocauda "0.0.9"]
```


## A Simple App that Use Castorocauda

Here is an entire concrete example program using Castorocauda. It displays two edit fields and displays the sum of the numbers entered into those fields as a result.

```html
<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <title>add-two-numbers</title>
    <script src="main.js"></script>
  </head>
  <body>
    <h1>Add Two Numbers</h1>
    <div id="add-two-numbers-widget">
      <!-- Castorocauda renders this area -->
    </div>
  </body>
</html>
```


```clojure
(ns add-two-numbers.core
  (:use [castorocauda.core     :only [launch-app]]
        [castorocauda.util     :only [dom-ready q-select dom-delegated-events]]
        [castorocauda.timeline :only [tl-map tl-filter tl-merge]]))


(defn render-all
  "Map a snapshot of the app's state to an EDN representing DOM"
  [{:keys [a b result]
    :or   {a 0, b 0, result 0}}]
  [:div
   [:h2 (str a " + " b " = " result)]
   [:input#a-in {:type "number"}] " + "
   [:input#b-in {:type "number"}] " = " (str result)
   [:p "the result is "
    [:span (if (even? result) "even" "odd")]]])


(defn val-timeline
  "Watch el for keyup and extract integer value from it"
  [sel]
  (->> (dom-delegated-events "keyup" sel)              ;;timeline of keyup
       (tl-map #(->> % .-target .-value js/parseInt))  ;;timeline of values
       (tl-filter (comp not js/isNaN))                 ;;reject invalid values
       ))


(defn main
  "launch-app takes:
   1. a map of timelines
   2. the render-all function defined above
   3. a HTMLElement that Castorocauda renders its state in"
  []
  (let [a-tl (val-timeline "#a-in")
        b-tl (val-timeline "#b-in")]
    (launch-app
     {:a      a-tl
      :b      b-tl
      :result (tl-merge + a-tl b-tl)}
     render-all
     (q-select "#add-two-numbers-widget"))))


(dom-ready main)
```

Here's a visualization of DOM elements that gets modified when you type some numbers in.
![delta static](https://rawgithub.com/ympbyc/castorocauda/rewrite/resources/public/images/castorocauda1.png)
![delta gif](https://rawgithub.com/ympbyc/castorocauda/rewrite/resources/public/images/Castorocauda3.gif)
With new html delta calculator and committer, you can trust that only the smallest possible set of nodes are re-rendered.


## Running the Test Suit

Castorocauda.html and Castorocauda.timeline are fully tested. You need to run `lein cljsbuild once` to build /resources/public/js/tests.js before running the tests in the browser. Once that's done open test.html to see how it goes.



## License

Distributed under the Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php), the same as Clojure.
