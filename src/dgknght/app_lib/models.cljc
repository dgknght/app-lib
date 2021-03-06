(ns dgknght.app-lib.models)

(defn- append-children
  [item grouped-items {:keys [id-fn
                              decorate-fn]
                       :or {decorate-fn identity
                            id-fn :id}
                       :as opts}]
  (if-let [children (get-in grouped-items [(id-fn item)])]
    (decorate-fn
      (assoc item
             :children
             (map #(append-children % grouped-items opts)
                  children)))
    item))

(defn nest
  "Given a collection of maps with recursive references, returns the same list with
  children assigned to their parents.
  
  options:
    :id-fn       - A key or fn that extracts the id from each model. Defaults to :id.
    :parent-fn   - A key or fn that extracts the parent id from each model. Defaults to :parent-id.
    :decorate-fn - A function that receives each model that has children with the model as the first argument and the children as the second and returns the model."
  ([collection] (nest {} collection))
  ([{:keys [parent-fn]
     :or {parent-fn :parent-id}
     :as opts}
    collection]
   (let [grouped (group-by parent-fn collection)]
     (->> collection
          (remove parent-fn)
          (mapv #(append-children % grouped opts))))))

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
