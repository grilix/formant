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

(ns formant.validators-test
  (:require [clojure.test :refer :all]
            [formant.validators :as validators]))

(deftest test-number
  (testing "Ignores non-existing keys"
    (let [tran-fn (validators/number)
          result  (tran-fn {:data {}} :age)]
      (is (nil? (get-in result [:data-errors :age])))
      (is (nil? (get-in result [:data :age])))))

  (testing "Transfroms values to numbers"
    (let [tran-fn (validators/number)
          result  (tran-fn {:data {:age "30"}} :age)]
      (is (nil? (get-in result [:data-errors :age])))
      (is (= 30 (get-in result [:data :age])))))

  (testing "Allows custom error message"
    (let [tran-fn (validators/number :message "Your value is incorrect")
          result  (tran-fn {:data {:age "bad-value"}} :age)]
      (is (= "Your value is incorrect" (get-in result [:data-errors :age])))
      (is (= "bad-value" (get-in result [:data :age])))))

  (testing "Allows custom error message function"
    (let [tran-fn (validators/number :msg-fn (constantly "Your value is incorrect"))
          result  (tran-fn {:data {:age "bad-value"}} :age)]
      (is (= "Your value is incorrect" (get-in result [:data-errors :age])))
      (is (= "bad-value" (get-in result [:data :age]))))))

(deftest test-range-of
  (testing "Ignores non-existing keys"
    (let [tran-fn (validators/range-of :min 18)
          result  (tran-fn {:data {}} :age)]
      (is (nil? (get-in result [:data-errors :age])))
      (is (nil? (get-in result [:data :age])))))

  (testing "Converts values to numbers"
    (let [tran-fn (validators/range-of)
          result  (tran-fn {:data {:age "30"}} :age)]
      (is (nil? (get-in result [:data-errors :age])))
      (is (= 30 (get-in result [:data :age])))))
  
  (let [tran-fn (validators/range-of :min 18)]
    (testing "Only minimum is checked [positive]"
      (let [result (tran-fn {:data {:age 42}} :age)]
        (is (nil? (get-in result [:data-errors :age])))
        (is (= 42 (get-in result [:data :age])))))

    (testing "Only minimum is checked [negative]"
      (let [result (tran-fn {:data {:age 12}} :age)]
        (is (= "should be greater than 18" (get-in result [:data-errors :age])))
        (is (= 12 (get-in result [:data :age]))))))

  (let [tran-fn (validators/range-of :max 18)]
    (testing "Only maximum is checked [positive]"
      (let [result (tran-fn {:data {:age 12}} :age)]
        (is (nil? (get-in result [:data-errors :age])))
        (is (= 12 (get-in result [:data :age])))))

    (testing "Only maximum is checked [negative]"
      (let [result (tran-fn {:data {:age 42}} :age)]
        (is (= "should be less than 18" (get-in result [:data-errors :age])))
        (is (= 42 (get-in result [:data :age]))))))

  (let [tran-fn (validators/range-of :min 10 :max 16)]
    (testing "Range is checked [positive]"
      (let [result (tran-fn {:data {:age 12}} :age)]
        (is (nil? (get-in result [:data-errors :age])))
        (is (= 12 (get-in result [:data :age])))))

    (testing "Range is checked [negative]"
      (let [result (tran-fn {:data {:age 42}} :age)]
        (is (= "should be between 10 and 16" (get-in result [:data-errors :age])))
        (is (= 42 (get-in result [:data :age])))))))

