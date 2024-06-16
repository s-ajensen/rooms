(ns acme.routes-spec
  (:require-macros [acme.spec-helperc :refer [it-routes]]
                   [speclj.core :refer [around before context describe it should= stub with-stubs]])
  (:require [acme.page :as page]
            [acme.routes :as sut]
            [secretary.core :as secretary]
            [acme.room :as room]
            [speclj.core]))

(describe "Routes"
  (with-stubs)
  (before (page/clear!)
    (secretary/reset-routes!)
    (sut/app-routes))

  (around [it] (with-redefs [sut/load-page! (stub :load-page!)] (it)))

  (it-routes "/" :home)
  (it-routes "/room/shrine" :room
             (should= "shrine" @room/code))
  (it-routes "/room/depths" :room
             (should= "depths" @room/code))
  )