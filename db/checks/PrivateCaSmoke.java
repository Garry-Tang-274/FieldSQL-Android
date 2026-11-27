import dev.fieldsql.db.Database;
import dev.fieldsql.db.Databases;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;

/** Real driver connections. No passwords or server key material are logged. */
public class PrivateCaSmoke {
    interface Attempt {void run()throws Exception;}
    static void eq(boolean b,String m){if(!b)throw new AssertionError(m);}
    static void reject(Attempt a,String label)throws Exception{
        try{a.run();}catch(Exception expected){return;}throw new AssertionError("expected rejection: "+label);
    }
    static void tlsReject(Attempt a,String label)throws Exception{
        try{a.run();}
        catch(SQLException e){
            StringBuilder reasons=new StringBuilder();for(Throwable t=e;t!=null;t=t.getCause())reasons.append(' ').append(t.getMessage());
            String msg=reasons.toString().toLowerCase(Locale.ROOT);
            eq(msg.matches("(?s).*(certifi|pkix|trust anchor|hostname|handshake|ssl verification).*"),label+": failure was not certificate/hostname validation");return;
        }
        throw new AssertionError("TLS accepted "+label);
    }
    static byte[] cat(byte[]... list)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();for(byte[] a:list)o.write(a);return o.toByteArray();}
    static void one(Database d)throws Exception{eq("1".equals(d.execute("SELECT 1").rows.get(0).get(0)),"row readback");}
    public static void main(String[] args)throws Exception{
        Path dir=Paths.get(args[0]);byte[] ca=Files.readAllBytes(dir.resolve("ca.pem")),der=Files.readAllBytes(dir.resolve("ca.der")),wrong=Files.readAllBytes(dir.resolve("wrong-ca.pem"));
        byte[] key=Files.readAllBytes(dir.resolve("ssh.pem")),mixed=cat(wrong,ca);
        String pw=System.getenv("FIELD_TEST_PASS");boolean system=args[6].equals("system");
        Databases.SshConfig ssh=new Databases.SshConfig("127.0.0.1",Integer.parseInt(args[4]),"qa",key,null,args[5]);
        if(!system){
            eq(Arrays.equals(ca,Databases.normalizeCa(der)),"DER to reproducible PEM");eq(Arrays.equals(mixed,Databases.normalizeCa(mixed)),"two CAs");
            byte[] crlf=new String(ca,StandardCharsets.US_ASCII).replace("\n","\r\n").getBytes(StandardCharsets.US_ASCII);
            eq(Arrays.equals(ca,Databases.normalizeCa(cat(new byte[]{(byte)239,(byte)187,(byte)191},crlf))),"BOM/CRLF");
            for(byte[] bad:new byte[][]{new byte[0],"not a cert".getBytes(StandardCharsets.UTF_8),key,cat(ca,key),cat(ca,new byte[]{63}),cat(der,new byte[]{0}),new byte[262145],Files.readAllBytes(dir.resolve("leaf.pem"))})
                reject(()->Databases.normalizeCa(bad),"invalid CA material");
            reject(()->Databases.openRemote("mysql","localhost",1,"db","x","x",false,null,ca),"custom CA with TLS disabled");
            System.out.println("PASS parser: PEM, bundle, DER, BOM/CRLF; rejected private/mixed, leaf, junk, oversized and trailing input");
        }
        int i=0;
        for(String kind:new String[]{"postgres","mariadb","mysql"}){
            int port=Integer.parseInt(args[++i]);boolean pg=kind.equals("postgres");String db=pg?"postgres":"fieldsql_qa",user=pg?"fieldsql":"root";
            if(system){try(Database d=Databases.openRemote(kind,"localhost",port,db,user,pw,true)){one(d);}
                tlsReject(()->Databases.openRemote(kind,"localhost",port,db,user,pw,true,null,wrong).close(),"custom wrong CA must not borrow trusted system certificate");
                System.out.println("PASS "+kind+" legacy system trust succeeds; explicit unrelated CA remains isolated");continue;}
            for(Databases.SshConfig route:new Databases.SshConfig[]{null,ssh}){
                try(Database good=Databases.openRemote(kind,"localhost",port,db,user,pw,true,route,route==null?mixed:der)){
                    one(good);
                    tlsReject(()->Databases.openRemote(kind,"localhost",port,db,user,pw,true,route,wrong).close(),"wrong CA");
                    tlsReject(()->Databases.openRemote(kind,"localhost",port,db,user,pw,true,route).close(),"default must not inherit sibling's custom CA");
                    tlsReject(()->Databases.openRemote(kind,"127.0.0.1",port,db,user,pw,true,route,ca).close(),"certificate valid for different host");
                    if(!pg){
                        good.setQueryTimeoutSeconds(1);long before=System.nanoTime();boolean interrupted=false;
                        try{Database.Result slow=good.execute("SELECT SLEEP(5)");interrupted="1".equals(slow.rows.get(0).get(0));}
                        catch(SQLException e){interrupted=true;}
                        eq(interrupted&&(System.nanoTime()-before)<3500000000L,"custom-CA control connection deadline");good.setQueryTimeoutSeconds(60);
                    }
                    one(good);
                }
                System.out.println("PASS "+kind+(route==null?" direct":" via SSH")+" private-root chain, host match/rejection, wrong/default CA, independent connection"+(pg?"":" and cancellation"));
            }
        }
    }
}
