package com.BilawalAhmed0900;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class RelaxedSSLContext
{
    // Create a trust manager that does not validate certificate chains like
    public static TrustManager[] trustAllCerts = new TrustManager[]
            {
                new X509TrustManager()
                {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers()
                    {
                        return null;
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
                    {
                        //No need to implement.
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
                    {
                        //No need to implement.
                    }
                }
            };

    //hostname verifier. All hosts valid
    public static HostnameVerifier allHostsValid = (hostname, session) -> true;

    //hostname verifier. Only localhost and 127.0.0.1 valid
    public static HostnameVerifier localhostValid = (hostname, session) -> "localhost".equals(hostname) || "127.0.0.1".equals(hostname);


    public static SSLContext getInstance()
            throws KeyManagementException, NoSuchAlgorithmException
    {
        return getInstance("SSL");
    }

    //get a 'Relaxed'  SSLContext with no trust store (all certificates are valids)
    public static SSLContext getInstance(String protocol)
            throws KeyManagementException, NoSuchAlgorithmException
    {
        SSLContext sc = SSLContext.getInstance(protocol);
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        return sc;
    }
}