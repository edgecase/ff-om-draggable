(ns ff-om-draggable.core
  (:require [cljs.core.async :as async :refer [put! <! mult untap tap chan sliding-buffer]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [cljs.core.async.macros :as am :refer [go]]))

(def position-chan (chan (sliding-buffer 1)))
(def movement (mult position-chan))

(defn e-position
  [e]
  {:left (.-clientX e) :top (.-clientY e)})

(defn touch-move
  [e]
  (.preventDefault e)
  (e-position (aget (.-changedTouches e) 0)))

(defn track-movement
  [e extract-pos]
  (put! position-chan (extract-pos e)))

(.addEventListener js/window "mousemove" #(track-movement % e-position))
(.addEventListener js/window "touchmove" #(track-movement % touch-move))

(defn move-start
  [e owner current-position]
  (when (not (om/get-state owner :disabled))
    (let [item-move (om/get-state owner :item-move)
          offset (merge-with - (e-position e) current-position)]
      (om/set-state! owner :new-position #(merge-with - % offset))
      (tap movement item-move))))

(defn touch-start
  [e owner current-position]
  (move-start (aget (.-changedTouches e) 0) owner current-position))

(defn mouse-start
  [e owner current-position]
  (move-start e owner current-position))

(defn move-end
  [owner cursor position-cursor]
  (let [item-move (om/get-state owner :item-move)]
    (untap movement item-move)
    (om/update! cursor position-cursor (om/get-state owner :position))
    (om/set-state! owner :new-position nil)))

(defn position
  [item position-cursor state]
  (if (state :new-position)
    (state :position)
    (get-in item position-cursor)))

(defn draggable-item
  [view position-cursor]
  (fn [item owner]
    (reify
      om/IInitState
      (init-state [_]
        {:position (get-in item position-cursor)
         :item-move (chan)
         :draggable (chan)
         :disabled false
         :new-position nil})
      om/IWillMount
      (will-mount [_]
        (let [position (om/get-state owner :item-move)]
          (go (while true
            (let [mouse (<! position)
                  new-position (om/get-state owner :new-position)]
              (om/set-state! owner :position (new-position mouse)))))
          (go (while true
              (let [draggable (om/get-state owner :draggable)
                    disabled (not (<! draggable))]
                (om/set-state! owner :disabled disabled)
                (when disabled (move-end owner item position-cursor)))))))
      om/IRenderState
      (render-state [_ state]
        (let [current-position (position item position-cursor state)
              primative-value #(if (om/cursor? %) @% %)]
          (dom/div (clj->js {:style (conj {:position "absolute"} current-position)
                             :onTouchStart #(touch-start % owner (primative-value current-position))
                             :onMouseDown #(mouse-start % owner (primative-value current-position))
                             :onTouchEnd #(move-end owner item position-cursor)
                             :onMouseUp #(move-end owner item position-cursor)})
                   (om/build view item {:init-state {:draggable (om/get-state owner :draggable)}})))))))
