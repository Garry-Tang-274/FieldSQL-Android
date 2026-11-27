package dev.fieldsql.app;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.WindowManager;
public final class FieldApp extends Application implements Application.ActivityLifecycleCallbacks{
 int started=0; boolean verified=false,authOpen=false;
 boolean locked(){return getSharedPreferences("device",0).getBoolean("lock",false);}
 public void onCreate(){super.onCreate();registerActivityLifecycleCallbacks(this);}
 public void onActivityCreated(Activity a,Bundle b){if(locked())a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);}
 public void onActivityStarted(Activity a){started++;}
 public void onActivityResumed(Activity a){if(!(a instanceof LockActivity)&&locked()&&!verified&&!authOpen){authOpen=true;a.startActivity(new Intent(a,LockActivity.class));}}
 public void onActivityPaused(Activity a){}
 public void onActivityStopped(Activity a){if(--started==0&&!a.isChangingConfigurations())verified=false;}
 public void onActivitySaveInstanceState(Activity a,Bundle b){}
 public void onActivityDestroyed(Activity a){}
}
