(ns acme.home-spec
  (:require-macros [speclj.core :refer [around stub should-have-invoked should-not-have-invoked with-stubs describe context it should= should-be-nil should-contain should should-not before should-not-be-nil]])
  (:require [accountant.core :as accountant]
            [acme.occupant :as occupant]
            [c3kit.apron.corec :as ccc]
            [c3kit.wire.js :as wjs]
            [acme.dark-souls :as ds]
            [acme.home :as sut]
            [c3kit.wire.spec-helper :as wire]
            [acme.state :as state]))

(defn stub-navigate! []
  (around [it]
          (with-redefs [accountant/navigate! (stub :redirect!)]
            (it))))

(describe "Home"
  (with-stubs)
  (wire/stub-ws)
  (stub-navigate!)
  (wire/with-root-dom)
  (ds/with-schemas)
  (before (reset! state/nickname nil))

  (context "joins room with code and nickname"
    (it "UK2LLJ, Lautrec"
      (reset! state/nickname "Lautrec")
      (sut/navigate-to-room! ["UK2LLJ"])
      (should-have-invoked :redirect! {:with ["/room/UK2LLJ"]})))

  (context "nickname input"
    (before (wire/render [sut/home state/nickname]))

    (it "updates value on change"
      (wire/change! "#-nickname-input" "Lautrec")
      (should= "Lautrec" (wire/value "#-nickname-input"))
      (wire/change! "#-nickname-input" "Patches")
      (should= "Patches" (wire/value "#-nickname-input"))))

  (context "create room"
    (before (wire/render [sut/home state/nickname]))

    (it "does nothing if no nickname"
      (wire/click! "#-create-room-button")
      (should-not-have-invoked :ws/call!))

    (it "does nothing if blank nickname"
      (wire/change! "#-nickname-input" " ")
      (wire/click! "#-create-room-button")
      (should-not-have-invoked :ws/call!))))