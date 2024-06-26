(ns acme.init
  (:require #?(:cljs [acme.core :as core])
            #?(:cljs [acme.page :as page])
            #?(:cljs [reagent.core :as reagent])
            [c3kit.apron.legend :as legend]
            [c3kit.bucket.api :as db]
            [c3kit.bucket.memory]
            [c3kit.wire.api :as api]
            [acme.config :as config]
            [acme.schema.full :as schema]
            [acme.schema.game :as game]
            [acme.schema.occupant :as occupant]
            [acme.schema.room :as room]))

(defn install-legend! []
  (legend/init! {:room       room/room
                 :occupant   occupant/occupant
                 :game       game/game
                 :db/retract legend/retract
                 }))

#?(:cljs (defn install-reagent-db-atom! []
           (db/set-impl! (db/create-db config/bucket schema/full-schema))))

(defn configure-api! []
  (api/configure! #?(:clj  {:ws-handlers 'acme.routes/ws-handlers
                            :version     (api/version-from-js-file (if config/development? "public/cljs/acme_dev.js" "public/cljs/acme.js"))}
                     :cljs {:redirect-fn       core/goto!
                            })))
