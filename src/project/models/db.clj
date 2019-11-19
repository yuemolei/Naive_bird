(ns project.models.db
  (:require [clojure.java.jdbc :as sql]))

(def db-spec {:subprotocol "mysql"
              :subname "//localhost:3306/csci6221?serverTimezone=UTC"
              :user "root"
              :password "root"})

(defn test-db []
  (sql/query db-spec "select 3*5 as result"))

(defn save-user! [user password]
  (sql/insert! db-spec :users {:user user :password password}))
(defn insert-score! [user level score]
  (sql/insert! db-spec :ranklist {:user user :level level :score score}))
(defn select-user [user]
  (sql/query db-spec ["SELECT user FROM users WHERE user = ? " user]))

(defn check-password [user]
  (sql/query db-spec ["SELECT password FROM users WHERE user = ?" user]))

(defn select-all-users []
  (sql/query db-spec ["SELECT * from users"]))
