import dev.fieldsql.db.*;
import com.jcraft.jsch.JSch;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
public final class Ed25519Smoke {
    public static void main(String[] args)throws Exception{
        String impl=JSch.getConfig("ssh-ed25519");
        if(!impl.contains(".bc."))throw new AssertionError("Not the Android-compatible JSch/BC implementation: "+impl);
        Databases.SshConfig ssh=new Databases.SshConfig("127.0.0.1",Integer.parseInt(args[0]),"qa",
              Files.readAllBytes(Paths.get(args[3])),System.getenv("FIELD_TEST_KEYPASS").getBytes(StandardCharsets.UTF_8),args[2]);
        try(Database d=Databases.openRemote("postgres","localhost",Integer.parseInt(args[1]),"postgres","fieldsql",System.getenv("FIELD_TEST_PASS"),true,ssh)){
            String result=d.execute("SELECT title FROM qa_catalog WHERE id=7").rows.get(0).get(0);
            if(!"Android JDBC fixture".equals(result))throw new AssertionError(result);
            System.out.println("PASS public and encrypted-private Ed25519 keys via Java-8 JSch/BC + verified database TLS");
        }
    }
}