(deftest test-choice
  (let [tran-fn (validators/choice #{"red" "green" "blue"})]

    (testing "Catches nil values"
      (let [result (tran-fn {} :color)]
        (is (= "is required" (get-in result [:data-errors :color])))))

    (testing "Catches wrong values"
      (let [result (tran-fn {:data {:color "yellow"}} :color)]
        (is (= "is not allowed" (get-in result [:data-errors :color])))))

    (testing "Allowes correct values"
      (let [result (tran-fn {:data {:color "red"}} :color)]
        (is (= "red" (get-in result [:data :color])))
        (is (nil? (get-in result [:data-errors :color])))))))

(deftest test-length
  (let [tran-fn (validators/length :is 5)]

    (testing "Accepts nils"
      (let [result (tran-fn {} :password)]
        (is (nil? (get-in result [:data-errors :password])))))

    (testing "Catches wrong values"
      (let [result (tran-fn {:data {:password "123456"}} :password)]
        (is (= "should be exactly 5 character(s)" (get-in result [:data-errors :password])))))

    (testing "Allows correct values"
      (let [result (tran-fn {:data {:password "12345"}} :password)]
        (is (nil? (get-in result [:data-errors :password]))))))

  (let [tran-fn (validators/length :min 6)]
    (testing "Accepts nils"
      (let [result (tran-fn {} :name)]
        (is (nil? (get-in result [:data-errors :name])))))

    (testing "Catches wrong values"
      (let [result (tran-fn {:data {:name "Bob"}} :name)]
        (is (= "should be at least 6 character(s)" (get-in result [:data-errors :name])))))

    (testing "Allows correct values"
      (let [result (tran-fn {:data {:name "Audrey"}} :name)]
        (is (nil? (get-in result [:data-errors :name]))))))

  (let [tran-fn (validators/length :max 6)]
    (testing "Accepts nils"
      (let [result (tran-fn {} :name)]
        (is (nil? (get-in result [:data-errors :name])))))

    (testing "Catches wrong values"
      (let [result (tran-fn {:data {:name "Frederic"}} :name)]
        (is (= "should be at most 6 character(s)" (get-in result [:data-errors :name])))))

    (testing "Allows correct values"
      (let [result (tran-fn {:data {:name "Audrey"}} :name)]
        (is (nil? (get-in result [:data-errors :name]))))))

  (let [tran-fn (validators/length :min 3 :max 6)]
    (testing "Accepts nils"
      (let [result (tran-fn {} :name)]
        (is (nil? (get-in result [:data-errors :name])))))

    (testing "Catches wrong values"
      (let [result (tran-fn {:data {:name "Le"}} :name)]
        (is (= "should be between 3 and 6 characters long" (get-in result [:data-errors :name])))))

    (testing "Allows correct values"
      (let [result (tran-fn {:data {:name "Audrey"}} :name)]
        (is (nil? (get-in result [:data-errors :name])))))))

(deftest ^:a test-required
  (testing "Catches nil values"
    (let [tran-fn (validators/required)
          result  (tran-fn {:data {}} :age)]
      (is (= "is required" (get-in result [:data-errors :age])))))

  (testing "Catches empty strings"
    (let [tran-fn (validators/required)
          result  (tran-fn {:data {:age ""}} :age)]
      (is (= "is required" (get-in result [:data-errors :age])))))

  (testing "Allows custom error message"
    (let [tran-fn (validators/required :message "Your value is incorrect")
          result  (tran-fn {} :age)]
      (is (= "Your value is incorrect" (get-in result [:data-errors :age])))))

  (testing "Allows custom error message function"
    (let [tran-fn (validators/required :msg-fn (constantly "Your value is incorrect"))
          result  (tran-fn {} :age)]
      (is (= "Your value is incorrect" (get-in result [:data-errors :age]))))))

(deftest test-pattern
  (testing "Ignores matches and nils"
    (let [tran-fn (validators/pattern #"\d+")]
      (is (nil? (get-in (tran-fn {:data {:age "18"}} :age) [:data-errors :age])))
      (is (nil? (get-in (tran-fn {} :age) [:data-errors :age])))))

  (testing "Catches wrong format & nils"
    (let [tran-fn (validators/pattern #"\d+" :allow-nil false)]
      (is (= "has incorrect format" (get-in (tran-fn {:data {:age "18 years"}} :age) [:data-errors :age])))
      (is (= "has incorrect format" (get-in (tran-fn {} :age) [:data-errors :age])))))

  (testing "Allows custom error message"
    (let [tran-fn (validators/pattern #"\d+" :message "Your value is incorrect" :allow-nil false)
          result  (tran-fn {} :age)]
      (is (= "Your value is incorrect" (get-in result [:data-errors :age])))))

  (testing "Allows custom error message function"
    (let [tran-fn (validators/pattern #"\d+" :msg-fn (constantly "Your value is incorrect") :allow-nil false)
          result  (tran-fn {} :age)]
      (is (= "Your value is incorrect" (get-in result [:data-errors :age]))))))

(deftest test-email
  (testing "Ignores matches and nils"
    (let [tran-fn (validators/email :allow-nil true)]
      (is (nil? (get-in (tran-fn {:data {:email "dale.cooper@thecoopers.com"}} :email) [:data-errors :email])))
      (is (nil? (get-in (tran-fn {} :email) [:data-errors :email])))))

  (testing "Catches wrong format & nils"
    (let [tran-fn (validators/email)]
      (is (= "is not a valid email" (get-in (tran-fn {:data {:email "email"}} :email) [:data-errors :email])))
      (is (= "is not a valid email" (get-in (tran-fn {} :email) [:data-errors :email]))))))

(deftest test-keywordize
  (let [tran-fn (validators/keywordize)]
    (testing "Ignores nils"
      (let [result (tran-fn {} :type)]
        (is (nil? (get-in result [:data :type])))))

    (testing "Perfroms transformation"
      (let [result (tran-fn {:data {:type "raw"}} :type)]
        (is (= :raw (get-in result [:data :type])))))))

(deftest test-checkbox
  (testing " Performs transformation"
    (let [tran-fn (validators/checkbox)]
      (are [result input] (= result (get-in (tran-fn input :enabled) [:data :enabled]))
           false {}
           false {:data {:enabled ""}}
           false {:data {:enabled "false"}}
           true  {:data {:enabled "on"}}))))
