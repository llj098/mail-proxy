(ns mail-proxy.core
  (:import
   [java.util.concurrent
    Executors]
   [org.jboss.netty.channel
    ChannelPipeline
    ChannelUpstreamHandler
    ChannelEvent]
   [javax.net.ssl
    X509TrustManager
    SSLContext]
   [org.jboss.netty.channel.group
    DefaultChannelGroup]
   [org.jboss.netty.channel.socket.nio
    NioClientSocketChannelFactory]
   [org.jboss.netty.bootstrap
    ClientBootstrap]
   [org.jboss.netty.handler.ssl
    SslHandler]
   [java.net
    InetSocketAddress]))

(def naive-trust-manager
  (reify X509TrustManager
    (checkClientTrusted [_ _ _])
    (checkServerTrusted [_ _ _])
    (getAcceptedIssuers [_])))

(def message-handler
  (reify 

(defn create-ssl-handler
  [{:keys [server-name server-port ignore-ssl-certs?]}]
  (doto
      (SslHandler.
       (doto
           (.createSSLEngine
            (doto (SSLContext/getInstance "TLS")
              (.init nil (into-array [naive-trust-manager]) nil))
            server-name
            server-port)
         (.setUseClientMode true)))
    (.setIssueHandshake true)
    (.setCloseOnSSLException true)))


(def client (ClientBootstrap.))
(def pipe (.getPipeline client))


(doto pipe
  (.addLast "ssl", 
            (create-ssl-handler {:server-name "pop3.feinno.com" :server-port 995}))
  (.addLast "framer" (DelimiterBasedFrameDecoder. 8192 (Delimiters/lineDelimiter)))
  (.addLast "decoder" (StringDecoder.))
  (.addLast "encoder" (StringEncoder.)))





(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))


(def mm [1 2 3])



(foo #{1})


