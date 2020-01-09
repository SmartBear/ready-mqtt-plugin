package com.smartbear.mqttsupport.connection.ssl;

import com.eviware.soapui.support.StringUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class SSLCertsHelper {
    private static final String SECURITY_PROVIDER = "BC";

    public static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile,
                                                    final String password,
                                                    String sniHost) throws Exception {

        Security.addProvider(new BouncyCastleProvider());
        JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter();

        // load CA certificate
        PEMParser caCertificateReader = new PEMParser(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(caCrtFile)))));
        X509CertificateHolder caCertHolder = (X509CertificateHolder) caCertificateReader.readObject();
        X509Certificate caCertificate = certificateConverter.setProvider(SECURITY_PROVIDER).getCertificate(caCertHolder);
        caCertificateReader.close();

        // load client certificate
        PEMParser clientCertificateReader = new PEMParser(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(crtFile)))));
        X509CertificateHolder clientCertificateHolder = (X509CertificateHolder) clientCertificateReader.readObject();
        X509Certificate clientCertificate = certificateConverter.setProvider(SECURITY_PROVIDER).getCertificate(clientCertificateHolder);
        clientCertificateReader.close();

        // load client private key
        PEMParser clientPrivateKeyReader = new PEMParser(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(keyFile)))));
        Object encryptedKey = clientPrivateKeyReader.readObject();
        clientPrivateKeyReader.close();
        PEMDecryptorProvider decryptorProvider = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(SECURITY_PROVIDER);

        PrivateKey privateKey;
        if (encryptedKey instanceof PEMEncryptedKeyPair) {
            KeyPair keyPair = converter.getKeyPair(((PEMEncryptedKeyPair) encryptedKey).decryptKeyPair(decryptorProvider));
            privateKey = keyPair.getPrivate();
        } else if (encryptedKey instanceof PrivateKeyInfo) {
            privateKey = converter.getPrivateKey((PrivateKeyInfo) encryptedKey);
        } else {
            KeyPair keyPair = converter.getKeyPair((PEMKeyPair) encryptedKey);
            privateKey = keyPair.getPrivate();
        }
        
        // CA certificate is used to authenticate server
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", caCertificate);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(caKs);

        // client key and certificates are sent to server so it can authenticate us
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("certificate", clientCertificate);
        ks.setKeyEntry("private-key", privateKey, password.toCharArray(), new java.security.cert.Certificate[]{clientCertificate});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());

        // finally, create SSL socket factory
        SSLContext context = SSLContext.getInstance("TLSv1");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        if (!StringUtils.isNullOrEmpty(sniHost)) {
            List<SNIServerName> serverNames = new ArrayList<>();
            SNIServerName serverName = new SNIHostName(sniHost);
            serverNames.add(serverName);

            return new SSLSocketFactoryWrapper(context.getSocketFactory(), serverNames);
        } else {
            return context.getSocketFactory();
        }
    }
}
