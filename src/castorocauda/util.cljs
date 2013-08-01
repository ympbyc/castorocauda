(ns castorocauda.util)

;;; This file isn't what castorocauda uses.
;;; It is here for your convenience.


(defn dom-ready [fun]
  (set! (.-onload js/window) fun))

(defn q-select [q]
  (.querySelector js/document q))


(defn q-select-all [q]
  (.querySelectorAll js/document q))
