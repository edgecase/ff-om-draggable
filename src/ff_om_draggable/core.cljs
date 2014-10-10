(ns ff-draggable.core
  (:require [cljs.core.async :as async :refer [put! <! mult untap tap chan sliding-buffer]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :refer [join]])
  (:require-macros [cljs.core.async.macros :as am :refer [go]]))

(enable-console-print!)

(defn update-mouse-position
  [e channel]
  (.preventDefault e)
  (let [position {:left (.-clientX e) :top (.-clientY e)}]
    (put! channel position)))

(def mouse-position (chan (sliding-buffer 1)))
(js/window.addEventListener "mousemove" #(update-mouse-position % mouse-position))

(defn move-start
  [e owner]
  (.preventDefault e)
  (let [mouse-position (om/get-state owner :mouse-position)
        position-tap (om/get-state owner :position-tap)
        current-position (om/get-state owner :position)
        position-offset {:top (- (.-clientY e) (current-position :top))
                         :left (- (.-clientX e) (current-position :left))}]
    (tap mouse-position position-tap)
    (om/set-state! owner :position-offset position-offset)))

(defn move-end
  [e owner cursor position-cursor]
  (.preventDefault e)
  (let [mouse-position (om/get-state owner :mouse-position)
        position-tap (om/get-state owner :position-tap)
        position (om/get-state owner :position)]
    (untap mouse-position position-tap)
    (om/update! cursor position-cursor position)))

(defn draggable-item
  [view position-cursor]
  (fn [cursor owner]
    (reify
      om/IInitState
      (init-state [_]
        {:mouse-position (mult mouse-position)
         :position {:left (get-in cursor (conj position-cursor :left))
                    :top (get-in cursor (conj position-cursor :top))}
         :position-tap (chan)
         :position-offset nil})
      om/IWillMount
      (will-mount [_]
        (let [position (om/get-state owner :position-tap)]
          (go (while true
                (let [mouse (<! position)
                      offset (om/get-state owner :position-offset)]
                  (om/set-state! owner :position {:top (- (mouse :top) (offset :top))
                                                  :left (- (mouse :left) (offset :left))}))))))
      om/IRenderState
      (render-state [_ state]
        (dom/div (clj->js {:style {:position "absolute"
                                   :top (-> state :position :top)
                                   :left (-> state :position :left)}
                           :onMouseDown #(move-start % owner)
                           :onMouseUp #(move-end % owner cursor position-cursor)})
                 (om/build view cursor))))))

