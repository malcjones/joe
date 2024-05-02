(ns joe.core
  (:require [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.term.colors :as colors])
  (:import (java.awt Desktop)
           (java.net URI)))

(def bookmarks (atom []))

(defn create-bookmark [title url tags]
  {:title title :url url :tags (vec tags)})

(defn validate-input [title url]
  (and (str/blank? title) (str/blank? url)
       (throw (IllegalArgumentException. "title and url should be provided and non-empty."))))

(defn add-bookmark [title url & tags]
  (validate-input title url)
  (swap! bookmarks conj (apply create-bookmark title url tags)))

(defn save-bookmarks []
  (spit "bookmarks.edn" (with-out-str (pprint @bookmarks))))

(defn load-bookmarks []
  (if (.exists (io/file "bookmarks.edn"))
    (try (reset! bookmarks (read-string (slurp "bookmarks.edn")))
         (catch Exception e (println "could not load bookmarks:" (.getMessage e))))
    (println "no bookmarks.edn found")))

(defn pretty-bm [b]
  (format "%s (%s) [%s]" (:title b) (colors/blue (:url b)) (str/join ", " (map colors/green (:tags b)))))

(defn list-bookmarks []
  (doseq [b @bookmarks] (println (pretty-bm b))))

(defn find-bookmarks [query]
  (filter #(or (str/includes? (str/lower-case (:title %)) (str/lower-case query))
               (some (partial str/includes? (str/lower-case query)) (:tags %)))
          @bookmarks))

(defn open-bookmark [query]
  (if-let [b (first (find-bookmarks query))]
    (do (.browse (Desktop/getDesktop) (URI. (:url b)))
        (println "opened bookmark:" (:title b)))
    (println "bookmark not found")))

(defmulti command (fn [cmd & _] (keyword cmd)))

(defmethod command :add [_ title url & tags]
  (try
    (add-bookmark title url tags)
    (println "bookmark added.")
    (catch IllegalArgumentException e (println (.getMessage e)))))

(defmethod command :list [_]
  (doseq [b (list-bookmarks)] (println b)))

(defmethod command :find [_ query]
  (doseq [b (find-bookmarks query)] (println b)))

(defmethod command :open [_ query]
  (open-bookmark query))

(defmethod command :save [_]
  (save-bookmarks)
  (println "bookmarks saved."))

(defmethod command :load [_]
  (load-bookmarks)
  (println "Bookmarks loaded."))

(defmethod command :help [_]
  (println "available commands:")
  (doseq [[k _] (methods command)] (println "  " (name k))))

(defmethod command :clear [_]
  (reset! bookmarks [])
  (println "bookmarks cleared."))

(defmethod command :default [cmd & _]
  (println "unknown command:" cmd))

(defn -main
  ([] (println "usage: joe <command> [args...]"))
  ([& args]
   (load-bookmarks)
   (apply command args)
   (save-bookmarks)))
