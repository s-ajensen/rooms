(ns acme.routes
  (:require [acme.config :as config]
            [c3kit.apron.corec :as ccc]
            [c3kit.apron.util :as util]
            [c3kit.wire.ajax :as ajax]
            [clojure.string :as str]
            [compojure.core :as compojure :refer [defroutes routes]]
            [ring.util.response :as response]))

(defn wrap-prefix [handler prefix not-found-handler]
  (fn [request]
    (let [path (or (:path-info request) (:uri request))]
      (when (str/starts-with? path prefix)
        (let [request (assoc request :path-info (subs path (count prefix)))]
          (if-let [response (handler request)]
            response
            (not-found-handler request)))))))

(def resolve-handler
  (if config/development?
    (fn [handler-sym] (util/resolve-var handler-sym))
    (memoize (fn [handler-sym] (util/resolve-var handler-sym)))))

(defn lazy-handle
  "Reduces load burden of this ns, which is useful in development.
  Runtime errors will occur for missing handlers, but all the routes should be tested in routes_spec.
  Assumes all handlers take one parameter, request."
  [handler-sym request]
  (let [handler (resolve-handler handler-sym)]
    (handler request)))

(defmacro lazy-routes
  "Creates compojure route for each entry where the handler is lazily loaded.
  Why are params a hash-map instead of & args? -> Intellij nicely formats hash-maps as tables :-)"
  [table]
  `(routes
     ~@(for [[[path method] handler-sym] table]
         (let [method (if (= :any method) nil method)]
           (compojure/compile-route method path 'req `((lazy-handle '~handler-sym ~'req)))))))

(defn redirect-handler [path]
  (let [segments (str/split path #"/")
        segments (map #(if (str/starts-with? % ":") (keyword (subs % 1)) %) segments)]
    (fn [request]
      (let [params   (:params request)
            segments (map #(if (keyword? %) (get params %) %) segments)
            dest     (str/join "/" segments)]
        (response/redirect dest)))))

(defmacro redirect-routes [table]
  `(routes
     ~@(for [[[path method] dest] table]
         (let [method (if (= :any method) nil method)]
           (compojure/compile-route method path 'req `((redirect-handler ~dest)))))))

(def ws-handlers
  {
   :ws/close            'acme.room/ws-leave-room
   :room/create         'acme.room/ws-create-room
   :room/join           'acme.room/ws-join-room
   :room/fetch          'acme.room/ws-fetch-room
   :game/start          'acme.game/ws-start-game
   :game/submit-answers 'acme.game/ws-submit-answers
   :game/update-answer  'acme.game/ws-update-answer
   :game/next-category  'acme.game/ws-next-category
   })

(defn sleep-for-10 [] (Thread/sleep 10000))
(defn spinner [_]
  (sleep-for-10)
  (ajax/ok {} nil))

(def ajax-routes-handler
  (-> (lazy-routes
        {
         ;["/forgot-password" :post]  acme.auth/ajax-forgot-password
         ;["/recover-password" :post] acme.auth/ajax-recover-password
         ["/spinner" :get]    acme.routes/spinner
         ["/csrf-token" :get] acme.auth/ajax-csrf-token
         ;["/user/csrf-token" :get]   acme.auth/ajax-csrf-token
         ;["/user/signin" :post]      acme.auth/ajax-login
         })
    (wrap-prefix "/api" ajax/api-not-found-handler)
    ajax/wrap-ajax))

(def web-routes-handlers
  (lazy-routes
    {
     ["/" :get]               acme.layouts/web-rich-client
     ["/room/:code" :get]     acme.layouts/web-rich-client
     ;["/error" :any]                            acme.errors/web-error
     ;["/forgot-password" :get]                  acme.layouts/web-rich-client
     ;["/recover-password/:recovery-token" :get] acme.layouts/web-rich-client
     ;["/signout" :any]                          acme.auth/web-signout
     ;["/signout/:reason" :any]                  acme.auth/web-signout
     ["/user/websocket" :any] acme.auth/websocket-open
     }))

(def dev-handler
  (lazy-routes
    {
     ;["/sandbox" :get]                 acme.sandbox.core/index
     ;["/sandbox/" :get]                acme.sandbox.core/index
     ;["/sandbox/:page" :get]           acme.sandbox.core/handler
     ;["/sandbox/:page/:ns" :get]       acme.sandbox.core/handler
     ;["/sandbox/:page/:ns1/:ns2" :get] acme.sandbox.core/handler
     }))

(defroutes handler
           ajax-routes-handler
           web-routes-handlers
           (if config/production? ccc/noop dev-handler)
           )