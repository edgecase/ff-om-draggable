(ns ff-om-draggable.core
  (:require [cljs.core.async :as async :refer [put! <! mult untap tap chan sliding-buffer]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [cljs.core.async.macros :as am :refer [go]]))

(enable-console-print!)

(def position-chan (chan (sliding-buffer 1)))
(def mouse-mult (mult position-chan))
(js/window.addEventListener "mousemove"
                            #(put! position-chan {:left (.-clientX %) :top (.-clientY %)}))

(defn target-position
  [e]
  (let [rect (.getBoundingClientRect (.-currentTarget e))]
    {:top (.-top rect) :left (.-left rect)}))

(defn move-start
  [e owner]
  (.preventDefault e)
  (let [mouse-tap (om/get-state owner :mouse-tap)
        current-position (target-position e)
        position-offset {:top (- (.-clientY e) (current-position :top))
                         :left (- (.-clientX e) (current-position :left))}]
    (tap mouse-mult mouse-tap)
    (om/set-state! owner :is-dragging true)
    (om/set-state! owner :position-offset position-offset)))

(defn move-end
  [e owner cursor position-cursor]
  (.preventDefault e)
  (let [mouse-tap (om/get-state owner :mouse-tap)]
    (untap mouse-mult mouse-tap)
    (om/set-state! owner :is-dragging false)
    (om/update! cursor position-cursor (target-position e))))

(defn position
  [item position-cursor state]
  (if (state :is-dragging)
    (state :position)
    (get-in item position-cursor)))

(defn draggable-item
  [view position-cursor]
  (fn [item owner]
    (reify
      om/IInitState
      (init-state [_]
        {:is-dragging false
         :position (get-in item position-cursor)
         :mouse-tap (chan)
         :position-offset nil})
      om/IWillMount
      (will-mount [_]
        (let [position (om/get-state owner :mouse-tap)]
          (go (while true
                (let [mouse (<! position)
                      offset (om/get-state owner :position-offset)
                      top (- (mouse :top) (offset :top))
                      left (- (mouse :left) (offset :left))]
                  (om/set-state! owner :position {:top top :left left}))))))
      om/IRenderState
      (render-state [_ state]
        (dom/div (clj->js {:style (conj {:position "absolute"} (position item position-cursor state))
                           :onMouseDown #(move-start % owner)
                           :onMouseUp #(move-end % owner item position-cursor)})
                 (om/build view item))))))
