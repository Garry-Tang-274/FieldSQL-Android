import dev.fieldsql.db.*;
import java.nio.file.*;
import java.util.*;
public class RemoteSmoke {
    static void eq(boolean b,String m){ if(!b)throw new AssertionError(m); }
    static void fail(String name,Run r)throws Exception{try{r.go();}catch(Exception expected){System.out.println("PASS rejected "+name+": "+expected.getClass().getSimpleName());return;}throw new AssertionError(name+" was accepted");}
    interface Run{void go()throws Exception;}
    public static void main(String[] a)throws Exception{
        int pg=Integer.parseInt(a[0]), my=Integer.parseInt(a[1]),oracle=Integer.parseInt(a[2]),sh=Integer.parseInt(a[3]);String pass=System.getenv("FIELD_TEST_PASS");
        byte[] key=Files.readAllBytes(Paths.get(a[4]));
        Databases.SshConfig ssh=new Databases.SshConfig("127.0.0.1",sh,"qa",key,null,a[5]);
        for(String kind:new String[]{"postgres","mariadb","mysql"}){
            int port=kind.equals("postgres")?pg:kind.equals("mysql")?oracle:my;String user=kind.equals("postgres")?"fieldsql":"root",db=kind.equals("postgres")?"postgres":"fieldsql_qa";
            for(int mode:new int[]{1,2,0}){
                Databases.SshConfig cfg=mode==2?ssh:null;
                try(Database d=Databases.openRemote(kind,"localhost",port,db,user,pass,mode!=0,cfg)){
                    d.execute("DROP TABLE IF EXISTS fieldsql_smoke");
                    d.execute("CREATE TABLE fieldsql_smoke (id INTEGER, sub INTEGER, note VARCHAR(80), PRIMARY KEY(id,sub))");
                    d.execute("INSERT INTO fieldsql_smoke VALUES (1,1,'甲'),(1,2,'乙'),(2,1,NULL)");
                    eq(d.tables().contains("fieldsql_smoke"),"list tables");
                    eq(d.describe("fieldsql_smoke").rows.size()>=4,"describe");
                    eq(d.browse("fieldsql_smoke",0,2).truncated,"paging");
                    eq(d.execute("SELECT note FROM fieldsql_smoke ORDER BY id, sub",2).truncated,"query limit");
                    eq(d.update("fieldsql_smoke",Map.of("note","O'Brien"),Map.of("id","1","sub","2"))==1,"edit");
                    eq("O'Brien".equals(d.execute("SELECT note FROM fieldsql_smoke WHERE sub=2").rows.get(0).get(0)),"roundtrip");
                    fail("non-unique key",()->d.update("fieldsql_smoke",Map.of("note","bad"),Map.of("id","1")));
                    eq(d.execute("SELECT note FROM fieldsql_smoke WHERE id=2").rows.get(0).get(0)==null,"SQL NULL");
                    d.execute("BEGIN");d.execute("UPDATE fieldsql_smoke SET note='rollback'");d.execute("ROLLBACK");eq("O'Brien".equals(d.execute("SELECT note FROM fieldsql_smoke WHERE sub=2").rows.get(0).get(0)),"transaction rollback");
                    d.setQueryTimeoutSeconds(1);long now=System.nanoTime();boolean cancelled=false;
                    try {Database.Result slow=d.execute(kind.equals("postgres")?"SELECT pg_sleep(4)":"SELECT SLEEP(4)");cancelled=!slow.rows.isEmpty()&&"1".equals(slow.rows.get(0).get(0));}
                    catch(java.sql.SQLException ex){cancelled=true;}
                    long elapsed=(System.nanoTime()-now)/1000000;
                    System.out.println("DEADLINE "+kind+"/"+mode+" cancelled="+cancelled+" ms="+elapsed);
                    eq(cancelled&&elapsed<3200,"query deadline");d.setQueryTimeoutSeconds(60);eq("9".equals(d.execute("SELECT 9").rows.get(0).get(0)),"session after timeout");
                    d.execute("DROP TABLE fieldsql_smoke");
                    System.out.println("PASS "+kind+" "+new String[]{"TCP explicit plaintext","TLS verified","SSH pinned + TLS verified"}[mode]+" metadata/create/read/limit/update");
                }
            }
            fail(kind+" TLS wrong name",()->Databases.openRemote(kind,"127.0.0.1",port,db,user,pass,true).close());
            fail(kind+" SSH TLS wrong name",()->Databases.openRemote(kind,"127.0.0.1",port,db,user,pass,true,ssh).close());
        }
        if(a.length>6&&"hold".equals(a[6])){
            for(String kind:new String[]{"postgres","mariadb","mysql"}){
                boolean isPg=kind.equals("postgres");int port=isPg?pg:kind.equals("mysql")?oracle:my;
                try(Database d=Databases.openRemote(kind,"localhost",port,isPg?"postgres":"fieldsql_qa",isPg?"fieldsql":"root",pass,true)){
                    d.execute("CREATE TABLE qa_catalog(id INTEGER PRIMARY KEY,title VARCHAR(60) NOT NULL)");
                    d.execute("INSERT INTO qa_catalog VALUES(7,'Android JDBC fixture')");
                    if(isPg)d.execute("CREATE VIEW qa_titles AS SELECT title FROM qa_catalog");
                }
            }
        }
        Databases.SshConfig wrong=new Databases.SshConfig("127.0.0.1",sh,"qa",key,null,"SHA256:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        fail("SSH host key change",()->Databases.openRemote("postgres","localhost",pg,"postgres","fieldsql",pass,true,wrong).close());
    }
}





