package com.lgcns.dna.http3.client.util;

import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.client.http.ClientConnectionFactoryOverHTTP3;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class HttpUtil {
  public static void downloadChunk() throws Exception {
    long startTime = System.nanoTime();

    // Create and start HTTP2Client.
    HTTP2Client client = new HTTP2Client();
    SslContextFactory sslContextFactory = new SslContextFactory.Client(true);
    client.addBean(sslContextFactory);
    client.start();

    // Connect to host.
    String host = "localhost";
    int port = 8443;

    FuturePromise<Session> sessionPromise = new FuturePromise<>();
    client.connect(sslContextFactory, new InetSocketAddress(host, port), new ServerSessionListener.Adapter(), sessionPromise);

    // Obtain the client Session object.
    Session session = sessionPromise.get(5, TimeUnit.SECONDS);

    // Prepare the HTTP request headers.
    HttpFields.Mutable requestFields = HttpFields.build();
    requestFields.put("User-Agent", client.getClass().getName() + "/" + Jetty.VERSION);
    // Prepare the HTTP request object.
    MetaData.Request request = new MetaData.Request("GET", HttpURI.build("https://" + host + ":" + port + "/files/csv"),
        HttpVersion.HTTP_2, requestFields);

        
    // Create the HTTP/2 HEADERS frame representing the HTTP request.
    HeadersFrame headersFrame = new HeadersFrame(request, null, true);


    // Prepare the listener to receive the HTTP response frames.
    Stream.Listener responseListener = new Stream.Listener.Adapter() {
      @Override
      public void onData(Stream stream, DataFrame frame, Callback callback) {
        byte[] bytes = new byte[frame.getData().remaining()];
        frame.getData().get(bytes);
        int duration = (int) TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
        System.out.println("After " + duration + " seconds: " + new String(bytes));
        callback.succeeded();
      }
    };

    session.newStream(headersFrame, new FuturePromise<>(), responseListener);
    // session.newStream(headersFrame, new FuturePromise<>(), responseListener);
    // session.newStream(headersFrame, new FuturePromise<>(), responseListener);

    Thread.sleep(TimeUnit.SECONDS.toMillis(20));

    client.stop();
  }

  public static void downloadChunkByHttp3() throws Exception {
    // Connect to host.
    String host = "localhost";
    int port = 8444;

    SslContextFactory sslContextFactory = new SslContextFactory.Client(true);

    HTTP3Client http3Client = new HTTP3Client();
    http3Client.addBean(sslContextFactory);
    http3Client.getHTTP3Configuration().setStreamIdleTimeout(15000);
    // http3Client.getClientConnector().setSslContextFactory(sslContextFactory);
    http3Client.getQuicConfiguration().setVerifyPeerCertificates(false);

    ClientConnectionFactoryOverHTTP3.HTTP3 http3 = new ClientConnectionFactoryOverHTTP3.HTTP3(http3Client);
    HttpClientTransportDynamic httpClientTransportDynamic = new HttpClientTransportDynamic(http3);
    HttpClient httpClient = new HttpClient(httpClientTransportDynamic);
    httpClient.start();

    ContentResponse response = httpClient.GET("https://localhost:8444/files/csv");
    System.out.println(response.getContentAsString());

    httpClient.stop();    

    // SocketAddress serverAddress = new InetSocketAddress(host, port);
    // // FuturePromise<org.eclipse.jetty.http3.api.Session> sessionPromise = new FuturePromise<>();
    // CompletableFuture<org.eclipse.jetty.http3.api.Session.Client> sessionCF = http3Client.connect(serverAddress, new org.eclipse.jetty.http3.api.Session.Client.Listener() {});
    // org.eclipse.jetty.http3.api.Session.Client session = sessionCF.get();

    // // Configure the request headers.
    // HttpFields requestHeaders = HttpFields.build()
    //   .put(HttpHeader.USER_AGENT, "Jetty HTTP3Client 11.0.15");

    // // The request metadata with method, URI and headers.
    // MetaData.Request request = new MetaData.Request("GET", HttpURI.from("http://localhost:8444/files/csv"), HttpVersion.HTTP_3, requestHeaders);

    // // The HTTP/3 HEADERS frame, with endStream=true
    // // to signal that this request has no content.
    // org.eclipse.jetty.http3.frames.HeadersFrame headersFrame = new org.eclipse.jetty.http3.frames.HeadersFrame(request, true);

    // // Open a Stream by sending the HEADERS frame.
    // session.newRequest(headersFrame, new org.eclipse.jetty.http3.api.Stream.Client.Listener() {
    //   @Override
    //   public void onResponse(org.eclipse.jetty.http3.api.Stream.Client stream, org.eclipse.jetty.http3.frames.HeadersFrame frame)
    //   {
    //       MetaData metaData = frame.getMetaData();
    //       MetaData.Response response = (MetaData.Response)metaData;
    //       System.getLogger("http3").log(Level.INFO, "Received response {0}", response);
    //   }

    //   @Override
    //   public void onDataAvailable(org.eclipse.jetty.http3.api.Stream.Client stream)
    //   {
    //       // Read a chunk of the content.
    //       org.eclipse.jetty.http3.api.Stream.Data data = stream.readData();
    //       if (data == null)
    //       {
    //         // No data available now, demand to be called back.
    //         stream.demand();
    //       } else {
    //         // Process the content.
    //         System.getLogger("http3").log(Level.INFO, data.getByteBuffer().toString());

    //         // Notify the implementation that the content has been consumed.
    //         data.complete();

    //         if (!data.isLast())
    //         {
    //           // Demand to be called back.
    //           stream.demand();
    //         }
    //       }
    //   }
    // });
  }

  // public static HttpClient createHttpClient() throws SSLException {
  //   // Configure TLS if needed
  //   SslContextFactory sslContextFactory = new SslContextFactory.Client();
  //   // ...

  //   // Create the HTTP/2 client connector
  //   HttpConfiguration httpConfig = new HttpConfiguration();
  //   httpConfig.addCustomizer(new SecureRequestCustomizer());
  //   HttpConnectionFactory httpConnFact = new HTTP2CServerConnectionFactory(httpConfig);

  //   // Create the HTTP/2 client
  //   HttpClient httpClient = new HttpClient(new ServerConnector(null, httpConnFact, sslContextFactory));
  //   httpClient.start();
  //   return httpClient;
  // }

  // public int sendData(String url, byte[] buffer) throws IOException {
  //   HttpClient httpClient = createHttpClient();

  //   try {
  //     HttpExchange exchange = httpClient.newHTTPExchange();
  //     exchange.setRequestMethod(HttpMethod.POST);
  //     exchange.setURI(url);
  //     exchange.getRequestHeaders().add("Content-Type", "application/octet-stream");
  //     exchange.setResponseListener((exchange1 -> {
  //       try {
  //         InputStream responseBody = exchange1.getResponseBody();
  //         ByteArrayOutputStream baos = new ByteArrayOutputStream();
  //         byte[] buf = new byte[buffer.length];
  //         while (responseBody.read(buf) != -1)
  //           baos.write(buf);

  //         return Integer.parseInt(baos.toString());
  //       } catch (IOException e) {
  //         throw new UncheckedIOException(e);
  //       } finally {
  //         exchange1.close();
  //       }
  //     }));

  //     OutputStream requestBody = exchange.getRequestOutputStream();
  //     requestBody.write(buffer);
  //     requestBody.flush();

  //     return exchange.waitForDone().getResponseCode();
  //   } finally {
  //     httpClient.stop();
  //   }
  // }
}
