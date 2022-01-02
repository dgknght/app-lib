(ns dgknght.app-lib.api-async-test
  (:require [cljs.test :refer-macros [deftest is async]]
            [cljs.core.async :as a]
            [cljs-http.client :as http]
            [goog.string :as g-str]
            [dgknght.app-lib.api :refer [path]]
            [dgknght.app-lib.api-async :as api]))

(def pattern?
  (partial instance? js/RegExp))

(defmulti match?
  (fn [m & _]
    (cond
      (pattern? m) :regex
      (string? m)  :uri)))

(defmethod match? :regex
  [pattern {:keys [url]}]
  (re-find pattern url))

(defmethod match? :uri
  [expected {:keys [url]}]
  (= expected url))

(defn- handle
  [{:keys [matcher response response-fn]} opts]
  (when (match? matcher opts)
    (or response
        (response-fn opts))))

(defn- mock-req
  [calls & handlers]
  (fn [opts]
    (swap! calls conj opts)
    (let [out-ch (or (:channel opts) (ex-info "Expected to receive a :channel" opts))
          res (some #(handle % opts)
                    handlers)]
      (if res
        (a/go (a/>! out-ch res))
        (throw (g-str/format "Unable to match the request to \"%s\"" (:url opts))))
      out-ch)))

(deftest get-an-http-resource
  (async
    done
    (let [calls (atom [])]
      (with-redefs [http/request (mock-req calls {:matcher "/api/customers"
                                                  :response {:status 200
                                                             :body [{:first-name "John"}
                                                                    {:first-name "Jane"}]}})]
        (api/get (path :customers)
                 {}
                 {:transform (map
                               (fn [result]
                                 (is (= [{:first-name "John"}
                                         {:first-name "Jane"}]
                                        (:body result))
                                     "The correct data is returned")
                                 (is (= "/api/customers"
                                        (:url (first @calls)))
                                     "The GET call goes to the correct location")
                                 (done)))})))))

(deftest get-an-http-resource-with-error
  (async
    done
    (let [calls (atom [])]
      (with-redefs [http/request (mock-req calls {:matcher "/api/customers/101"
                                                  :response {:status 404
                                                             :success false
                                                             :body {:message "not found"}}})]
        (api/get (path :customers 101)
                 {}
                 {:transform (map (fn [_res]
                                    (is false "should not receive the response")
                                    (done)))
                  :handle-ex (fn [e]
                               (is (= "not found" (.-message e)))
                               (done))})))))

(deftest get-and-transform-a-resource
  (async
    done
    (a/go
      (let [calls (atom [])]
        (with-redefs [http/request (mock-req calls {:matcher "/api/customers"
                                                    :response {:status 200
                                                               :success true
                                                               :body [{:first-name "John"}
                                                                      {:first-name "Jane"}]}})]
          (let [add-indices (fn [s] (map-indexed #(assoc %2 :index %1) s))
                result (a/<! (api/get (path :customers)
                                      {}
                                      {:transform (map #(update-in % [:body] add-indices))}))]
            (is (= [{:first-name "John"
                     :index 0}
                    {:first-name "Jane"
                     :index 1}]
                   (:body result))
                "The correct data is returned")
            (done)))))))

(deftest post-a-resource
  (async
    done
    (a/go
      (let [calls (atom [])]
        (with-redefs [http/request (mock-req calls {:matcher "/api/customers"
                                                    :response {:status 201
                                                               :success true
                                                               :body {:id 101
                                                                      :first-name "Jimmy"}}})]
          (let [result (a/<! (api/post (path :customers)
                                       {:first-name "Jimmy"}))]
            (is (= {:id 101
                    :first-name "Jimmy"}
                   (:body result))
                "The body contains the created resource")
            (is (= :post (:method (first @calls)))
                "The POST http verb is used"))))
      (done))))

(deftest patch-a-resource
  (async
    done
    (a/go
      (let [calls (atom [])]
        (with-redefs [http/request (mock-req calls {:matcher "/api/customers/101"
                                                    :response {:status 201
                                                               :body {:id 101
                                                                      :first-name "Jimmy"}}})]
          (let [result (a/<! (api/patch (path :customers 101)
                                        {:first-name "Jimmy"}))]
            (is (= {:id 101
                    :first-name "Jimmy"}
                   (:body result))
                "The body contains the created resource")
            (is (= :patch (:method (first @calls)))
                "The PATCH http verb is used"))))
      (done))))

(deftest delete-a-resource
  (async
    done
    (a/go
      (let [calls (atom [])]
        (with-redefs [http/request (mock-req calls {:matcher "/api/customers/101"
                                                    :response {:status 204
                                                               :body nil}})]
          (let [result (a/<! (api/delete (path :customers 101)))]
            (is (nil? (:body result))
                "The body is empty")
            (is (= :delete (:method (first @calls)))
                "The DELETE http verb is used"))))
      (done))))
