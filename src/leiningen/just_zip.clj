(ns leiningen.just-zip
  (:require [clojure.java.io :as io]
            [leiningen.core.main :refer [info]]
            [clojure.string :as str])
  (:import [java.io FileOutputStream]
           [java.util.zip ZipOutputStream ZipEntry]))


(defn process-item [vec-separator list-separator project name]
  (cond
    (string? name) name
    (keyword? name) (project name)
    (vector? name) (str/join vec-separator (map #(process-item vec-separator list-separator project %) name))
    (list? name) (str/join list-separator (map #(process-item vec-separator list-separator project %) name))))

(def process-name (partial process-item "" ""))
(def process-path (partial process-item java.io.File/separator ""))


(defn just-zip
  "just zips files..."
  [project]
  (let [dest (get-in project [:just-zip :dest])
        name (process-name project (get-in project [:just-zip :name]))
        paths (map #(process-path project %) (get-in project [:just-zip :files]))
        extension? (get-in project [:just-zip :extension?])]

    (with-open [out (-> (str (process-path project dest) java.io.File/separator name (when-not extension? ".zip"))
                        (FileOutputStream.)
                        (ZipOutputStream.))]
      (doseq [p paths
              f (file-seq (io/file p))]
        (when-not (.isDirectory f)
          (with-open [in (io/input-stream f)]
            (.putNextEntry out (ZipEntry. (.getPath f)))
            (io/copy in out)
            (.closeEntry out)
            (info "zipped file" (.getPath f))))))))


(comment
  (def test-project
    {:target-path "target"
     :name "test-project"
     :version "1.2.3-SNAPSHOT"
     :just-zip {:dest :target-path
                :files [:target-path]
                :name ["hello" :name "-" :version]}})


  (just-zip test-project))

