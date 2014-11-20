# formant

A Clojure library that helps to transform and validate data.

This library is based on (it's a plain copy of) the great job Pavel Prokopenko
have done with formar (https://github.com/propan/formar).

## Installation

Include the library in your leiningen project dependencies:

```clojure
[org.clojars.grilix/formant "0.1.0"]
```

## Data validation

Simplest example, with your own validator:

```clojure
(require '[formant.core :as formant])

(defn require-name [data key]
  (if-not (get-in data [:data key])
    (assoc-in data [:data-errors key] "The name is required")
    data))

(def data-validators
  [[:name require-name]])

(formant/validate {} data-validators)
; => {:data-errors {:name "The name is required"}, :data {:name nil}}

(formant/validate {"name" "John"} data-validators)
; => {:data {:name "John"}}
```

Or, you can also use the included validators:

```clojure
(require '[formant.core :as formant]
         '[formant.validators :as validators])

(def require-name (validators/required :message "The name is required"))

(def data-validators-1
  [[:name require-name]])

; or, directly:
(def data-validators-2
  [[:name (validators/required :message "The name is required")]])

; or, using the default message:
(def data-validators-3
  [[:name (validators/required)]])


(formant/validate {} data-validators-1)
; => {:data-errors {:name "The name is required"}, :data {:name nil}}

(formant/validate {} data-validators-2)
; => {:data-errors {:name "The name is required"}, :data {:name nil}}

(formant/validate {} data-validators-3)
; => {:data-errors {:name "is required"}, :data {:name nil}}
```

If you have multiple fields (do you?), just add them to the validators list:

```clojure
(def data-validators
  [[:name (validators/required)]
   [:password (validators/required)]])

(formant/validate {} data-validators)
; => {:data-errors {:password "is required", :name "is required"}, :data {:password nil, :name nil}}
```

Want multiple validators?, sure:

```clojure
(defn cant-be-john [data key]
  (if (= (get-in data [:data :name])
         "John")
    (assoc-in
      (assoc-in data [:data :name] nil) ; we also want to remove the name,
                                        ; they are not John, right?
      [:data-errors :name]
      "We know you are not John!")
    data))

(def data-validators
  [[:name (validators/required) cant-be-john]])

(formant/validate {"name" "John"} data-validators)
; => {:data-errors {:name "We know you are not John!"}, :data {:name nil}}
```

## Form validations

When we know the data is valid and safe, we can peform other validations, we
call them "form validations".
Form validations could be, for example, checking that the user does not exist
already, checking for matching password confirmation, or anything else.

These validations are skipped when the data validation fails, so feel free
to assume that the data is valid until that point. Also, form validations
stops when one fails. Oh, and they can be used to transform the data!

Adding form validations:

```clojure
(require '[formant.core :as formant]
         '[formant.validators :as validators])

(defn passwords-match [m]
  (let [password (get-in m [:data :password])
        repeat-password (get-in m [:data :repeat-password])]
    (if-not (= password repeat-password)
      (update-in m [:form-errors] conj "Passwords should match")
      m)))

(def data-validators
  [[:password (validators/required)]
   [:repeat-password (validators/required)]])

(def form-validators
   [passwords-match])

(formant/validate {"password" "pass" "repeat-password" "diff"}
                  data-validators form-validators)
; => {:form-errors ("Passwords should match"), :data {:repeat-password "diff", :password "pass"}}
```

## Checking result

Now we have the validations, we want to know if data is valid. As you might
already guessed, the data is valid if :errors and :form-errors include
no errors. If you are feeling lazy, we have a helper function which will
tell you if a result indicates a valid data or not: wait for it... `valid?`.

So:

```clojure
(require '[formant.core :as formant]
         '[formant.validators :as validators])

(def data-validators
  [[:name (validators/required)]])

(let [params {}
      result (formant/validate params data-validators)]
 (if (formant/valid? result)
    "VALID!!"
    "Nope, it is not valid."))
; => "Nope, it is not valid."

(let [params {"name" "John"}
      result (formant/validate params data-validators)]
 (if (formant/valid? result)
    "VALID!!"
    "Nope, it is not valid."))
; => "VALID!!"
```

## Contributing

Yes, please.

## License

Copyright © 2013 Pavel Prokopenko (`formar` author).

Copyright © 2014 Gonzalo Arreche (I'll add my name too, nobody will notice).

Distributed under the Eclipse Public License.
