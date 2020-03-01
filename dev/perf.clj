(ns perf
  (:require [criterium.core :as crit]
            [datoms-differ.core2 :as c2]
            [datoms-differ.core :as c]))

(comment
  (def history-schema
    {:vessel/imo {:spec string? :db/unique :db.unique/identity}
     :vessel/name {:spec string?}
     :vessel/type {}
     :vessel/dim-port {:spec int?}
     :vessel/dim-starboard {:spec int?}
     :vessel/dim-bow {:spec int?}
     :vessel/dim-stern {:spec int?}

     :position/lon {}
     :position/lat {}
     :position/inst {:db/unique :db.unique/identity :spec inst?}
     :position/speed {:spec number?}
     :position/heading {:spec int?}
     :position/rate-of-turn {:spec int?}
     :position/course {:spec number?}

     :entity/id {:spec string? :db/unique :db.unique/identity}
     :location/place-id {:spec string?}
     :location/lat {}
     :location/type {:spec keyword?}
     :location/lon {}
     :location/polygon {:spec vector? :compound-value? true}
     :location/name {:spec string?}

     :move/id {:spec string? :db/unique :db.unique/identity}
     :move/inst {:spec inst?}
     :move/imo {:spec string?}
     :move/location {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
     :move/kind {:spec keyword?}})

  ;; TODO: DOH, You'll have to try to get hold of your own test fixture for this for now (:
  (def history-entities
    (->> (slurp "/Users/mrundberget/projects/datoms-differ/history-entities.edn")
         (clojure.edn/read-string {})))

  (let [sample-db (c2/create-conn history-schema)]
    (println "\n********  NEW CLEAN DB  *********")
    (time (c2/transact-sources! sample-db history-entities))
    (println "\n********  NEW UPDATE  ***********")
    (time (c2/transact-sources! sample-db history-entities))
    (count (:eavs @sample-db)))

  (let [sample-db (c/create-conn history-schema)]
    (println "\n********  OLD CLEAN DB  ********")
    (time (c/transact-sources! sample-db history-entities))
    (println "\n********  OLD UPDATE  ***********")
    (time (c/transact-sources! sample-db history-entities))
    nil)


  ;; ************* benchmarking **********************


  ;; NEW IMPL TESTS
  (def first-history-db
    (let [sample-db (c2/create-conn history-schema)]
      (c2/transact-sources! sample-db history-entities)
      sample-db))

  (defn transact-for-empty []
    (c2/transact-sources! (c2/create-conn history-schema) history-entities))

  (defn transact-for-existing []
    (let [db (atom @first-history-db)]
     (c2/transact-sources! db history-entities)))

  (crit/with-progress-reporting (crit/bench (transact-for-empty)))
  (crit/with-progress-reporting (crit/bench (transact-for-existing)))


  ;; OLD IMPL TESTS
  (def first-history-db-old
    (let [sample-db (c/create-conn history-schema)]
      (c/transact-sources! sample-db history-entities)
      sample-db))

  (defn transact-for-empty-old []
    (c/transact-sources! (c/create-conn history-schema) history-entities))

  (defn transact-for-existing-old []
    (let [db (atom @first-history-db)]
      (c/transact-sources! db history-entities)))

  (crit/with-progress-reporting (crit/bench (transact-for-empty-old)))
  (crit/with-progress-reporting (crit/bench (transact-for-existing-old)))

  )


(comment

 (let [db (empty-db history-schema)
       attrs (:attrs db)
       entities (doall (find-all-entities attrs (:prepare-observations history-entities)))
       old-refs (doall (create-refs-lookup attrs (:from default-db-id-partition) default-db-id-partition {} entities))]

   (time (create-refs-lookup attrs (get-lowest-new-eid default-db-id-partition (:eavs @first-history-db))  default-db-id-partition (:refs @first-history-db) entities))

   #_"done")

 )


(comment
  (def sample-datoms
    [(d/datom 536870912 :vessel/imo "1234" :prepare-vessels)
     (d/datom 536870912 :vessel/name "Fjordnerd" :prepare-vessels)
     (d/datom 536870912 :vessel/type :mf :prepare-vessels)
     (d/datom 536870913 :vessel/imo "5678" :prepare-vessels)
     (d/datom 536870913 :vessel/name "Limasol" :prepare-vessels)
     (d/datom 536870913 :vessel/type :ms :prepare-vessels)
     (d/datom 536870912 :vessel/imo "1234" :prepare-observations)
     (d/datom 536870912 :vessel/lat 60 :prepare-observations)
     (d/datom 536870912 :vessel/lon 59 :prepare-observations)
     ;; conflict
     (d/datom 536870912 :vessel/imo "12345" :prepare-infos)
     ;; dupe
     (d/datom 536870912 :vessel/imo "1234" :prepare-vessels)])

  (d/to-eavs sample-datoms)

  )
