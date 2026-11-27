package dev.fieldsql.db;

import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import javax.net.ssl.*;
import org.mariadb.jdbc.internal.protocol.tls.DefaultTlsSocketPlugin;
import org.mariadb.jdbc.util.Options;

/** Connector/J plugin, created anew for every handshake. Uses the original PKIX trust manager,
 * but never the desktop-only javax.naming fallback. Verification precedes database authentication. */
public final class MariaVerifiedTls extends DefaultTlsSocketPlugin {
    static final String TYPE="fieldsql-verified-tls", EXPECTED="fieldsqlExpectedTlsHost";
    private String expected;
    @Override public String name(){return "FieldSQL verified TLS";}
    @Override public String type(){return TYPE;}
    @Override public SSLSocketFactory getSocketFactory(Options opts)throws SQLException{
        expected=opts.nonMappedOptions.getProperty(EXPECTED);
        if(expected==null||expected.isEmpty())throw new SQLException("Missing expected database TLS hostname","08006");
        return super.getSocketFactory(opts);
    }
    @Override public SSLSocket createSocket(Socket socket,SSLSocketFactory factory)throws IOException{
        // JSSE receives the database DNS name (for SNI), not the IP of a loopback SSH listener.
        return (SSLSocket)factory.createSocket(socket,expected,socket.getPort(),true);
    }
    @Override public void verify(String host,SSLSession session,Options options,long threadId)throws SSLException{
        if(!expected.equals(host)||!new DatabaseHostname().verify(host,session))
            throw new SSLPeerUnverifiedException("Database hostname does not match certificate subjectAltName (SAN): "+host);
    }
}
