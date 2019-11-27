# project

A Clojure game. Naive Bird.

## Usage

Open the website http://35.245.163.14:3000/ and you can play this game.

## Re-do

If you want to run this project in your own computer, you need to install "clojure" "leiningen". 

Also, you need to create a database (mysql) which have two tables.

First is "users" table, which include "user" "password".

Second is "ranklist" table, which include "user" "score" "level".

The database info should be updated in "/src/project/models/db.clj" line 4 to line 7.

Then you can run this project by using "lein ring server-headless" "lein figwheel" in terminal.

Open the page http://"your-IP":3000/ and you can play this game.
