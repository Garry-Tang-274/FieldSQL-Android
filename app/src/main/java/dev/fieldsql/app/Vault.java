package dev.fieldsql.app;
import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Device-local, non-exportable key; encryption failures are never treated as empty data. */
final class Vault {
  private final SharedPreferences prefs;
  private final javax.crypto.SecretKey key;
  Vault(Context ctx) throws Exception {
    prefs=ctx.getSharedPreferences("encrypted-v1",0);
    KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
    String alias="FieldSQL.LocalVault.1";
    if(!ks.containsAlias(alias)){
      KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");
      g.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();
    }
    key=(javax.crypto.SecretKey)ks.getKey(alias,null);
  }
  synchronized String read(String name,String fallback)throws Exception{
    String data=prefs.getString(name,null); if(data==null)return fallback;
    String[] parts=data.split(":",2);
    Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.decode(parts[0],2)));
    c.updateAAD(name.getBytes(StandardCharsets.UTF_8));
    return new String(c.doFinal(Base64.decode(parts[1],2)),StandardCharsets.UTF_8);
  }
  synchronized void put(String name,String data)throws Exception{
    Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key);c.updateAAD(name.getBytes(StandardCharsets.UTF_8));
    String encoded=Base64.encodeToString(c.getIV(),2)+":"+Base64.encodeToString(c.doFinal(data.getBytes(StandardCharsets.UTF_8)),2);
    if(!prefs.edit().putString(name,encoded).commit())throw new java.io.IOException("无法保存到本机");
  }
}
