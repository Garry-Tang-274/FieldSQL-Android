package dev.fieldsql.app;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.util.Base64;
import android.view.*;
import android.webkit.*;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.BiometricManager;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;
import dev.fieldsql.db.*;
import dev.fieldsql.ai.AssistantActivity;

public final class MainActivity extends FragmentActivity {
 static final String ORIGIN="https://appassets.androidplatform.net/";
 final ExecutorService jobs=Executors.newSingleThreadExecutor();
 WebView web; Vault vault; Database db; String session="",kind="";
 int pendingId=-1; String pendingAction,pendingName; byte[] pendingBytes; JSONObject pendingArgs;
 boolean prepared,openShelf; volatile boolean ended;
 public void onCreate(Bundle b){super.onCreate(b);openShelf=getIntent().getBooleanExtra("shelf",false);
   try{vault=new Vault(this);}catch(Exception e){new AlertDialog.Builder(this).setMessage("无法打开本机加密存储："+e.getLocalizedMessage()).setPositiveButton("退出",(x,y)->finish()).show();return;}
   web=new WebView(this);setContentView(web);WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setDomStorageEnabled(false);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);
   web.setWebViewClient(new WebViewClient(){
     public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}
     public boolean shouldOverrideUrlLoading(WebView v,String url){return true;}
     public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
       String u=r.getUrl().toString(),name=u.startsWith(ORIGIN)?u.substring(ORIGIN.length()):"";
       if(name.equals("ai-fonts/IBMPlexSans.ttf")||name.equals("ai-fonts/IBMPlexMono.ttf"))try{return new WebResourceResponse("font/ttf",null,getAssets().open(name));}catch(IOException ignored){}
       if(name.matches("(index\\.html|[a-zA-Z0-9_-]+\\.(js|css))"))try{return new WebResourceResponse(name.endsWith("css")?"text/css":name.endsWith("js")?"application/javascript":"text/html","UTF-8",getAssets().open(name));}catch(IOException ignored){}
       return new WebResourceResponse("text/plain","UTF-8",404,"Not found",Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
     }
   });
   web.addJavascriptInterface(new Bridge(),"Native");web.loadUrl(ORIGIN+"index.html");
 }
 public void onNewIntent(Intent i){super.onNewIntent(i);if(i.getBooleanExtra("connections",false)&&prepared)web.evaluateJavascript("go('conn')",null);}
 void reply(int id,Object value,boolean ok){if(ended)return;runOnUiThread(()->{if(!ended&&web!=null){JSONArray arr=new JSONArray();arr.put(id);arr.put(ok);arr.put(value==null?JSONObject.NULL:value);web.evaluateJavascript("window.receive.apply(null,"+arr.toString().replace("\u2028","\\u2028").replace("\u2029","\\u2029")+")",null);}});}
 final class Bridge {
  @JavascriptInterface public void send(int id,String op,String body){
   if(ended)return;jobs.execute(()->{try{JSONObject a=new JSONObject(body);
     if(op.equals("key")||op.equals("import")||op.equals("export")||op.equals("ai")){runOnUiThread(()->picker(id,op,a));return;}
     reply(id,dispatch(op,a),true);
   }catch(Exception|LinkageError e){reply(id,message(e),false);}});
  }
 }
 static String message(Throwable e){return e.getLocalizedMessage()==null?e.getClass().getSimpleName():e.getLocalizedMessage();}
 JSONObject profiles()throws Exception{return new JSONObject(vault.read("profiles","{}"));}
 JSONObject publicProfile(JSONObject p)throws Exception{JSONObject r=new JSONObject(p.toString()); for(String k:new String[]{"password","privateKey","passphrase"}){r.put("has_"+k,!r.optString(k).isEmpty());r.remove(k);}return r;}
 JSONArray publicProfiles()throws Exception{JSONObject p=profiles();JSONArray list=new JSONArray();Iterator<String> it=p.keys();while(it.hasNext())list.put(publicProfile(p.getJSONObject(it.next())));return list;}
 void ensure(JSONObject a){if(db==null||!session.equals(a.optString("session")))throw new IllegalStateException("连接已切换，请重新打开此页");}
 Object dispatch(String op,JSONObject a)throws Exception{
  switch(op){
  case "init": {JSONObject r=new JSONObject();r.put("profiles",publicProfiles());r.put("workspace",new JSONObject(vault.read("workspace","{}")));r.put("local",new JSONArray(Databases.listLocal(this)));r.put("lock",((FieldApp)getApplication()).locked());r.put("openShelf",openShelf);prepared=true;return r;}
  case "workspace": vault.put("workspace",a.toString());return true;
  case "systemTheme":{boolean dark=a.optBoolean("dark",true);runOnUiThread(()->{int c=dark?0xff101116:0xfff7f8fb;getWindow().setStatusBarColor(c);getWindow().setNavigationBarColor(c);View decor=getWindow().getDecorView();int flags=decor.getSystemUiVisibility();int mask=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;decor.setSystemUiVisibility(dark?(flags&~mask):(flags|mask));});return true;}
  case "saveProfile":{JSONObject all=profiles();String id=a.optString("id");if(id.isEmpty())id=UUID.randomUUID().toString();JSONObject p=all.optJSONObject(id);if(p==null)p=new JSONObject();String host=a.getString("host").trim();if(host.isEmpty())throw new IllegalArgumentException("请填写数据库主机名");
     for(String k:new String[]{"name","engine","host","port","database","user","timeout","tls","ssh","sshHost","sshPort","sshUser","fingerprint"})if(a.has(k))p.put(k,a.get(k));
     for(String k:new String[]{"password","privateKey","passphrase"})if(!a.optString(k).isEmpty())p.put(k,a.getString(k));
     p.put("id",id);all.put(id,p);vault.put("profiles",all.toString());QueryWidget.refresh(this);return publicProfile(p);}
  case "deleteProfile":{JSONObject all=profiles();String id=a.getString("id");all.remove(id);vault.put("profiles",all.toString());QueryWidget.refresh(this);return true;}
  case "connect":{
    String id=a.getString("id"),engine=a.getString("engine");Database next;String name;
    if(engine.equals("sqlite")){name=a.getString("name").trim();if(name.equalsIgnoreCase("Field Notes")&&!id.equals("demo"))throw new IllegalArgumentException("Field Notes 为内置示例名称，请换一个库名");next=Databases.openLocal(this,name);
       if(id.equals("demo")&&!vault.read("seeded","false").equals("true"))try{seed(next);vault.put("seeded","true");}catch(Exception e){next.close();throw e;}
    } else {JSONObject p=profiles().getJSONObject(id);engine=p.getString("engine");name=p.optString("name",p.getString("host"));Databases.SshConfig ssh=null;
      if(p.optBoolean("ssh"))ssh=new Databases.SshConfig(p.getString("sshHost"),p.optInt("sshPort",22),p.getString("sshUser"),Base64.decode(p.getString("privateKey"),Base64.DEFAULT),p.optString("passphrase").getBytes(StandardCharsets.UTF_8),p.getString("fingerprint"));
      next=Databases.openRemote(engine,p.getString("host"),p.getInt("port"),a.has("database")?a.getString("database"):p.getString("database"),p.getString("user"),p.optString("password"),p.optBoolean("tls",true),ssh);
      try{next.setQueryTimeoutSeconds(p.optInt("timeout",60));}catch(Exception e){next.close();throw e;}
    }
    Database old=db;db=next;session=UUID.randomUUID().toString();kind=engine;if(old!=null)try{old.close();}catch(Exception ignored){};service(!engine.equals("sqlite")&&a.optBoolean("keep",true));
    return new JSONObject().put("session",session).put("engine",engine).put("name",name).put("id",id);
  }
  case "disconnect":{if(db!=null){db.close();db=null;}session="";service(false);return true;}
  case "keep":service(db!=null&&!kind.equals("sqlite")&&a.getBoolean("value"));return true;
  case "query":{ensure(a);Database.Result r=db.execute(a.getString("sql"),a.optInt("limit",100));return new JSONObject().put("columns",new JSONArray(r.columns)).put("rows",new JSONArray(r.rows)).put("cellTypes",new JSONArray(r.cellTypes)).put("affected",r.affected).put("truncated",r.truncated);}
  case "dictionary":{ensure(a);return new JSONArray(db.tables());}
  case "localList":return new JSONArray(Databases.listLocal(this));
  case "share":{String f=a.getString("name").replaceAll("[^\\p{L}\\p{N}._ -]","_");File dir=new File(getCacheDir(),"shared-results");dir.mkdirs();File[] old=dir.listFiles();if(old!=null)for(File item:old){if(item.isFile()&&item.lastModified()<System.currentTimeMillis()-86400000L)item.delete();}
     if(f.isEmpty()||f.equals(".")||f.equals(".."))f="result";File file=File.createTempFile("sql-","-"+f.substring(0,Math.min(100,f.length())),dir);
     try(FileOutputStream out=new FileOutputStream(file)){out.write(Base64.decode(a.getString("data"),Base64.DEFAULT));}
     Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".exports",file);Intent send=new Intent(Intent.ACTION_SEND).setType(a.getString("mime")).putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);send.setClipData(ClipData.newRawUri("result",uri));runOnUiThread(()->startActivity(Intent.createChooser(send,null)));return true;}
  case "copy":{String txt=a.getString("text");runOnUiThread(()->{android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("FieldSQL",txt));});return true;}
  case "lock":{boolean on=a.getBoolean("value");if(on&&BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)!=BiometricManager.BIOMETRIC_SUCCESS)throw new IllegalStateException("请先在系统设置登记可用的生物识别");
    FieldApp app=(FieldApp)getApplication();app.verified=!on;getSharedPreferences("device",0).edit().putBoolean("lock",on).commit();
    runOnUiThread(()->{if(on){getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);app.onActivityResumed(this);}else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);});return on;}
  default:throw new IllegalArgumentException("不支持的操作");
  }
 }
 static void seed(Database d)throws Exception{
    d.execute("BEGIN");try{
    d.execute("CREATE TABLE IF NOT EXISTS orders (id INTEGER PRIMARY KEY, region TEXT NOT NULL, product TEXT NOT NULL, units INTEGER NOT NULL, revenue NUMERIC NOT NULL)");
    d.execute("INSERT OR IGNORE INTO orders VALUES (1,'杭州','手冲咖啡',16,512),(2,'上海','冷萃咖啡',12,432),(3,'杭州','冷萃咖啡',8,288),(4,'成都','手冲咖啡',10,320),(5,'上海','意式浓缩',15,360),(6,'成都','意式浓缩',9,216)");
    d.execute("CREATE VIEW IF NOT EXISTS regional_sales AS SELECT region,SUM(revenue) AS revenue FROM orders GROUP BY region");d.execute("COMMIT");
    }catch(Exception e){try{d.execute("ROLLBACK");}catch(Exception ignored){}throw e;}
 }
 void service(boolean on){Intent i=new Intent(this,ConnectionService.class);if(!on){stopService(i);return;}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED&&!getPreferences(0).getBoolean("notified",false)){getPreferences(0).edit().putBoolean("notified",true).apply();runOnUiThread(()->requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},71));}if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
 void picker(int id,String op,JSONObject a){
  if(pendingId!=-1){reply(id,"请先完成当前文件或 AI 操作",false);return;}pendingId=id;pendingAction=op;pendingArgs=a;
  try{Intent i;
    if(op.equals("ai")){i=new Intent(this,AssistantActivity.class);for(String k:new String[]{"dialect","schema","sql","mode","error","lang"})i.putExtra(k,a.optString(k));i.putExtra("dark",a.optBoolean("dark"));}
    else if(op.equals("export")){pendingBytes=Base64.decode(a.getString("data"),Base64.DEFAULT);i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(a.getString("mime")).putExtra(Intent.EXTRA_TITLE,a.getString("name"));}
    else {i=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");}
    startActivityForResult(i,51);
  }catch(Exception e){pendingId=-1;pendingBytes=null;reply(id,message(e),false);}
 }
 public void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req!=51||pendingId==-1)return;
    int id=pendingId;String op=pendingAction;byte[] bytes=pendingBytes;JSONObject a=pendingArgs;pendingId=-1;pendingBytes=null;pendingArgs=null;
    if(res!=RESULT_OK||data==null){reply(id,"已取消",false);return;}
    jobs.execute(()->{try{Object result;
       if(op.equals("ai"))result=data.getStringExtra("sql");
       else if(op.equals("key"))result=Base64.encodeToString(Databases.readSshKey(this,data.getData()),Base64.NO_WRAP);
       else if(op.equals("import")){if("Field Notes".equalsIgnoreCase(a.getString("name").trim()))throw new IllegalArgumentException("请避开内置示例名称 Field Notes");try(Database imported=Databases.importLocal(this,a.getString("name"),data.getData())){}result=a.getString("name");}
       else {try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")){if(out==null)throw new IOException("无法写入");out.write(bytes);}result=true;}
       reply(id,result,true);
    }catch(Exception e){reply(id,message(e),false);}});
 }
 public void onBackPressed(){if(web!=null)web.evaluateJavascript("window.back ? window.back() : false",v->{if(!"true".equals(v))super.onBackPressed();});else super.onBackPressed();}
 protected void onDestroy(){ended=true;service(false);jobs.execute(()->{if(db!=null)try{db.close();}catch(Exception ignored){}});jobs.shutdown();if(web!=null){web.removeJavascriptInterface("Native");web.destroy();}super.onDestroy();}
}
