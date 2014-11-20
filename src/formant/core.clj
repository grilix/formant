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
  "Transforms and validates a field using a transformers list
   (see transform). The transformers are applied in sequence and it will
   stop when a transformer fail.

   This function returns what the last transformer return. Transformers
   are expected to return a map (see transform).

   Example usage:

     (def data-validators
       [[:username (required) (pattern #\"^[a-zA-Z0-9_]+$\")]
        [:email (required) (email)]
        [:password (required)]
        [:repeat-password (required)]])

     (transform-field
       {\"username\" \"John\"}
       {:data {}}
       (first data-validators))"
  [source dest [field & transformers]]
  (let [value (get source (name field))]
    (loop [result (assoc-in dest [:data field] value)
           transformers  transformers]
      (if-not (empty? transformers)
        (let [transformer (first transformers)
              result (transformer result field)]
          (if (valid? result field)
            (recur result
                   (rest transformers))
            result))
        result))))

(defn validate
  "Transforms and validates params (presumably an http-form) with a set of rules
   defined by data-validators and form-validators. It returns a map - the
   result of this transfromation.

   Returns a map with the following keys:
     :data        - all data produced by transformers
     :data-errors - all data errors detected by field transformers
     :form-errors - a vector of errors detected by form transformers

   Notes:
     1. The field transformation stops if any of the field transformers detects an error.
     2. If any of field transformers detects a problem, non of the form transformers is triggered.
     3. If a form transformer detects an issue, the transformation ends.

   Example usage:
     (def data-validators
       [[:username (required) (pattern #\"^[a-zA-Z0-9_]+$\")]
        [:email (required) (email)]
        [:password (required)]
        [:repeat-password (required)]])

     (def form-validators
       [password-match])

     (validate {\"username\" \"superuser\"}
               data-validators
               form-validators)"
  ([params data-validators]
   (validate params data-validators []))
  ([params data-validators form-validators]
   (let [result (reduce (partial transform-field params)
                        {} data-validators)]
     (if (valid? result)
       (loop [data result
              form-validators form-validators]
         (if (and (not (empty? form-validators))
                  (valid? data))
           (recur ((first form-validators) data)
                  (rest form-validators))
           data))
       result))))
