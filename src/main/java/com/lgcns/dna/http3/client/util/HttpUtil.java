package com.lgcns.dna.http3.client.util;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.client.http.ClientConnectionFactoryOverHTTP3;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class HttpUtil {
  public static void downloadChunk() throws Exception {
    long start = System.currentTimeMillis();

    // Create and start HTTP2Client.
    HTTP2Client http2Client = new HTTP2Client();
    ClientConnectionFactoryOverHTTP2.HTTP2 http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);
    HttpClientTransportDynamic httpClientTransportDynamic = new HttpClientTransportDynamic(http2);
    httpClientTransportDynamic.getClientConnector().setSslContextFactory(new SslContextFactory.Client(true));
    HttpClient httpClient = new HttpClient(httpClientTransportDynamic);
    httpClient.getSslContextFactory();
    httpClient.start();

    InputStreamResponseListener listener = new InputStreamResponseListener();
    Request request = httpClient.newRequest("https://localhost:8443/files/download-chunk").method(HttpMethod.POST);
    request.headers(headers -> {
      headers.put(HttpHeader.USER_AGENT, "Jetty HTTP2Client 11.0.15");
      headers.put(HttpHeader.CONTENT_TYPE, "application/octet-stream");
    })
    .send(listener);

    Response response =  listener.get(30, TimeUnit.SECONDS);
    if (response.getStatus() == 200)
    {
      byte[] buff;

      try (InputStream input = listener.getInputStream())
      {
        buff = input.readAllBytes();
      }

      // sha512
      MessageDigest digest;

      try {
        digest = MessageDigest.getInstance("SHA-512");
        digest.reset();
        digest.update(buff);
        System.out.println(response.getHeaders().get("XX-CHUNK-CHECKSUM") + ", " + String.format("%0128x", new BigInteger(1, digest.digest())));
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      }

      NumberFormat formatter = NumberFormat.getNumberInstance();
      long timeElapsed = System.currentTimeMillis() - start;
      System.out.println("[Download Chunk:" + response.getStatus() + "] " + formatter.format(timeElapsed) + ", " + buff.length);
    } else {
      NumberFormat formatter = NumberFormat.getNumberInstance();
      long timeElapsed = System.currentTimeMillis() - start;
      System.out.println("[Download Chunk:" + response.getStatus() + "] " + formatter.format(timeElapsed));    
    }
    
    httpClient.stop();
  }

  public static void downloadChunkByHttp3() throws Exception {
    long start = System.currentTimeMillis();

    SslContextFactory sslContextFactory = new SslContextFactory.Client(true);

    HTTP3Client http3Client = new HTTP3Client();
    http3Client.addBean(sslContextFactory);
    http3Client.getHTTP3Configuration().setStreamIdleTimeout(15000);
    http3Client.getQuicConfiguration().setVerifyPeerCertificates(false);

    ClientConnectionFactoryOverHTTP3.HTTP3 http3 = new ClientConnectionFactoryOverHTTP3.HTTP3(http3Client);
    HttpClientTransportDynamic httpClientTransportDynamic = new HttpClientTransportDynamic(http3);
    HttpClient httpClient = new HttpClient(httpClientTransportDynamic);
    httpClient.start();

    // ContentResponse response = httpClient.POST("https://localhost:8444/files/download-chunk");
    InputStreamResponseListener listener = new InputStreamResponseListener();
    Request request = httpClient.newRequest("https://localhost:8444/files/download-chunk").method(HttpMethod.POST);
    request.headers(headers -> {
      headers.put(HttpHeader.USER_AGENT, "Jetty HTTP3Client 11.0.15");
      headers.put(HttpHeader.CONTENT_TYPE, "application/octet-stream");
    })
    .send(listener);

    Response response =  listener.get(10, TimeUnit.MINUTES);
    // ContentResponse contentRes = request.send();

    if (response.getStatus() == 200)
    {
      byte[] buff;

      try (InputStream input = listener.getInputStream())
      {
        buff = input.readAllBytes();
      }

      // sha512
      MessageDigest digest;

      try {
        digest = MessageDigest.getInstance("SHA-512");
        digest.reset();
        digest.update(buff);
        System.out.println(response.getHeaders().get("XX-CHUNK-CHECKSUM") + ", " + String.format("%0128x", new BigInteger(1, digest.digest())));
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      }

      NumberFormat formatter = NumberFormat.getNumberInstance();
      long timeElapsed = System.currentTimeMillis() - start;
      System.out.println("[Download Chunk3:" + response.getStatus() + "] " + formatter.format(timeElapsed) + ", " + buff.length);
    } else {
      NumberFormat formatter = NumberFormat.getNumberInstance();
      long timeElapsed = System.currentTimeMillis() - start;
      System.out.println("[Download Chunk3:" + response.getStatus() + "] " + formatter.format(timeElapsed));    
    }
    
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
