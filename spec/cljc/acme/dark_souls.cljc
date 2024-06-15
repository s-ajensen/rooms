(ns acme.dark-souls
  (:require [c3kit.bucket.api :as db]
            [c3kit.bucket.spec-helperc :as helperc]
            [acme.occupantc :as occuantc]
            [acme.roomc :as roomc]
            [acme.schema.room :as room.schema]
            [acme.schema.occupant :as occupant.schema]
            [speclj.core #?(:clj :refer :cljs :refer-macros) [before]])
  #?(:clj (:import (clojure.lang IDeref))))

(def schemas [room.schema/all
              occupant.schema/all])

(def shrine-code "shrine")
(def depths-code "depths")

(def firelink-atom (atom nil))
(def lautrec-atom (atom nil))
(def frampt-atom (atom nil))
(def patches-atom (atom nil))
(def laurentius-atom (atom nil))
(def depths-atom (atom nil))

(deftype Entity [atm]
  #?(:clj IDeref :cljs cljs.core/IDeref)
  (#?(:clj deref :cljs -deref) [this] (db/reload @atm)))

(def firelink (Entity. firelink-atom))                      ;; a populated room
(def lautrec (Entity. lautrec-atom))                        ;; a occupant at firelink
(def frampt (Entity. frampt-atom))                          ;; a occupant at firelink
(def patches (Entity. patches-atom))                        ;; a occupant at firelink
(def laurentius (Entity. laurentius-atom))                  ;; a occupant who hasn't joined
(def depths (Entity. depths-atom))                          ;; an empty room

(defn init []
  (reset! firelink-atom (roomc/create-room! shrine-code))
  (reset! depths-atom (roomc/create-room! depths-code))
  (reset! lautrec-atom (db/tx (occuantc/->occupant "Lautrec" "conn-lautrec")))
  (reset! frampt-atom (db/tx (occuantc/->occupant "Kingseeker Frampt" "conn-frampt")))
  (reset! patches-atom (db/tx (occuantc/->occupant "Patches" "conn-patches")))
  (reset! laurentius-atom (db/tx (occuantc/->occupant "Laurentius" "conn-laurentius")))
  (roomc/add-occupant! @firelink @lautrec)
  (roomc/add-occupant! @firelink @frampt)
  (roomc/add-occupant! @firelink @patches))

(defn with-schemas
  ([] (with-schemas schemas))
  ([& schemas] (helperc/with-schemas schemas)))

(defn init-with-schemas []
  (list (with-schemas)
    (before (init))))