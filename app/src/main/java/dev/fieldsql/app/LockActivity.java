package dev.fieldsql.app;
import android.os.Bundle;
import android.widget.*;
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
public final class LockActivity extends FragmentActivity {
 FieldApp app; boolean pending;
 public void onCreate(Bundle b){super.onCreate(b);app=(FieldApp)getApplication();Button retry=new Button(this);retry.setText("解锁 FieldSQL");setContentView(retry);retry.setOnClickListener(v->authenticate());authenticate();}
 void authenticate(){if(pending)return;pending=true;BiometricPrompt p=new BiometricPrompt(this,ContextCompat.getMainExecutor(this),new BiometricPrompt.AuthenticationCallback(){
   public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r){pending=false;app.verified=true;app.authOpen=false;finish();}
   public void onAuthenticationError(int code,CharSequence msg){pending=false;Toast.makeText(LockActivity.this,msg,Toast.LENGTH_SHORT).show();}
 });p.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle("解锁 FieldSQL").setNegativeButtonText("取消").build());}
 public void onBackPressed(){moveTaskToBack(true);}
 public void onDestroy(){app.authOpen=false;super.onDestroy();}
}
