(ns project.models.db
  (:require [clojure.java.jdbc :as sql]))

(def db-spec {:subprotocol "mysql"
              :subname "//34.74.9.108/cs6221?serverTimezone=UTC"
              :user "root"
              :password "root"})

(defn test-db []
  (sql/query db-spec "select 3*5 as result"))

(defn save-user! [user password]
  (sql/insert! db-spec :users {:user user :password password}))
(defn init-score! [user]
  (sql/insert! db-spec :ranklist {:user user :level 1 :score 0})
  (sql/insert! db-spec :ranklist {:user user :level 2 :score 0})
  (sql/insert! db-spec :ranklist {:user user :level 3 :score 0}))
(defn update-score! [user level score]
  (sql/update! db-spec :ranklist {:score score} ["user = ? and level = ?" user level]))
(defn select-user [user]
  (sql/query db-spec ["SELECT user FROM users WHERE user = ?" user]))
(defn get-score [user level]
  (sql/query db-spec ["SELECT score FROM ranklist WHERE user = ? AND level = ?" user level]))
(defn rank [level]
  (sql/query db-spec ["SELECT `user`,`score` FROM `ranklist` where level = ? ORDER BY `ranklist`.`score` DESC" level]))
(defn check-password [user]
  (sql/query db-spec ["SELECT password FROM users WHERE user = ?" user]))

(defn select-all-users []
  (sql/query db-spec ["SELECT * from users"]))
