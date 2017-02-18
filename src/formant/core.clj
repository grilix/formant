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

(ns formant.core)

(defn valid?
  "Checks if the given form is valid."
  ([form]
   (and (empty? (:data-errors form))
        (empty? (:form-errors form))))
  ([form attribute]
   (nil? (get-in form [:data-errors attribute]))))

(defn- transform-field
  "Transforms and validates a field using a transformers.

   This function returns what the last transformer returns. Transformers
   are expected to return a map.

   Example usage:

     (def validators
       [[:username (required) (pattern #\"^[a-zA-Z0-9_]+$\")]
        [:password (required)]
        [:repeat-password (required)]])

     (transform-field
       \"John\"
       {:data {}}
       (first validators))"
  [value dest [field & transformers]]
    (loop [result (assoc-in dest [:data field] value)
           transformers transformers]
      (if (seq transformers)
        (let [transformer (first transformers)
              result (transformer result field)]
          (if (valid? result field)
            (recur result
                   (rest transformers))
            result))
        result)))

(defn- perform-steps
  "Iterate steps until complete or one returns an error."
  [result actions fun]
  (loop [result result
         actions actions]
    (if (and (seq actions) (valid? result))
      (recur (fun result (first actions))
             (rest actions))
      result)))

(defn- process-validators [validators params]
  (if (seq validators)
    (let [inputs (or (params :input) {})]
      (reduce #(transform-field (inputs (first %2))
                                %1
                                %2)
              params
              validators))
      params))

(defn perform
  "Transforms and validates params with a set of rules
   defined by validators and actions It returns a map - the
   result of this transfromation.

   Returns params with the following keys added:
     :data        - all data produced by transformers
     :data-errors - all data errors detected by field transformers
     :form-errors - a vector of errors detected by form transformers

   Notes:
     1. The field transformation stops if any of the field transformers
        detects an error.
     2. If any of field transformers detects a problem, none of the form
        transformers is triggered.
     3. If a form transformer detects an issue, the transformation ends.

   Example usage:
     (def validators
       [[:username (required) (pattern #\"^[a-zA-Z0-9_]+$\")]
        [:password (required)]
        [:repeat-password (required)]])

     (def form-actions
       [password-match])

     (perform {:input {:username \"superuser\"}}
              {:validators validators
               :actions form-actions})"
  ([params {validators :validators
            requires   :requires
            actions    :actions}]
   (let [params (if requires
                  (perform-steps params requires
                                (fn [result action]
                                  (perform result action)))
                  params)
         result (process-validators validators params)]
     (perform-steps result actions
                    (fn [result action]
                      (action result))))))
