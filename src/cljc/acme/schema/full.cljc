(ns acme.schema.full
  (:require [acme.schema.room :as room]
            [acme.schema.player :as player]))

(def full-schema
  [room/room
   player/answer
   player/player])
