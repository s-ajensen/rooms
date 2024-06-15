(ns acme.schema.full
  (:require [acme.schema.room :as room]
            [acme.schema.occupant :as occupant]))

(def full-schema
  (concat room/all
          occupant/all))
