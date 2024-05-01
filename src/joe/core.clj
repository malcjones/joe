(ns joe.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.term.colors :refer [blue green]])
  (:import (java.awt Desktop)
           (java.net URI)))

(def bookmarks (atom ()))

(defn create-bookmark
  "Create a bookmark map with title, url, and tags."
  [title url & tags]
  {:title title :url url :tags tags})

(defn listing
  "Format a bookmark for display."
  [b]
  (str (:title b) " (" (blue (:url b)) ") [" (str/join ", " (map green (:tags b))) "]"))

(defn load-bookmarks
  "Load bookmarks from file or create an empty list."
  []
  (if (.exists (io/file "bookmarks.edn"))
    (reset! bookmarks (read-string (slurp "bookmarks.edn")))
    (println "no bookmarks found")))

(defn save-bookmarks
  "Save bookmarks to file using a(n) (optional) formatter function."
  ([formatter] (spit "bookmarks.edn" (formatter @bookmarks)))
  ([] (spit "bookmarks.edn" (pr-str @bookmarks))))

;; formatter

(def pretty-formatter (fn [b] (with-out-str (pprint b))))

(defn add-bookmark
  "Add a bookmark to the list."
  [title url & tags]
  (swap! bookmarks conj (apply create-bookmark title url tags))
  (println "added bookmark:" title))

(defn list-bookmarks
  "List all bookmarks."
  [] (doseq [bookmark @bookmarks]
       (println (listing bookmark))))

(defn find-bookmarks
  "Find bookmarks by title or tag."
  [query]
  (filter (fn [b]
            (or
             (str/includes? (str/lower-case (:title b)) (str/lower-case query))
             (some #(str/includes? % query) (:tags b))))
          @bookmarks))

(defn open-bookmark
  "Open the first bookmark that matches the query."
  [query]
  (if-let [b (first (find-bookmarks query))]
    (do
      (.browse (Desktop/getDesktop) (URI. (:url b)))
      (println "opened bookmark:" (:title b)))
    (println "bookmark not found")))

(defmulti command (fn [cmd & _] (keyword cmd)))

;; command definitions

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

(defmethod command :clear [_]
  (reset! bookmarks ()))

(defmethod command :default [cmd & _]
  (println "unknown command:" cmd))

(defn -main
  ([] (println "try 'joe help' for a list of commands"))
  ([& args]
   (load-bookmarks)
   (apply command args)
   (save-bookmarks)))