package dev.fieldsql.ai;

import android.content.ContentResolver;
import android.os.Build;
import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import java.io.*;
import java.util.*;

/** The only network path is an explicit model download. Inference never starts a server. */
final class LocalModels {
    static final String[] NAMES = {"Qwen2.5 Coder · 1.5B", "Qwen2.5 Coder · 0.5B"};
    static final long[] SIZES = {1117320768L, 491400064L};
    static final String[] URLS = {
        "https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/f86cb2c1fa58255f8052cc32aeede1b7482d4361/qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
        "https://huggingface.co/Qwen/Qwen2.5-Coder-0.5B-Instruct-GGUF/resolve/ebb2015119c907b064c512bf053e945850b5875f/qwen2.5-coder-0.5b-instruct-q4_k_m.gguf"
    };
    interface Job { boolean cancelled(); void progress(String s); }
    final Context ctx;
    final File dir;
    final AiText labels;
    private String t(String zh){return labels.t(zh);}
    LocalModels(Context ctx, AiText labels) {
        this.labels=labels;
        this.ctx=ctx; dir=new File(ctx.getNoBackupFilesDir(),"models");
        if (!dir.exists()) dir.mkdirs();
    }
    File runner() { return new File(ctx.getApplicationInfo().nativeLibraryDir,"libfieldsql-runner.so"); }
    String unsupported() {
        if (Build.VERSION.SDK_INT<28) return t("端侧推理需要 Android 9 或更高版本。");
        if (!Arrays.asList(Build.SUPPORTED_ABIS).contains("arm64-v8a") || !runner().canExecute()) return t("当前设备未提供可用的 ARM64 推理引擎。");
        return null;
    }
    File[] files() {
        ArrayList<File> found=new ArrayList<>();
        File[] imports=dir.listFiles((d,n)->n.endsWith(".gguf"));
        if(imports!=null)Collections.addAll(found,imports);
        File downloaded=ModelDownloads.directory(ctx);
        if(downloaded!=null)for(String n:ModelDownloads.FILES){File f=new File(downloaded,n);if(f.isFile())found.add(f);}
        found.sort(Comparator.comparingLong(File::lastModified).reversed());return found.toArray(new File[0]);
    }
    String label(File file) {return ctx.getSharedPreferences("fieldsql_ai",0).getString("label:"+file.getName(),file.getName());}
    File selected() {
        String name=ctx.getSharedPreferences("fieldsql_ai",0).getString("active","");
        for (File f:files()) if(f.getName().equals(name))return f;
        File[] f=files();return f.length==0?null:f[0];
    }
    void select(File f){ctx.getSharedPreferences("fieldsql_ai",0).edit().putString("active", f.getName()).apply();}
    void remove(File f)throws IOException{
        if (!f.delete()) throw new IOException(t("未能删除模型。"));
        ctx.getSharedPreferences("fieldsql_ai",0).edit().remove("label:"+f.getName()).apply();
    }
    void importUri(Uri uri,Job job)throws IOException{
        ContentResolver r=ctx.getContentResolver();String name=t("本地 GGUF");long size=-1;
        try(Cursor c=r.query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){
            if(c!=null && c.moveToFirst()){if(!c.isNull(0))name=c.getString(0);if(!c.isNull(1))size=c.getLong(1);}
        }
        try(InputStream in=r.openInputStream(uri)){if(in==null)throw new IOException(t("无法打开所选文件。"));store(in,size,name,job);}
    }
    static String mib(long n){return String.format(Locale.US,"%.0f MiB",n/1048576.0);}
    void store(InputStream in,long expected,String name,Job job)throws IOException{
        if(expected>0 && expected>dir.getUsableSpace())throw new IOException(t("空间不足，导入该模型至少需要 ")+mib(expected)+"。");
        String id=UUID.randomUUID().toString(); File partial=new File(dir,id+".part"); File dest=new File(dir,id+".gguf");
        try{
            DataInputStream data=new DataInputStream(new BufferedInputStream(in,128*1024));
            byte[] head=new byte[24]; data.readFully(head);
            if(head[0]!='G' || head[1]!='G'||head[2]!='U'||head[3]!='F'||(head[4]!=2 && head[4]!=3)||head[5]!=0||head[6]!=0||head[7]!=0)
                throw new IOException(t("所选文件不是受支持的 GGUF v2/v3 模型。"));
            long n=24,last=0;byte[] buf=new byte[128*1024];
            try(OutputStream out=new BufferedOutputStream(new FileOutputStream(partial), 128*1024)){
                out.write(head);
                for(int read;(read=data.read(buf))!=-1;){
                    if(job.cancelled())throw new InterruptedIOException(t("已停止。"));
                    out.write(buf,0,read);n+=read;
                    if(System.nanoTime()-last>350000000L){job.progress(mib(n)+(expected>0?" / "+mib(expected):""));last=System.nanoTime();}
                }
            }
            if(job.cancelled())throw new InterruptedIOException(t("已停止。"));
            if(expected>0 && n!=expected)throw new IOException(t("传输不完整，请重新下载或导入。"));
            if(!partial.renameTo(dest))throw new IOException(t("无法保存模型。"));
            ctx.getSharedPreferences("fieldsql_ai",0).edit().putString("label:"+dest.getName(),name).putString("active",dest.getName()).apply();
        }finally{if(partial.exists())partial.delete();}
    }
    Process start(File model,String prompt,int threads)throws IOException {
        ProcessBuilder pb=new ProcessBuilder(SqlPrompt.command(runner().getAbsolutePath(),model.getAbsolutePath(),prompt,threads));
        pb.directory(runner().getParentFile()); pb.environment().put("LD_LIBRARY_PATH",runner().getParent());
        pb.environment().put("HOME",ctx.getFilesDir().getPath());pb.environment().put("TMPDIR",ctx.getCacheDir().getPath());
        return pb.start();
    }
}
