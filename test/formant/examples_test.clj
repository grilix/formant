(ns formant.examples-test
  (:require [clojure.test :refer :all]
            [formant.validators :as validators]
            [formant.core :as formant]))

(defn users-create [data]
  (assoc data :id 2))

(defn users-by-username [username]
  (when (= username "john")
    {:id 1
     :username "john"}))

(defn users-by-id [id]
  (when (= id 1)
    {:id 1
     :username "john"}))

(defn posts-create [data]
  (assoc data :id 1))

(defn required []
  (fn [params key]
    (if (get-in params [:data key])
      params
      (assoc-in params [:data-errors key] "Is required"))))

(def login-validator
  [[:username (required)]
   [:password (required)]])

(def login-form
  {:validators login-validator})

(defn auth [params]
  (assoc params :auth (users-by-id (get-in params [:data :user-id]))))

(def auth-form
  {:validators [[:user-id (required)]]
   :actions [auth]})

(defn create-post [params]
  (let [post (posts-create {:user-id (get-in params [:auth :id])
                            :title (get-in params [:data :title])})]
    (update params :data assoc :id (post :id))))

(def create-post-form
  {:requires [auth-form]
   :validators [[:title (required)]]
   :actions [create-post]})

(defn create-user [params]
  (let [user-data (params :data)
        old-user (users-by-username (user-data :username))]
    (if old-user
      (assoc params :form-errors '("User exists"))
      (let [user (users-create user-data)]
        (update params :data assoc :id (user :id))))))

(def create-user-form
  {:validators [[:username (required)]]
   :actions [create-user]})

(deftest form-test
  (testing "README.md examples"
    (testing "validations"
      (let [result (formant/perform {} login-form)]
        (is (= {:data-errors {:username "Is required"
                              :password "Is required"},
                :data {:username nil
                       :password nil}}
               result)))
      (let [result (formant/perform {:input {:username "John"
                                             :password "123john"}}
                                    login-form)]
        (is (= {:input {:username "John"
                        :password "123john"}
                :data {:username "John"
                       :password "123john"}}
               result))))
    (testing "actions"
      (testing "when user doesn't exist"
        (let [result (formant/perform {:input {:username "johnny"}}
                                      create-user-form)]
          (is (= {:input {:username "johnny"}
                  :data {:username "johnny"
                         :id 2}}
                 result))))
      (testing "when user exists"
        (let [result (formant/perform {:input {:username "john"}}
                                      create-user-form)]
          (is (= {:input {:username "john"}
                  :data {:username "john"}
                  :form-errors '("User exists")}
                 result)))))
    (testing "requires"
      (let [result (formant/perform {:input {:user-id 1
                                             :title "First post"}}
                                    create-post-form)]
        (is (= {:input {:user-id 1
                        :title "First post"}
                :data {:user-id 1
                       :title "First post"
                       :id 1}
                :auth {:id 1
                       :username "john"}}
               result))))))
