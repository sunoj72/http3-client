package com.lgcns.dna.http3.client.util;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.ByteBufferRequestContent;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.http3.api.Session;
import org.eclipse.jetty.http3.api.Stream;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.client.http.ClientConnectionFactoryOverHTTP3;
import org.eclipse.jetty.http3.frames.HeadersFrame;
import org.eclipse.jetty.util.Jetty;
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

      NumberFormat formatter = NumberFormat.getNumberInstance();

      System.out.println("[Chunk Checksums] " + response.getHeaders().get("X-DNA-CHUNK-CHECKSUM") + ", " + createChecksum(buff));
      System.out.println("[Chunk Sizes] " + response.getHeaders().get("X-DNA-CHUNK-SIZE") + ", " +  formatter.format(buff.length));

      long timeElapsed = System.currentTimeMillis() - start;
      System.out.println("[Download Chunk:" + response.getStatus() + "] " + formatter.format(timeElapsed) + ", " + formatter.format(buff.length));
    } else {
      NumberFormat formatter = NumberFormat.getNumberInstance();
      long timeElapsed = System.currentTimeMillis() - start;
      System.out.println("[Download Chunk:" + response.getStatus() + "] " + formatter.format(timeElapsed));    
    }
    
    httpClient.stop();
  }

  public static void downloadChunkAsync() throws Exception {
    long start = System.currentTimeMillis();

    // Create and start HTTP2Client.
    HTTP2Client http2Client = new HTTP2Client();
    ClientConnectionFactoryOverHTTP2.HTTP2 http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);
    HttpClientTransportDynamic httpClientTransportDynamic = new HttpClientTransportDynamic(http2);
    httpClientTransportDynamic.getClientConnector().setSslContextFactory(new SslContextFactory.Client(true));
    HttpClient httpClient = new HttpClient(httpClientTransportDynamic);
    httpClient.getSslContextFactory();
    // httpClient.setMaxConnectionsPerDestination(1000);
    httpClient.start();

    try {
      CountDownLatch requestLatch = new CountDownLatch(1);

      // BufferingResponseListener listener = new BufferingResponseListener(12*1024*1024) {
      //   long totalBytes = 0;

      //   @Override
      //   public void onContent(Response response, ByteBuffer content) {
      //     requestLatch.countDown();
      //     long remain = content.remaining();
      //     totalBytes += remain;

      //     System.out.println("[Download Chunk:onContent] " + remain + "," + totalBytes);

      //   }

      //   @Override
      //   public void onComplete(Result result) {
      //     NumberFormat formatter = NumberFormat.getNumberInstance();
      //     requestLatch.countDown();

      //     System.out.println("[Chunk Checksums] " + result.getResponse().getHeaders().get("X-DNA-CHUNK-CHECKSUM"));

      //     long timeElapsed = System.currentTimeMillis() - start;
      //     System.out.println("[Download Chunk:" + result.getResponse().getStatus() + "] " + formatter.format(timeElapsed) + ", " + formatter.format(totalBytes));
      //   }

      //   @Override
      //   public void onFailure(Response response, Throwable failure) {
      //     requestLatch.countDown();
      //     failure.printStackTrace();
      //   }
      // };

      Request request = httpClient.newRequest("https://localhost:8443/files/download-chunk")
        .method(HttpMethod.POST)
        .onResponseContentAsync((response, content, callback) ->
        {
            requestLatch.incrementAndGet();
            callbackRef.set(callback);
            requestLatch.countDown();
        });
      request.headers(headers -> {
        headers.put(HttpHeader.USER_AGENT, "Jetty HTTP2Client 11.0.15");
        headers.put(HttpHeader.CONTENT_TYPE, "application/octet-stream");
      })
      .send(listener);
      
      requestLatch.await();

    } finally {
      httpClient.stop();
    }

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
    httpClient.setMaxConnectionsPerDestination(1000);
    httpClient.start();

    InputStreamResponseListener listener = new InputStreamResponseListener();
    Request request = httpClient.newRequest("https://localhost:8444/files/download-chunk").method(HttpMethod.POST);
    request.headers(headers -> {
      headers.put(HttpHeader.USER_AGENT, "Jetty HTTP3Client 11.0.15");
      headers.put(HttpHeader.CONTENT_TYPE, "application/octet-stream");
    })
    .send(listener);

    Response response =  listener.get(10, TimeUnit.MINUTES);
    if (response.getStatus() == 200)
    {
      byte[] buff;

      try (InputStream input = listener.getInputStream())
      {
        buff = input.readAllBytes();
      }

      NumberFormat formatter = NumberFormat.getNumberInstance();

      System.out.println("[Chunk Checksums] " + response.getHeaders().get("X-DNA-CHUNK-CHECKSUM") + ", " + createChecksum(buff));
      System.out.println("[Chunk Sizes] " + response.getHeaders().get("X-DNA-CHUNK-SIZE") + ", " +  formatter.format(buff.length));

      long timeElapsed = System.currentTimeMillis() - start;
      System.out.println("[Download Chunk3:" + response.getStatus() + "] " + formatter.format(timeElapsed) + ", " + formatter.format(buff.length));
    } else {
      NumberFormat formatter = NumberFormat.getNumberInstance();
      long timeElapsed = System.currentTimeMillis() - start;
      System.out.println("[Download Chunk3:" + response.getStatus() + "] " + formatter.format(timeElapsed));    
    }
    
    httpClient.stop();    
  }

  public static void downloadChunkByAsyncHttp3() throws Exception {
    final long start = System.currentTimeMillis();

    SslContextFactory sslContextFactory = new SslContextFactory.Client(true);

    HTTP3Client http3Client = new HTTP3Client();
    http3Client.addBean(sslContextFactory);
    http3Client.getHTTP3Configuration().setStreamIdleTimeout(15000);
    http3Client.getQuicConfiguration().setVerifyPeerCertificates(false);

    http3Client.start();
    try {
      CountDownLatch requestLatch = new CountDownLatch(1);
      Session.Client session = http3Client.connect(new InetSocketAddress("localhost", 8444), new Session.Client.Listener() {})
        .get(10, TimeUnit.SECONDS);

      // Prepare the HTTP request headers.
      HttpFields requestFields = HttpFields.build()
        .put(HttpHeader.USER_AGENT, "Jetty HTTP3Client 11.0.15")
        .put(HttpHeader.CONTENT_TYPE, "application/octet-stream");

      // Prepare the HTTP request object.
      MetaData.Request request = new MetaData.Request("POST", HttpURI.from("https://localhost:8444/files/download-chunk"), HttpVersion.HTTP_3, requestFields);

      // Create the HTTP/3 HEADERS frame representing the HTTP request.
      HeadersFrame headersFrame = new HeadersFrame(request, true);

      // Send the HEADERS frame to create a request stream.
      // Stream stream = session.newRequest(headersFrame, new Stream.Client.Listener()
      session.newRequest(headersFrame, new Stream.Client.Listener()
      {
        int statusCode = 0;
        long totalBytes = 0;
        String checksumHeader = "";

        @Override
        public void onResponse(Stream.Client stream, HeadersFrame frame)
        {
          // Inspect the response status and headers.
          MetaData.Response response = (MetaData.Response)frame.getMetaData();
          statusCode = response.getStatus();
          checksumHeader = response.getFields().get("X-DNA-CHUNK-CHECKSUM");
          System.out.println("[Download Chunk3:onResponse] (" + response.getStatus() + ") " + response.getContentLength() + " bytes");

          if (frame.isLast()) {
            requestLatch.countDown();
            System.err.println("LAST RESPONSE HEADER = " + frame);
            return;
          }

          // Demand for response content.
          stream.demand();
        }

        @Override
        public void onDataAvailable(Stream.Client stream)
        {
          Stream.Data data = stream.readData();
          if (data != null)
          {
            // Process the response content chunk.
            int remain = data.getByteBuffer().remaining();
            totalBytes += remain;
            // System.out.println("[Download Chunk3:onDataAvailable] " + remain + "," + totalBytes);

            data.complete();
            if (data.isLast()) {
              requestLatch.countDown();

              System.out.println("[Chunk Checksums] " + checksumHeader);
              NumberFormat formatter = NumberFormat.getNumberInstance();
              long timeElapsed = System.currentTimeMillis() - start;
              System.out.println("[Download Chunk3:" + statusCode + "] " + formatter.format(timeElapsed) + ", " + formatter.format(totalBytes));
              return;
            }
            }
            // Demand for more response content.
            stream.demand();
        }

        // @Override
        // public void onTrailer(Stream.Client stream, HeadersFrame frame)
        // {
        //   System.err.println("RESPONSE TRAILER = " + frame);
        //   requestLatch.countDown();
        // }
      });

      requestLatch.await();

      // // Use the Stream object to send request content, if any, using a DATA frame.
      // ByteBuffer requestChunk1 = ...;
      // stream.data(new DataFrame(requestChunk1, false))
      //     // Subsequent sends must wait for previous sends to complete.
      //     .thenCompose(s ->
      //     {
      //         ByteBuffer requestChunk2 = ...;
      //         s.data(new DataFrame(requestChunk2, true)));
      //     }

    } finally {
      http3Client.stop();
    }

  }

  public static void uploadChunk() throws Exception {
    HTTP2Client http2Client = new HTTP2Client();
    ClientConnectionFactoryOverHTTP2.HTTP2 http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);
    HttpClientTransportDynamic httpClientTransportDynamic = new HttpClientTransportDynamic(http2);
    httpClientTransportDynamic.getClientConnector().setSslContextFactory(new SslContextFactory.Client(true));

    HttpClient httpClient = new HttpClient(httpClientTransportDynamic);
    httpClient.start();

    byte[] buff = generateRandomByteArray(10 * 1024 * 1024);

    Request request = httpClient.newRequest("https://localhost:8443/files/upload-chunk")
      .method(HttpMethod.POST)
      .agent("Jetty HTTP2Client 11.0.15")
      .headers(headers -> {
        headers.put(HttpHeader.CONTENT_TYPE, "application/octet-stream");
        headers.put("X-DNA-CHUNK-CHECKSUM", createChecksum(buff));
        headers.put("X-DNA-CHUNK-SIZE", Integer.toString(buff.length));
      });

    request.body(new ByteBufferRequestContent(ByteBuffer.wrap(buff)));
    request.send();

    httpClient.stop();
  }

  private static byte[] generateRandomByteArray(int size) {
    byte[] byteArray = new byte[size];
    new Random().nextBytes(byteArray);
    return byteArray;
  }

  private static String createChecksum(byte[] buff) {
    MessageDigest digest;
    String result="";

    try {
      digest = MessageDigest.getInstance("SHA-512");
      digest.reset();
      digest.update(buff);
      result = String.format("%0128x", new BigInteger(1, digest.digest()));
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }

    return result;
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
