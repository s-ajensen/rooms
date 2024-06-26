(ns acme.game
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.bucket.api :as db]
            [c3kit.wire.websocket :as ws]
            [reagent.core :as reagent]))

(def current (reagent/track #(db/ffind :game)))

(defmethod ws/push-handler :game/update [push]
  (db/tx (:params push)))

(defn game []
  (fn []
    (prn "(db/ffind :game): " (db/ffind :game))
    [:div#-game-container
     [:p#-counter (:counter @current)]
     [:button#-inc-btn
      {:on-click #(ws/call! :game/inc-counter [] ccc/noop)}]]))