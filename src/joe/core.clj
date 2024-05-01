(ns joe.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.term.colors :refer [blue green]])
  (:import (java.awt Desktop)
           (java.net URI)))

(def bookmarks (atom ()))

(defn create-bookmark [title url tags]
  {:title title :url url :tags tags})

(defn listing [b]
  (str (:title b) " (" (blue (:url b)) ") [" (str/join ", " (map green (:tags b))) "]"))

(defn load-bookmarks []
  (if (.exists (io/file "bookmarks.edn"))
    (reset! bookmarks (read-string (slurp "bookmarks.edn")))
    (println "no bookmarks found")))

(defn save-bookmarks [formatter]
  (spit "bookmarks.edn" (formatter @bookmarks)))

(def pretty-formatter (fn [b] (with-out-str (pprint b))))

(def tiny-formatter (fn [b] (pr-str b)))

(defn add-bookmark [title url & tags]
  (swap! bookmarks conj (apply create-bookmark title url tags))
  (println "added bookmark:" title))

(defn list-bookmarks []
  [] (doseq [bookmark @bookmarks]
       (println (listing bookmark))))

(defn find-bookmarks [query]
  (filter (fn [b]
            (or
             (str/includes? (str/lower-case (:title b)) (str/lower-case query))
             (some #(str/includes? % query) (:tags b))))
          @bookmarks))

(defn find-bookmark [query]
  (first (find-bookmarks query)))

(defn open-bookmark [query]
  (if-let [b (find-bookmark query)]
    (do
      (.browse (Desktop/getDesktop) (URI. (:url b)))
      (println "opened bookmark:" (:title b)))
    (println "bookmark not found")))

(defmulti command (fn [cmd & _] (keyword cmd)))

(defmethod command :add [_ title url & tags]
  (add-bookmark title url tags))

(defmethod command :list [_]
  (list-bookmarks))

(defmethod command :find [_ query]
  (doseq [b (find-bookmarks query)]
    (println (listing b))))

(defmethod command :open [_ query]
  (open-bookmark query))

(defmethod command :help [_]
  (println "commands:")
  (doseq [m (sort (filter #(not= :default (first %)) (methods command)))]
    (let [cmd (str (name (first m)))]
      (println "  " cmd))))

(defmethod command :save-pretty [_]
  (save-bookmarks pretty-formatter)
  (System/exit 0))

(defmethod command :default [cmd & _]
  (println "unknown command:" cmd))

(defn -main
  ([] (println "try 'joe help' for a list of commands"))
  ([& args]
   (load-bookmarks)
   (apply command args)
   (save-bookmarks tiny-formatter)))