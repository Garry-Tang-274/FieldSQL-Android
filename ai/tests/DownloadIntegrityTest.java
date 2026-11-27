package dev.fieldsql.ai;
import java.nio.file.Files;
import java.nio.file.Path;
/** The promotion guard must reject wrong bytes even when the size is unchanged. */
public final class DownloadIntegrityTest {
    public static void main(String[] a)throws Exception{
        Path f=Files.createTempFile("fieldsql-integrity-", ".bin");
        try{
            String abc="ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
            Files.write(f,new byte[]{'a','b','c'});
            if(!ModelDownloads.authentic(f.toFile(),abc))throw new AssertionError("valid payload rejected");
            Files.write(f,new byte[]{'c','b','a'});
            if(ModelDownloads.authentic(f.toFile(),abc))throw new AssertionError("altered payload accepted");
            System.out.println("Transfer integrity: expected bytes accepted; same-size altered bytes rejected.");
        }finally{Files.delete(f);}
    }
}
