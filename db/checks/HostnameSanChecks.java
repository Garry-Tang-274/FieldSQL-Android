package dev.fieldsql.db;
import java.nio.file.*;
import java.security.cert.*;
import java.io.*;
public class HostnameSanChecks {
    static void check(Path p,String name,String host,boolean want)throws Exception{
        try(InputStream in=Files.newInputStream(p.resolve(name+".der"))){
            X509Certificate c=(X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(in);
            if(DatabaseHostname.matches(host,c)!=want)throw new AssertionError(name+" / "+host);
        }
    }
    public static void main(String[] a)throws Exception{
        Path p=Paths.get(a[0]);check(p,"absent","db.example.test",false);check(p,"mismatch","db.example.test",false);
        check(p,"mismatch","other.example.test",true);check(p,"numericdns","10.0.2.2",false);check(p,"ip","10.0.2.2",true);
        check(p,"ip","10.0.2.3",false);check(p,"v6","[2001:db8:0000::1]",true);check(p,"wild","ABC.example.test",true);
        check(p,"wild","example.test",false);check(p,"wild","a.b.example.test",false);
        System.out.println("PASS SAN verification: correct CN ignored for missing/mismatched SAN; distinct DNS/IP, IPv6, one-label DNS wildcard");
    }
}
