(ns joe.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.term.colors :as tc])
  (:import (java.awt Desktop)
           (java.net URI)))

(defrecord Bookmark [title url tags])
(def bookmarks (atom ()))

(defn bm->map [bm]
  (into {} bm))

(defn map->bm [m]
  (Bookmark. (:title m) (:url m) (:tags m)))

(defn listing [b]
  (str (:title b) " " (tc/blue (:url b)) " [" (str/join ", " (map tc/green (:tags b))) "]"))

(defn load-bookmarks []
  (if (.exists (io/file "bookmarks.edn"))
    (reset! bookmarks (map map->bm (read-string (slurp "bookmarks.edn"))))
    (println "no bookmarks found")))

(defn save-bookmarks []
  (spit "bookmarks.edn" (pr-str (map bm->map @bookmarks))))

(defn add-bookmark [title url & tags]
  (swap! bookmarks conj (Bookmark. title url tags))
  (println "added bookmark:" title))

(defn list-bookmarks
  ([] (doseq [b @bookmarks]
        (println (listing b)))))

(defn find-bookmarks [query]
  (filter (fn [b]
            (or
             (str/includes? (str/lower-case (:title b)) (str/lower-case query))
             (some #(str/includes? % query) (:tags b))))
          @bookmarks))

(defn find-bookmark [query]
  (first (find-bookmarks query)))

(defn open-bookmark [query]
  (let [b (find-bookmark query)]
    (when b
      (let [uri (URI. (:url b))]
        (.browse (Desktop/getDesktop) uri)))))

(defmulti command (fn [cmd & _] (keyword cmd)))

(defmethod command :add [_ title url & tags]
  (apply add-bookmark title url tags))

(defmethod command :list [_]
  (list-bookmarks))

(defmethod command :find [_ query]
  (doseq [b (find-bookmarks query)]
    (println (listing b))))

(defmethod command :open [_ query]
  (open-bookmark query))

(defmethod command :help [_]
  (println "commands:")
  (doseq [m (methods command)]
    (let [cmd (str (name (first m)))]
      (println "  " cmd))))

(defmethod command :default [cmd & _]
  (println "unknown command:" cmd))

(defn -main
  ([] (println "try 'joew help' for a list of commands"))
  ([& args]
   (load-bookmarks)
   (apply command args)
   (save-bookmarks)))