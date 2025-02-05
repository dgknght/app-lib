(ns dgknght.app-lib.test-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [ring.util.response :as res]
            [cheshire.core :as json]
            [crouton.html :as html]
            [dgknght.app-lib.test :as t]
            [dgknght.app-lib.test-assertions]))

(deftest get-a-set-from-an-attribute
  (is (= #{"One" "Two" "Three"}
         (t/attr-set :number
                     [{:number "One"}
                      {:number "Two"}
                      {:number "Three"}]))))

(deftest parse-an-html-body
  (testing "valid html"
    (is (comparable?
          {:html-body {:tag :html
                       :content [{:tag :head
                                  :content nil}
                                 {:tag :body
                                  :content [{:tag :h1
                                             :content ["You are here"]}]}]}}
          (t/parse-html-body
            {:body "<html><body><h1>You are here</h1></body></html>"}))
        "The parsed body is added at :html-body"))
  (testing "already parsed"
    (let [calls (atom [])]
      (with-redefs [html/parse-string (fn [& args]
                                        (swap! calls conj args)
                                        {})]
        (t/parse-html-body {:body "<html />"
                            :html-body {}}))
      (is (empty? @calls) "The parser is not invoked"))))

(deftest parse-a-json-body
  (testing "a map with :body"
    (is (comparable?
          {:json-body {:my-value "is high"}}
          (t/parse-json-body
            {:body "{\"my-value\":\"is high\"}"}))
        "The parsed body is added at :json-body"))
  (testing "a no-content response"
        (is (= {:status 204}
               (t/parse-json-body {:status 204}))
            "The map is returned unchanged"))
  (testing "invalid json"
    (is (= {:body "not valid json"
            :json-body {:parse-error "Unrecognized token 'not': was expecting 'null', 'true', 'false' or NaN\n at [Source: (StringReader); line: 1, column: 4]"}}
           (t/parse-json-body {:body "not valid json"}))))
  (testing "already parsed"
    (let [calls (atom [])]
      (with-redefs [json/parse-string (fn [& args]
                                        (swap! calls conj args)
                                        {})]
        (t/parse-json-body {:body "{}"
                            :json-body {}}))
      (is (empty? @calls) "The parser is not invoked"))))

(deftest parse-an-edn-body
  (testing "a map with :body"
    (is (comparable?
          {:edn-body {:user/first-name "John"}}
          (t/parse-edn-body
            {:body "{:user/first-name \"John\"}"}))
        "The response is returned with the parsed body at :edn-body"))
  (testing "a no-content response"
    (is (= {:status 204}
           (t/parse-edn-body {:status 204}))))
  (testing "a json response"
    (let [res (-> (res/response "{\"firstName\":\"John\"}")
                  (res/status 200)
                  (res/content-type "application/json"))]
      (is (= res (t/parse-edn-body res))
          "The response is returned unchanged")))
  (testing "invalid edn"
    (is (comparable?
          {:edn-body {:parse-error "Invalid token: :"}}
          (t/parse-edn-body {:body "{\"firstName\":\"John\"}"}))))
  (testing "already parsed"
    (let [calls (atom [])]
      (with-redefs [edn/read-string (fn [& args]
                                      (swap! calls conj args)
                                      {})]
        (t/parse-edn-body {:body "{}"
                            :edn-body {}}))
      (is (empty? @calls) "The reader is not invoked"))))

(deftest add-a-user-agent-to-a-request
  (is (= {:headers {"user-agent" "custom-agent"}}
         (t/user-agent {} "custom-agent"))))

(deftest create-a-multi-part-request
  (let [{:keys [body headers]} (t/multipart-body
                                 {}
                                 {:name "normal-field"
                                  :value "literal content"}
                                 {:name "file-field-1"
                                  :file-name "data.csv"
                                  :content-type "text/csv"
                                  :body "a,b,c\n1,2,3"})]
    (is (= {"content-type" "multipart/form-data;boundary=\"TestRequestFormBoundary\""}
           headers)
        "The proper content-type header is set")
    (is (instance? java.io.InputStream body)
        "The body is streamed")
    (is (= "--TestRequestFormBoundary\r\nContent-Disposition: form-data; name=\"normal-field\"\r\n\r\nliteral content\r\n--TestRequestFormBoundary\r\nContent-Disposition: form-data; name=\"file-field-1\"; filename=\"data.csv\"\r\nContent-Type: text/csv\r\n\r\na,b,c\n1,2,3--TestRequestFormBoundary--\r\n"
           (slurp body))
        "the body is constructed from the parts")))

(defn- send-mail [_msg])

(deftest capture-mail
  (t/with-mail-capture [mailbox send-mail 0]
    (send-mail :test)
    (is (= [:test] @mailbox))))
