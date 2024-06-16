(ns acme.state
  (:require [c3kit.bucket.api :as db]
            [reagent.core :as reagent]))

(def nickname (reagent/atom nil))