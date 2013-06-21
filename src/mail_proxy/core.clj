(ns mail-proxy.core
  (:use aleph.tcp
        lamina.core)
  (:gen-class))

(def global-opts {})

(defn create-options [name port ssl? ignore-ssl?]
  (let [basic {:server-name (str name) :server-port port}]
    (if ssl?
      (merge basic {:scheme "https"
                    :ignore-ssl-certs? ignore-ssl?})
      basic)))

(defn create-upstream-channel [& opts]
  (wait-for-result
   (tcp-client global-opts)))

(defn proxy-handler [channel client-info]
  (try
    (let [up-ch (create-upstream-channel)]
      (join channel up-ch)
      (join up-ch channel))
    (catch Exception e
      (do
        (println "fuck" "connect error")
        (close channel)))))

(defn -main [& opts]
  (do
    (def global-opts (apply create-options (map read-string (rest opts))))
    (start-tcp-server proxy-handler {:port (read-string (first opts))})))
