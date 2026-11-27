package dev.fieldsql.db;

import java.net.IDN;
import java.net.InetAddress;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import org.postgresql.ssl.PGjdbcHostnameVerifier;

/** SAN-only verifier. No CN/JNDI lookup; DNS wildcards cover one complete left-most label. */
public final class DatabaseHostname implements HostnameVerifier {
    public boolean verify(String host,SSLSession session){
        try{return matches(host,(X509Certificate)session.getPeerCertificates()[0]);}
        catch(Exception invalid){return false;}
    }
    static boolean matches(String host,X509Certificate cert)throws Exception{
        if(host.startsWith("[")&&host.endsWith("]"))host=host.substring(1,host.length()-1);
        boolean numeric=host.contains(":")||host.matches("[0-9.]+");
        byte[] ip=numeric?address(host):null;
        if(numeric&&ip==null)return false;
        String dns=numeric?null:dns(host);
        Collection<List<?>> sans=cert.getSubjectAlternativeNames();
        if(sans==null)return false;
        for(List<?> san:sans){
            if(san==null||san.size()<2)continue;
            Object value=san.get(1);
            if(numeric&&Integer.valueOf(7).equals(san.get(0))){
                byte[] other=value instanceof byte[]?(byte[])value:value instanceof String?address((String)value):null;
                if(Arrays.equals(ip,other))return true;
            }else if(!numeric&&Integer.valueOf(2).equals(san.get(0))&&value instanceof String){
                String pattern=(String)value;
                try{
                    if(pattern.startsWith("*.")){
                        String suffix=dns(pattern.substring(2));
                        if(suffix.indexOf('.')<0)continue; // e.g. *.com is not a server identity.
                        pattern="*."+suffix;
                    }else pattern=dns(pattern);
                    if(PGjdbcHostnameVerifier.INSTANCE.verifyHostName(dns,pattern))return true;
                }catch(IllegalArgumentException malformed){/* Invalid SAN is not an identity match. */}
            }
        }
        return false;
    }
    private static String dns(String s){
        String r=s.endsWith(".")?s.substring(0,s.length()-1):s;
        if(r.isEmpty()||r.contains("*"))throw new IllegalArgumentException("not a literal DNS name");
        return IDN.toASCII(r,IDN.USE_STD3_ASCII_RULES);
    }
    /** Numeric conversion only: a certificate entry can never trigger DNS resolution. */
    private static byte[] address(String s){
        try{
            if(s.contains(":"))return s.matches("[0-9A-Fa-f:.]+")?InetAddress.getByName(s).getAddress():null;
            String[] labels=s.split("\\.",-1);if(labels.length!=4)return null;byte[] bytes=new byte[4];
            for(int i=0;i<4;i++){String l=labels[i];if(!l.matches("0|[1-9][0-9]{0,2}"))return null;int v=Integer.parseInt(l);if(v>255)return null;bytes[i]=(byte)v;}
            return bytes;
        }catch(Exception malformed){return null;}
    }
}
