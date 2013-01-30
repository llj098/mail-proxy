(ns mail-proxy.core
  (:import
   [java.io
    BufferedReader
    InputStreamReader]
   [java.util.concurrent
    Executors]
   [org.jboss.netty.channel
    ChannelPipeline
    ChannelUpstreamHandler
    SimpleChannelUpstreamHandler
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
   [org.jboss.netty.handler.codec.frame
    DelimiterBasedFrameDecoder Delimiters]
   [org.jboss.netty.handler.codec.string
    StringEncoder StringDecoder]
   [java.net
    InetSocketAddress]))

(def naive-trust-manager
  (reify X509TrustManager
    (checkClientTrusted [_ _ _])
    (checkServerTrusted [_ _ _])
    (getAcceptedIssuers [_])))

(def message-handler
  (proxy [SimpleChannelUpstreamHandler] []
    (handleUpstream [ctx e] (proxy-super handleUpstream ctx e))
    (channelConnected [ctx e] )
    (messageReceived [ctx e] (println (.getMessage e)))
    (exceptionCaught [ctx e] (do (println (.getCause e)) (-> e .getChannel .close)))))

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


(defn -main[]
  (let [client (ClientBootstrap. (NioClientSocketChannelFactory.
                                  (Executors/newCachedThreadPool)
                                  (Executors/newCachedThreadPool)))
        pipe (.getPipeline client)]
    (do
      (doto pipe
        (.addLast "ssl", (create-ssl-handler {:server-name "pop3.feinno.com" :server-port 995}))
        (.addLast "framer" (DelimiterBasedFrameDecoder. 8192 (Delimiters/lineDelimiter)))
        (.addLast "decoder" (StringDecoder.))
        (.addLast "encoder" (StringEncoder.))
        (.addLast "handler" message-handler))
      (let [fu (-> client (.connect (InetSocketAddress. "pop3.feinno.com" 995)))
            ch (-> fu .awaitUninterruptibly .getChannel)
            in (BufferedReader. (InputStreamReader. System/in))]
            (while true
              (.write ch (str (.readLine in) "\r\n")))))))



