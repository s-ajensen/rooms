(ns acme.schema.full
  (:require [acme.schema.room :as room]
            [acme.schema.occupant :as occupant]
            [acme.schema.game :as game]))

(def full-schema
  (concat room/all
          occupant/all
          game/all))
