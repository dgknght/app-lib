(ns dgknght.app-lib.models)

(defn- append-children
  [item id-fn grouped-items]
  (if-let [children (get-in grouped-items [(id-fn item)])]
    (assoc item :children (map #(append-children % id-fn grouped-items)
                               children))
    item))

(defn nest
  "Given a collection of maps with recursive references, returns the same list with
  children assigned to their parents"
  ([collection] (nest :id :parent-id collection))
  ([id-fn parent-fn collection]
   (let [grouped (group-by parent-fn collection)]
     (->> collection
          (remove parent-fn)
          (map #(append-children % id-fn grouped))))))

(defn- unnest-item
  [item {:keys [id-fn
                path-segment-fn
                parent-ids
                parent-path]
         :as options
         :or {parent-ids '()
              parent-path []
              id-fn :id
              path-segment-fn :name}}]
  (let [path (conj parent-path (path-segment-fn item))
        ids (conj parent-ids (id-fn item))]
    (concat [(-> item
                 (assoc :path path
                        :parent-ids parent-ids
                        :child-count (count (:children item)))
                 (dissoc :children))]
            (mapcat #(unnest-item % (assoc options
                                           :parent-path path
                                           :parent-ids ids))
                    (:children item)))))

(defn unnest
  "Given a nested collection, return a flattened list"
  ([collection] (unnest {} collection))
  ([options collection]
   (mapcat #(unnest-item % (merge {:id-fn :id
                                   :path-segment-fn :name}
                                  options))
           collection)))
