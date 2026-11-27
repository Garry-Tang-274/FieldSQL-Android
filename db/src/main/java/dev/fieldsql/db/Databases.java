package dev.fieldsql.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Databases {
    private Databases(){}
    public static final class SshConfig {
        public final String host,user,hostKeySha256; public final int port; public final byte[] privateKey,keyPassphrase;
        public SshConfig(String host,int port,String user,byte[] privateKey,byte[] keyPassphrase,String hostKeySha256){
            this.host=host;this.port=port;this.user=user;this.privateKey=privateKey;this.keyPassphrase=keyPassphrase;this.hostKeySha256=hostKeySha256;
        }
    }
    public static byte[] readSshKey(Context ctx,Uri src)throws IOException{
        try(InputStream in=ctx.getContentResolver().openInputStream(src);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(in==null)throw new IOException("私钥文件不可读");byte[] b=new byte[4096];int k;
            while((k=in.read(b))!=-1){if(out.size()+k>256*1024)throw new IOException("所选文件超过私钥大小上限 256 KiB");out.write(b,0,k);}
            return out.toByteArray();
        }
    }
    static String name(String n){if(n==null)n="";n=n.trim();if(n.length()<1||n.length()>80||n.startsWith(".")||n.matches("(?s).*[\\p{Cntrl}\\\\/:*?\"<>|].*"))throw new IllegalArgumentException("库名须为 1–80 字，不能以点开头或包含路径、控制字符");return n;}
    private static File dir(Context c){return c.getDir("fieldsql_dbs",Context.MODE_PRIVATE);}
    private static SQLiteDatabase sqlite(File f,int flags){return SQLiteDatabase.openDatabase(f.toString(),null,flags, corrupt -> { throw new SQLiteDatabaseCorruptException("SQLite 文件损坏；已保留原文件"); });}
    public static List<String> listLocal(Context ctx){File[] files=dir(ctx).listFiles();List<String> r=new ArrayList<>();if(files!=null)for(File f:files)if(f.isFile()&&f.getName().endsWith(".sqlite"))r.add(f.getName().substring(0,f.getName().length()-7));Collections.sort(r);return r;}
    public static synchronized Database openLocal(Context ctx,String name){
        return new LocalDb(sqlite(new File(dir(ctx),name(name)+".sqlite"),SQLiteDatabase.OPEN_READWRITE|SQLiteDatabase.CREATE_IF_NECESSARY));
    }
    public static synchronized Database importLocal(Context ctx,String name,Uri uri)throws IOException{
        File dir=dir(ctx),dest=new File(dir,name(name)+".sqlite");if(dest.exists())throw new IOException("同名数据库已存在，请换一个库名");
        File tmp=File.createTempFile(".import-",".part",dir);
        try{
            try(InputStream in=ctx.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)){
                if(in==null)throw new IOException("无法读取所选文件");
                byte[] header=new byte[16];new DataInputStream(in).readFully(header);
                if(!Arrays.equals(header,"SQLite format 3\0".getBytes(StandardCharsets.US_ASCII)))throw new IOException("不是普通 SQLite 3 文件（不支持加密数据库）");
                out.write(header);byte[] b=new byte[65536];int k;while((k=in.read(b))!=-1)out.write(b,0,k);
            }
            try(SQLiteDatabase probe=sqlite(tmp,SQLiteDatabase.OPEN_READONLY);Cursor schema=probe.rawQuery("SELECT count(*) FROM sqlite_master",null)){schema.moveToFirst();schema.getLong(0);}
            if(!tmp.renameTo(dest))throw new IOException("保存导入副本失败");
            return openLocal(ctx,name);
        }finally{if(tmp.exists())tmp.delete();}
    }
    public static Database openRemote(String kind,String host,int port,String database,String user,String password,boolean tls)throws Exception{return openRemote(kind,host,port,database,user,password,tls,null);}
    public static Database openRemote(String kind,String host,int port,String database,String user,String password,boolean tls,SshConfig ssh)throws Exception{return Remote.open(kind,host,port,database,user,password,tls,ssh);}
}
