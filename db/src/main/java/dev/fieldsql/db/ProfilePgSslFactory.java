package dev.fieldsql.db;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.postgresql.ssl.WrappedFactory;

/** pgJDBC constructs this per connection with its Properties. verify-full still checks the real database hostname. */
public final class ProfilePgSslFactory extends WrappedFactory {
    static final String PEM_PROPERTY="fieldsqlPrivateCaPem";
    public ProfilePgSslFactory(Properties settings)throws Exception{
        String pem=settings.getProperty(PEM_PROPERTY);
        if(pem==null)throw new CertificateException("此连接没有可用的私有 CA");
        KeyStore trust=KeyStore.getInstance(KeyStore.getDefaultType());trust.load(null,null);
        int n=0;for(X509Certificate c:PrivateCa.parse(pem.getBytes(StandardCharsets.US_ASCII)))
            trust.setCertificateEntry("profile-ca-"+(++n),c);
        TrustManagerFactory tmf=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trust);SSLContext ctx=SSLContext.getInstance("TLS");ctx.init(null,tmf.getTrustManagers(),null);
        factory=ctx.getSocketFactory();
    }
}
