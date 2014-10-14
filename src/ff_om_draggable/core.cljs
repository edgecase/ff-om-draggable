(ns ff-om-draggable.core
  (:require [cljs.core.async :as async :refer [put! <! mult untap tap chan sliding-buffer]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [cljs.core.async.macros :as am :refer [go-loop]]))

(enable-console-print!)

(def position-chan (chan (sliding-buffer 1)))
(def mouse-mult (mult position-chan))

(defn touch-move
  [e]
  (.preventDefault e)
  (let [touch (aget (.-changedTouches e) 0)]
    {:left (.-clientX touch) :top (.-clientY touch)}))

(js/window.addEventListener "mousemove" #(put! position-chan {:left (.-clientX %) :top (.-clientY %)}))
(js/window.addEventListener "touchmove" #(put! position-chan (touch-move %)))

(defn target-position
  [e]
  (let [rect (.getBoundingClientRect (.-currentTarget e))]
    {:top (.-top rect) :left (.-left rect)}))

(defn touch-start
  [e owner]
  (.preventDefault e)
  (let [user-movement (om/get-state owner :user-movement)
        touch (aget (.-changedTouches e) 0)
        current-position (target-position e)
        offset {:top (- (.-clientY touch) (current-position :top))
                :left (- (.-clientX touch) (current-position :left))}
        new-position (fn [mouse]
                       {:top (- (mouse :top) (offset :top))
                        :left (- (mouse :left) (offset :left))})]

    (om/set-state! owner :new-position new-position)
    (tap mouse-mult user-movement)))


(defn move-start
  [e owner]
  (.preventDefault e)
  (let [user-movement (om/get-state owner :user-movement)
        current-position (target-position e)
        offset {:top (- (.-clientY e) (current-position :top))
                :left (- (.-clientX e) (current-position :left))}
        new-position (fn [mouse]
                       {:top (- (mouse :top) (offset :top))
                        :left (- (mouse :left) (offset :left))})]
    (om/set-state! owner :new-position new-position)
    (tap mouse-mult user-movement)))

(defn move-end
  [e owner cursor position-cursor]
  (.preventDefault e)
  (let [user-movement (om/get-state owner :user-movement)]
    (untap mouse-mult user-movement)
    (om/update! cursor position-cursor (target-position e))
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
         :user-movement (chan)
         :new-position nil})
      om/IWillMount
      (will-mount [_]
        (let [position (om/get-state owner :user-movement)]
          (go-loop []
            (let [mouse (<! position)
                  new-position (om/get-state owner :new-position)]
              (om/set-state! owner :position (new-position mouse)))
            (recur))))
      om/IRenderState
      (render-state [_ state]
        (dom/div (clj->js {:style (conj {:position "absolute"} (position item position-cursor state))
                           :onTouchStart #(touch-start % owner)
                           :onMouseDown #(move-start % owner)
                           :onTouchEnd #(move-end % owner item position-cursor)
                           :onMouseUp #(move-end % owner item position-cursor)})
                 (om/build view item))))))
