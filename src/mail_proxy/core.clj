(ns mail-proxy.core
  (:import
   [java.io
    BufferedReader
    InputStreamReader]
   [java.util.concurrent
    Executors]
   [org.jboss.netty.channel
    ChannelPipeline
    ChannelPipelineFactory
    Channels
    ChannelUpstreamHandler
    SimpleChannelUpstreamHandler
    DefaultChannelFuture
    ChannelEvent]
   [javax.net.ssl
    X509TrustManager
    SSLContext]
   [org.jboss.netty.channel.group
    DefaultChannelGroup]
   [org.jboss.netty.channel.socket.nio
    NioClientSocketChannelFactory
    NioServerSocketChannelFactory]
   [org.jboss.netty.bootstrap
    ClientBootstrap
    ServerBootstrap]
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

(def client-handler
  (proxy [SimpleChannelUpstreamHandler] []
    (handleUpstream [ctx e] (proxy-super handleUpstream ctx e))
    (channelDisconnected [ctx e]
      (let [cch (.getChannel e)
            sch (.getAttachment cch)]
        (do
          (.close cch)
          (.close sch))))
    (messageReceived [ctx e]
      (let [sch (-> ctx .getChannel .getAttachment)]
        (.write sch (.getMessage e))))
    (exceptionCaught [ctx e]
      (do
        (println (.getCause e))
        (let [cch (.getChannel e)
              sch (.getAttachment cch)]
          (do
            (.close sch)
            (.close cch)))))))

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


(defn create-client[]
  (let [client (ClientBootstrap. (NioClientSocketChannelFactory.
                                  (Executors/newCachedThreadPool)
                                  (Executors/newCachedThreadPool)))
        pipe (.getPipeline client)]
    (do
      (.setOption client "connectTimeoutMillis" 10000)
      (doto pipe
        (.addLast "ssl", (create-ssl-handler {:server-name "pop3.feinno.com" :server-port 995}))
;        (.addLast "framer" (DelimiterBasedFrameDecoder. 8192 (Delimiters/lineDelimiter)))
;        (.addLast "decoder" (StringDecoder.))
;        (.addLast "encoder" (StringEncoder.))
        (.addLast "handler" client-handler))
      client)))

(def message-handler
  (proxy [SimpleChannelUpstreamHandler] []
    (handleUpstream [ctx e] (proxy-super handleUpstream ctx e))
    (channelConnected [ctx e] )
    (messageReceived [ctx e] (println (.getMessage e)))
    (exceptionCaught [ctx e] (do (println (.getCause e)) (-> e .getChannel .close)))))

(def server-handler
  (proxy [SimpleChannelUpstreamHandler] []
    (messageReceived [ctx e]
      (.write (.getAttachment ctx) (.getMessage e)))
    (channelDisconnected [ctx e]
      (let [cch (.getAttachment ctx)
            sch (.getChannel e)]
        (do
          (.close cch)
          (.close sch))))
    (channelConnected [ctx e]
      (let [client (create-client)
            fu (.connect client (InetSocketAddress. "pop3.feinno.com" 995))
            cch (-> fu .awaitUninterruptibly .getChannel)]
        (do 
          (.setAttachment ctx cch)
          (.setAttachment cch (.getChannel e)))))
    (exceptionCaught [ctx e]
      (do
        (println (.getCause e))
        (let [sch (.getChannel e)
              cch (.getAttachment ctx)]
          (do
            (.close cch)
            (.close sch)))))))

(defn start-server [port]
  (let [svr (ServerBootstrap. (NioServerSocketChannelFactory.
                               (Executors/newCachedThreadPool)
                               (Executors/newCachedThreadPool)))
        pipe (.getPipeline svr)]
    (do
      (DefaultChannelFuture/setUseDeadLockChecker false)
      (.addLast pipe "handler" server-handler)
      (.bind svr (InetSocketAddress. port)))))

(defn -main[]
  (start-server 9999))