# Castorocauda - DOM Abstraction * FRP

![logo](http://d3j5vwomefv46c.cloudfront.net/photos/large/795746565.jpg)

## Introduction

Castorocauda provides a convenient way for client-side DOM manipulation.

Castorocauda is at this stage experimental. No API is fixed.

## Artifact

Add the following dependency to your project.clj file:

```
[castorocauda "0.1.2"]
```


## API

```clojure
(defn gendom
  "base-el :: `HTMLElement`
   old-edn :: hiccup-style EDN representing DOM
   new-edn :: hiccup-style EDN representing DOM"
  [old-edn new-edn base-el]
  new-edn)
```

## Usage

```
(ns yourapp.core
    (:use [castorocauda.util :only [dom-ready q-select]]
          [castorocauda.dom  :only [gendom]]))

(def app (atom {:note "blah"}))
(def dom (atom []))

(defn render [{:keys [note]}]
  [:div
    [:h2 "Your note:"]
    [:p note]])

(defn update-note [text]
  (swap! dom
         gendom
         (render (swap! app assoc :note text))
         (q-select "#note-view")))

(dom-ready
  #(.addEventListener
     (q-select "#text")
     "keyup"
     (fn [e] (update-note (.-value (.-target e)))))
```


Here's a visualization of DOM elements that gets modified when you type some numbers in.
![delta static](https://rawgithub.com/ympbyc/castorocauda/master/resources/public/images/castorocauda1.png)
![delta gif](https://rawgithub.com/ympbyc/castorocauda/master/resources/public/images/Castorocauda3.gif)
With a brand new html delta calculator and committer, you can trust that only the smallest possible set of nodes are re-rendered.


## Running the Test Suit

Castorocauda.html is fully tested. You need to run `lein cljsbuild once` to build /resources/public/js/tests.js before running the tests in the browser. Once that's done open test.html to see how it goes. Note that Castorocauda uses core.async, which is at this stage SNAPSHOT, for testing so you need to open project.clj and uncomment the specified line to build the tests.


## License

Distributed under the Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php), the same as Clojure.
