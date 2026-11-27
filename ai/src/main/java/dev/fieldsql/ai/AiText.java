package dev.fieldsql.ai;
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
final class AiText {
    private final HashMap<String,String> labels=new HashMap<>();
    final String language;
    AiText(Context context,String lang){
        int i;
        switch(lang.toLowerCase(Locale.ROOT).split("[-_]")[0]){
            case "en": i=1;language="English";break; case "ja": i=2;language="Japanese";break;
            case "ko": i=3;language="Korean";break; case "es": i=4;language="Spanish";break; default:i=0;language="Chinese";
        }
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(context.getAssets().open("ai-strings.txt"),StandardCharsets.UTF_8))){
            for(String line;(line=reader.readLine())!=null;){String[] a=line.split("\\|",-1);if(a.length!=5)throw new IOException("Invalid translation row: "+line);labels.put(a[0].replace("\\n","\n"),a[i].replace("\\n","\n"));}
        }catch(IOException ex){throw new IllegalStateException("Cannot load assistant labels",ex);}
    }
    String t(String zh){String s=labels.get(zh);return s==null?zh:s;}
}
