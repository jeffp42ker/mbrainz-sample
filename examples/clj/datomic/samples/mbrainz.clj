;;   Copyright (c) Metadata Partners, LLC. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns datomic.samples.mbrainz
  (:require [clojure.pprint :refer (pprint)]
            [datomic.client.api :as d]
            #_ [datomic.api :as d]
            [datomic.samples.mbrainz.rules :refer (rules)]))

;; this file is intended for evaluation, form-by-form, at the REPL

;;;;;;;;;;;;;;; get a connection ;;;;;;;;;;;;;;;;;;

;; Replace with your transactor's connection information
#_ (def uri "datomic:free://localhost:4334/mbrainz-1968-1973")

#_ (def conn (d/connect uri))

(def client (d/client {:server-type :dev-local
                       :system "dev"}))
(def conn (d/connect client {:db-name "mbrainz-1968-1973"}))
(def db (d/db conn))

;;;;;;;;;;;;;;; REPL safety and convenience ;;;;;;;;;;;;;;;;;;

;; for when you accidentally ask for all tracks...
(set! *print-length* 250)

;;;;;;;;;;;;;;; data queries ;;;;;;;;;;;;;;;;;;

(comment "What are the titles of all the tracks John Lennon played on?")

(d/q '[:find ?title
       :in $ ?artist-name
       :where
       [?a :artist/name ?artist-name]
       [?t :track/artists ?a]
       [?t :track/name ?title]]
     db
     "John Lennon")

(comment "What are the titles, album names, and release years
          of John Lennon's tracks?")

(d/q '[:find ?title ?album ?year
       :in $ ?artist-name
       :where
       [?a :artist/name   ?artist-name]
       [?t :track/artists ?a]
       [?t :track/name    ?title]
       [?m :medium/tracks ?t]
       [?r :release/media ?m]
       [?r :release/name  ?album]
       [?r :release/year  ?year]]
     db
     "John Lennon")

(comment "What are the titles, album names, and release years 
          of the John Lennon tracks released before 1970?")

(d/q '[:find ?title ?album ?year
       :in $ ?artist-name
       :where
       [?a :artist/name   ?artist-name]
       [?t :track/artists ?a]
       [?t :track/name    ?title]
       [?m :medium/tracks ?t]
       [?r :release/media ?m]
       [?r :release/name  ?album]
       [?r :release/year  ?year]
       [(< ?year 1970)]]
     db
     "John Lennon")

(comment "What are the titles, album names, and release years
          of John Lennon's tracks?")

(d/q '[:find ?title ?album ?year
       :in $ % ?artist-name
       :where
       [?a :artist/name   ?artist-name]
       [?t :track/artists ?a]
       [?t :track/name    ?title]
       (track-release ?t ?r)
       [?r :release/name  ?album]
       [?r :release/year  ?year]]
     db
     rules
     "John Lennon")

(comment "What are the titles, artists, album names, and release years
          of all tracks having the word \"always\" in their titles?")

(d/q '[:find ?title ?artist ?album ?year
       :in $ % ?search
       :where
       (track-search ?search ?track)
       (track-info ?track ?title ?artist ?album ?year)]
     db
     rules
     "always")

(comment "Who collaborated with one of the Beatles?")

(d/q '[:find ?aname ?aname2
       :in $ % [?aname ...]
       :where (collab ?aname ?aname2)]
     db rules ["John Lennon" "Paul McCartney" "George Harrison" "Ringo Starr"])

(comment "Who either directly collaborated with George Harrison, 
          or collaborated with one of his collaborators?")

(d/q '[:find ?aname ?aname2
       :in $ % [?aname ...]
       :where (collab-net-2 ?aname ?aname2)]
     db
     rules
     ["George Harrison"])

(comment "Who collaborated with Diana Ross or of her collaborators? (via recursion)")

(def query '[:find ?aname2
             :in $ % [[?aname]]
             :where (collab ?aname ?aname2)])

(d/q query
     db
     rules
     (d/q query
          db
          rules
          [["Diana Ross"]]))

(comment "Which artists have songs that might be covers of Bill Withers?")

(d/q '[:find ?aname ?tname
       :in $ ?artist-name
       :where
       [?a :artist/name ?artist-name]
       [?t :track/artists ?a]
       [?t :track/name ?tname]
       [(!= "Outro" ?tname)]
       [(!= "[outro]" ?tname)]
       [(!= "Intro" ?tname)]
       [(!= "[intro]" ?tname)]
       [?t2 :track/name ?tname]
       [?t2 :track/artists ?a2]
       [(!= ?a2 ?a)]
       [?a2 :artist/name ?aname]]
     db
     "Bill Withers")
