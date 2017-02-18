# formant

A Clojure library that helps performing actions on data.

> This library is based on the great job Pavel Prokopenko
> have done with formar (https://github.com/propan/formar).

## Installation

Include the library in your leiningen project dependencies:

```clojure
[org.clojars.grilix/formant "0.2.0"]
```

## Data format

Formant was designed to work with a simple format, adhering to that format will
help save a lot of development time:

```clojure
;; Initial data:
{:input {:color "Red"
         :size 4}}

;; After some processing:
{:input {:color "Red"
         :size "4"}
  :data {:color "Red"
         :size 4}}

;; After validation error:
{:input {:color "Red"
         :size "hey"}
  :data {:color "Red"
         :size nil}
  :data-errors {:size ("Is invalid")}}

;; After action errors:
{:input {:color "Red"
         :size "4"}
  :data {:color "Red"
         :size 4}
  :form-errors ("Size not available in that color")}
```

- `:input` The initial values we will work on (these are unsafe
  values, don't trust them).
- `:data` The processed/transformed values.
- `:data-errors` The errors found by the validators/transformers.
- `:form-errors` The errors found by the actions, normally not attached to
  a single field.

Whenever `:data-errors` or `:form-errors` exist, the data is considered
invalid and processing will stop.

## Forms

A form would be a set of validations and actions that define a result for a
given dataset.

Forms are defined as a hashmap and can include validations, actions and/or
other forms.

### Validators

Validators (or transformers) are executed first; They have the responsability
of transforming and validating the data before the actions are processed.

The validators will stop processing when an error is encountered, but only for
that field, i.e. next fields will be processed after it. They will be attached
to a field (they can be reused on multiple fields).

Even though validators order will be honored, validators should not rely on
other validators of the same form.

Validators are a vector of vectors. Each vector has the attibute name and the
validators/transformer functions associated to them:

```clojure
(require '[formant.core :as formant])

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

(formant/perform {} login-form)
;; => {:data-errors {:username "Is required"
                    :password "Is required"},
      :data {:username nil
             :password nil}}

(formant/perform {:input {:username "John"
                          :password "123john"}}
                  login-form)
;; => {:input {:username "John"
              :password "123john"}
      :data {:username "John"
             :password "123john"}}
```

### Actions

Actions take place after validations, and contain business logic. Processing
will stop after the first action that returns a state with errors.

```clojure
(require '[formant.core :as formant])

(defn create-user [params]
  (let [user-data (params :data)
        old-user (users-by-username (user-data :username))]
    (if old-user
      (assoc params :form-errors '("User exists"))
      (let [user (users-create user-data)]
        (update params :data assoc :id (user :id))))))

(def create-user-form
  {:actions [create-user]})

(formant/perform {:input {:username "john"}} create-user-form)
;; => {:input {:username "john"}
       :data {:username "john"}
       :form-errors ("User exists")}

(formant/perform {:input {:username "johnny"}} create-user-form)
;; => {:input {:username "johnny"}
       :data {:username "johnny"
              :id 2}}
```

### Requirements

This will be a list of other forms that must succeed before this form.

```clojure
(require '[formant.core :as formant])

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

(formant/perform {:input {:user-id 1
                          :title "First post"}}
                 create-post-form)
;; => {:input {:user-id 1
               :title "First post"}
       :data {:user-id 1
              :title "First post"
              :id 1}
       :auth {:id 1
              :username "john"}}
```

## Contributing

Yes, please.

## License

Copyright © 2013 Pavel Prokopenko (`formar` author).

Copyright © 2014-2017 Gonzalo Arreche.

Distributed under the Eclipse Public License.
