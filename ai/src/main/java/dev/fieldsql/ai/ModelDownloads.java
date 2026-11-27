package dev.fieldsql.ai;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import java.io.*;
import java.security.*;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;

/** One app-owned system download. No SQL data crosses this boundary. */
final class ModelDownloads {
    enum Phase { IDLE, QUEUED, RUNNING, PAUSED, VERIFYING, FAILED }
    static final int BAD_FILE=-201, LOST_FILE=-202, STORAGE=-203, SERVICE=-204;
    static final String[] FILES={"catalog-qwen2.5-coder-1.5b-q4_k_m.gguf","catalog-qwen2.5-coder-0.5b-q4_k_m.gguf"};
    // Git LFS object IDs at the two published, pinned Qwen revisions.
    static final String[] DIGESTS={"cc324af070c2ecbfd324a30884d2f951a7ff756aba85cb811a6ec436933bb046","1d9614638d18024d0fbb36575a15f1302a3adf044df10345688ec4f6e1c4ff32"};
    static final class Snapshot {
        final Phase phase; final int index,reason; final long bytes;
        Snapshot(Phase p,int i,long n,int r){phase=p;index=i;bytes=n;reason=r;}
    }
    private static ModelDownloads instance;
    static synchronized ModelDownloads get(Context c){if(instance==null)instance=new ModelDownloads(c.getApplicationContext());return instance;}
    static File directory(Context c){return c.getExternalFilesDir("FieldSQLModels");}
    private final Context ctx;private final DownloadManager manager;private final SharedPreferences jobs, models;
    private final Executor verifyWorker=Executors.newSingleThreadExecutor();
    private long verifying=-1;
    private ModelDownloads(Context c){ctx=c;manager=(DownloadManager)c.getSystemService(Context.DOWNLOAD_SERVICE);jobs=c.getSharedPreferences("fieldsql_download",0);models=c.getSharedPreferences("fieldsql_ai",0);}
    synchronized void start(int index)throws IOException {
        if(index<0||index>=FILES.length)throw new IllegalArgumentException("model");
        if(manager==null)throw new IOException("DownloadManager");
        if(jobs.getLong("id",-1)>=0)throw new IOException("Download already queued");
        File dir=directory(ctx);
        if(dir==null||!dir.isDirectory())throw new IOException("STORAGE");
        File existing=new File(dir,FILES[index]);
        if(existing.isFile()&&existing.length()==LocalModels.SIZES[index]){activate(existing,index);return;}
        if(dir.getUsableSpace()<LocalModels.SIZES[index])throw new IOException("STORAGE");
        String stage="fetch-"+java.util.UUID.randomUUID()+".part";
        DownloadManager.Request req=new DownloadManager.Request(Uri.parse(LocalModels.URLS[index]));
        req.setTitle(LocalModels.NAMES[index]).setDescription("FieldSQL · GGUF").setMimeType("application/octet-stream")
            .setAllowedOverMetered(true).setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .addRequestHeader("Accept-Encoding","identity").setDestinationInExternalFilesDir(ctx,"FieldSQLModels",stage);
        long id=manager.enqueue(req);
        if(!jobs.edit().clear().putLong("id",id).putInt("index",index).putString("stage",stage).commit()){
            manager.remove(id);throw new IOException("STORAGE");
        }
    }
    synchronized Snapshot poll(){
        long id=jobs.getLong("id",-1);int i=jobs.getInt("index",0);
        if(id<0)return new Snapshot(Phase.IDLE,i,0,0);
        if(verifying==id)return new Snapshot(Phase.VERIFYING,i,LocalModels.SIZES[i],0);
        int fail=jobs.getInt("fail",0);if(fail!=0)return new Snapshot(Phase.FAILED,i,0,fail);
        try(Cursor c=manager.query(new DownloadManager.Query().setFilterById(id))){
            if(c==null||!c.moveToFirst())return new Snapshot(Phase.FAILED,i,0,LOST_FILE);
            int s=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            long n=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            int reason=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
            if(s==DownloadManager.STATUS_SUCCESSFUL){
                verifying=id;String stage=jobs.getString("stage","");
                verifyWorker.execute(()->finish(id,i,stage));return new Snapshot(Phase.VERIFYING,i,n,0);
            }
            Phase phase=s==DownloadManager.STATUS_FAILED?Phase.FAILED:s==DownloadManager.STATUS_PAUSED?Phase.PAUSED:
                s==DownloadManager.STATUS_PENDING?Phase.QUEUED:Phase.RUNNING;
            return new Snapshot(phase,i,Math.max(0,n),reason);
        }catch(RuntimeException e){return new Snapshot(Phase.FAILED,i,0,SERVICE);}
    }
    private void finish(long id,int i,String stageName){
        int failure=0;
        try{
            File dir=directory(ctx);if(dir==null)throw new IOException("STORAGE");
            File part=new File(dir,stageName),target=new File(dir,FILES[i]);
            // Recovery for a process death between rename and committing the selection.
            File candidate=part.isFile()?part:target;
            if(!candidate.isFile())failure=LOST_FILE;
            else if(candidate.length()!=LocalModels.SIZES[i]||!authentic(candidate,DIGESTS[i]))failure=BAD_FILE;
            else{
                synchronized(this){
                    if(jobs.getLong("id",-1)!=id)return;
                    if(candidate.equals(part)&&!part.renameTo(target))throw new IOException("STORAGE");
                    activate(target,i);jobs.edit().clear().commit();
                }
                try{manager.remove(id);}catch(RuntimeException ignored){/* completed .part path no longer exists */}
            }
        }catch(Exception e){failure=STORAGE;}
        finally{synchronized(this){if(failure!=0&&jobs.getLong("id",-1)==id)jobs.edit().putInt("fail",failure).apply();if(verifying==id)verifying=-1;}}
    }
    private void activate(File f,int i)throws IOException{
        if(!models.edit().putString("active",f.getName()).putString("label:"+f.getName(),LocalModels.NAMES[i]+" · Q4_K_M").commit())throw new IOException("STORAGE");
    }
    static boolean authentic(File file,String expected)throws IOException,NoSuchAlgorithmException {
        MessageDigest sha=MessageDigest.getInstance("SHA-256");byte[] b=new byte[512*1024];
        try(InputStream in=new BufferedInputStream(new FileInputStream(file),b.length)){
            int n;while((n=in.read(b))!=-1)sha.update(b,0,n);
        }
        StringBuilder hex=new StringBuilder(64);for(byte x:sha.digest())hex.append(String.format(Locale.ROOT,"%02x",x&255));return hex.toString().equals(expected);
    }
    synchronized void discard(){
        long id=jobs.getLong("id",-1);if(id<0||id==verifying)return;
        manager.remove(id);File dir=directory(ctx);String stage=jobs.getString("stage",null);
        if(dir!=null&&stage!=null)new File(dir,stage).delete();
        jobs.edit().clear().commit();
    }
}
