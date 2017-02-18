;   Copyright (c) Pavel Prokopenko. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php).
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;
;   -----------
;
;   This library is based on the work of Pavel Prokopenko for the
;   formar library (https://github.com/propan/formar).

(ns formant.core-test
  (:require [clojure.test :refer :all]
            [formant.validators :as validators]
            [formant.core :as formant]))

(defn passwords-match
  [m]
  (let [{:keys [password repeat-password]} (:data m)]
    (if-not (= password repeat-password)
      (update m :form-errors conj "Passwords don't match!")
      m)))

(def registration-data
  [[:username (validators/required) (validators/pattern #"^[a-zA-Z0-9_]+$")]
   [:email (validators/required) (validators/email)]
   [:password (validators/required)]
   [:repeat-password (validators/required)]])

(def auth-validator
  [[:user-id (validators/number) (validators/required)]])

(defn authenticate [m]
  (assoc m :auth {:user-id (get-in m [:input :user-id])}))

(def article-validator
  [[:title (validators/required)]])

(def registration-form
   [passwords-match])

(defn create-article [m]
  (update m :data assoc :id 1))

(deftest form-test
  (testing "Field validation"
    (let [params {:username        "bob"
                  :email           "email"
                  :password        ""
                  :repeat-password ""
                  :extra-field     "bad-data"}
          result (formant/perform {:input params}
                                  {:validators registration-data})]
      (is (= "bob" (get-in result [:data :username])))
      (is (= "email" (get-in result [:data :email])))
      (is (= "" (get-in result [:data :password])))
      (is (nil? (get-in result [:data :extra-field])))
      (is (result :data-errors))
      (is (nil? (get-in result [:data-errors :username])))
      (is (= "is not a valid email" (get-in result [:data-errors :email])))
      (is (= "is required" (get-in result [:data-errors :password])))))

  (testing "Form validation"
    (let [params {:username        "bob"
                  :email           "bob@thebobs.com"
                  :password        "pass"
                  :repeat-password "word"}
          result (formant/perform {:input params}
                                  {:validators registration-data
                                   :actions registration-form})]
      (is (empty? (:data-errors result)))
      (is (= 1 (count (:form-errors result))))
      (is (true? (some #(= "Passwords don't match!" %) (:form-errors result))))))

  (testing "Nested validations"
    (let [auth-actions {:validators auth-validator
                        :actions [authenticate]}
          validator {:requires [auth-actions]
                     :validators article-validator
                     :actions [create-article]}
          valid-params {:user-id 1
                        :title   "Welcome"}
          result (formant/perform {:input valid-params}
                                  validator)]
      (is (= "Welcome" (get-in result [:data :title])))
      (is (= 1 (get-in result [:data :id])))
      (is (= (valid-params :user-id)
             (get-in result [:auth :user-id])))))
  (testing "No validations"
    (let [data {:input {:user-id 3}}
          result (formant/perform data {:actions [authenticate]})]
      (is (= 3 (get-in result [:auth :user-id]))))))
