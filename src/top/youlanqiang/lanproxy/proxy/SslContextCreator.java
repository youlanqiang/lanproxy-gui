package top.youlanqiang.lanproxy.proxy;

import org.fengfei.lanproxy.common.Config;


import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class SslContextCreator {


    public static SSLContext createSSLContext() {
        return new SslContextCreator().initSSLContext();
    }

    public SSLContext initSSLContext() {
        final String jksPath = Config.getInstance().getStringValue("ssl.jksPath");
        if (jksPath == null || jksPath.isEmpty()) {
            // key_store_password or key_manager_password are empty
            return null;
        }

        // if we have the port also the jks then keyStorePassword and
        // keyManagerPassword
        // has to be defined
        final String keyStorePassword = Config.getInstance().getStringValue("ssl.keyStorePassword");
        // if client authentification is enabled a trustmanager needs to be
        // added to the ServerContext

        try {
            InputStream jksInputStream = jksDatastore(jksPath);
            SSLContext clientSSLContext = SSLContext.getInstance("TLS");
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(jksInputStream, keyStorePassword.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            TrustManager[] trustManagers = tmf.getTrustManagers();

            // init sslContext
           clientSSLContext.init(null, trustManagers, null);

            return clientSSLContext;
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | KeyManagementException
                | IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private InputStream jksDatastore(String jksPath) throws FileNotFoundException {
        URL jksUrl = getClass().getClassLoader().getResource(jksPath);
        if (jksUrl != null) {
            return getClass().getClassLoader().getResourceAsStream(jksPath);
        }

        File jksFile = new File(jksPath);
        if (jksFile.exists()) {
            return new FileInputStream(jksFile);
        }

        return null;
    }
}