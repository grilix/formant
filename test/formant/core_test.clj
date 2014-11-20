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
      (update-in m [:form-errors] conj "Passwords don't match!")
      m)))

(def registration-data
  [[:username (validators/required) (validators/pattern #"^[a-zA-Z0-9_]+$")]
   [:email (validators/required) (validators/email)]
   [:password (validators/required)]
   [:repeat-password (validators/required)]])

(def registration-form
   [passwords-match])

(deftest form-test
  (testing "Field validation"
    (let [params {"username"        "bob"
                  "email"           "email"
                  "password"        ""
                  "repeat-password" ""
                  "extra-field"     "bad-data"}
          result (formant/validate params registration-data)]
      (is (= "bob" (get-in result [:data :username])))
      (is (= "email" (get-in result [:data :email])))
      (is (= "" (get-in result [:data :password])))
      (is (nil? (get-in result [:data :extra-field])))
      (is (nil? (get-in result [:data-errors :username])))
      (is (= "is not a valid email" (get-in result [:data-errors :email])))
      (is (= "is required" (get-in result [:data-errors :password])))))

  (testing "Form validation"
    (let [params {"username"        "bob"
                  "email"           "bob@thebobs.com"
                  "password"        "pass"
                  "repeat-password" "word"}
          result (formant/validate params registration-data registration-form)]
      (is (empty? (:data-errors result)))
      (is (= 1 (count (:form-errors result))))
      (is (true? (some #(= "Passwords don't match!" %) (:form-errors result)))))))
