(ns acme.gamec
  (:require [c3kit.bucket.api :as db]))

(defn create-game [room-or-id]
  {:kind    :game
   :room    (:id room-or-id room-or-id)
   :counter 0})

(defn create-game! [room-or-id]
  (db/tx (create-game room-or-id)))