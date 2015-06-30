package com.amazonaws.http;

import static com.amazonaws.SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.ChallengeState;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeLayeredSocketFactory;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.http.conn.ssl.SdkTLSSocketFactory;
import com.amazonaws.http.impl.client.HttpRequestNoRetryHandler;
import com.amazonaws.http.impl.client.SdkHttpClient;

import com.amazonaws.http.ConnectionManagerFactory;

public class Main {
  public static void main(String[] args) throws Exception {
    ClientConfiguration config = new ClientConfiguration();

    HttpParams httpClientParams = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(httpClientParams, config.getConnectionTimeout());
    HttpConnectionParams.setSoTimeout(httpClientParams, config.getSocketTimeout());
    HttpConnectionParams.setStaleCheckingEnabled(httpClientParams, true);
    HttpConnectionParams.setTcpNoDelay(httpClientParams, true);
    HttpConnectionParams.setSoKeepalive(httpClientParams, config.useTcpKeepAlive());

    int socketSendBufferSizeHint = config.getSocketBufferSizeHints()[0];
    int socketReceiveBufferSizeHint = config.getSocketBufferSizeHints()[1];
    if (socketSendBufferSizeHint > 0 || socketReceiveBufferSizeHint > 0) {
      HttpConnectionParams.setSocketBufferSize(httpClientParams,
      Math.max(socketSendBufferSizeHint, socketReceiveBufferSizeHint));
    }

    PoolingClientConnectionManager connectionManager = ConnectionManagerFactory
      .createPoolingClientConnManager(config, httpClientParams);
    SdkHttpClient httpClient = new SdkHttpClient(connectionManager, httpClientParams);
    httpClient.setHttpRequestRetryHandler(HttpRequestNoRetryHandler.Singleton);
    //httpClient.setRedirectStrategy(new NeverFollowRedirectStrategy());

    if (config.getLocalAddress() != null) {
      ConnRouteParams.setLocalAddress(httpClientParams, config.getLocalAddress());
    }

    try {
      Scheme http = new Scheme("http", 80, PlainSocketFactory.getSocketFactory());
      SSLSocketFactory sf = config.getApacheHttpClientConfig().getSslSocketFactory();
      if (sf == null) {
        sf = new SdkTLSSocketFactory(
        SSLContext.getDefault(),
        SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
      }
      Scheme https = new Scheme("https", 443, sf);
      SchemeRegistry sr = connectionManager.getSchemeRegistry();
      sr.register(http);
      sr.register(https);
      System.out.println("Success");
    } catch (NoSuchAlgorithmException e) {
      throw new AmazonClientException("Unable to access default SSL context", e);
    }
  }
}
