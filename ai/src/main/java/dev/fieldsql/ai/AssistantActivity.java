package dev.fieldsql.ai;

import android.app.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.*;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AssistantActivity extends Activity {
    private static final int PICK=410;
    private int INK=0xff21212a,ACCENT=0xff5447cf,PAPER=0xfff7f8fb,MUTED=0xff626573,SOFT=0xffeeecfc;
    private int SURFACE=Color.WHITE, EDGE=0xffcaced8, LINE=0xffe1e3eb, ON_ACCENT=Color.WHITE;
    private Typeface sans,mono;
    private LinearLayout root,body,leftPane,rightPane;
    private Boolean twoColumns;
    private AiText labels;
    private String t(String zh){return labels.t(zh);}
    private final ArrayList<String[]> history=new ArrayList<>();
    private String quotedQuestion="",quotedAnswer="",quotedMode="";
    private Button historyButton,clearQuote;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private LocalModels models;
    private String dialect, schema, sql, error,mode;
    private TextView modelName,status,result,contextLabel;
    private EditText request;
    private Button go,use,manage,copy,downloadButton,cancelDownload;
    private TextView downloadDetail;
    private ProgressBar downloadProgress;
    private LinearLayout downloadActions;
    private ModelDownloads downloads;
    private ModelDownloads.Snapshot downloadState=new ModelDownloads.Snapshot(ModelDownloads.Phase.IDLE,0,0,0);
    private final ExecutorService downloadReader=Executors.newSingleThreadExecutor();
    private boolean watchingDownloads;
    private long downloadSeq;
    private final Runnable pollDownload=this::reloadDownloads;
    private LinearLayout modes, actions;
    private volatile Process process;
    private AtomicBoolean cancel=new AtomicBoolean();
    private boolean busy=false;
    private String response="",resultMode="";
    private String extra(String key,String def){String s=getIntent().getStringExtra(key);return s==null?def:s;}
    @Override public void onCreate(Bundle b){
        boolean dark=getIntent().getBooleanExtra("dark",true);
        setTheme(dark?android.R.style.Theme_Material_NoActionBar:android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(b);labels=new AiText(this,extra("lang","zh"));models=new LocalModels(this,labels);downloads=ModelDownloads.get(this);
        if(dark){INK=0xfff0f1f6;ACCENT=0xffb1a9ff;PAPER=0xff101116;MUTED=0xffa4a9b5;SOFT=0xff282538;SURFACE=0xff17191f;EDGE=0xff3b404b;LINE=0xff30343d;ON_ACCENT=0xff18152c;}
        dialect=extra("dialect","SQLite");schema=extra("schema","");sql=extra("sql","");error=extra("error","");mode=extra("mode","generate");
        if(!"fix".equals(mode)&&!"explain".equals(mode))mode="generate";
        if(b!=null){mode=b.getString("mode",mode);response=b.getString("response","");resultMode=b.getString("resultMode","");}
        sans=Typeface.createFromAsset(getAssets(),"ai-fonts/IBMPlexSans.ttf");mono=Typeface.createFromAsset(getAssets(),"ai-fonts/IBMPlexMono.ttf");
        getWindow().setStatusBarColor(PAPER);getWindow().setNavigationBarColor(PAPER);getWindow().getDecorView().setSystemUiVisibility(dark?0:(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(PAPER);setContentView(root);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),0,dp(16),0);bar.setBackgroundColor(PAPER);root.addView(bar,new LinearLayout.LayoutParams(-1,dp(48)));
        Button back=button(t("‹  返回"),false);back.setBackgroundColor(PAPER);back.setTextColor(INK);back.setOnClickListener(v->onBackPressed());bar.addView(back);
        TextView tag=txt("FieldSQL  /  AI",13,MUTED);tag.setTypeface(mono);tag.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);bar.addView(tag,new LinearLayout.LayoutParams(0,-1,1));
        View headerRule=new View(this);headerRule.setBackgroundColor(LINE);root.addView(headerRule,new LinearLayout.LayoutParams(-1,dp(1)));
        body=new LinearLayout(this);body.setBackgroundColor(PAPER);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        leftPane=new LinearLayout(this);leftPane.setOrientation(1);leftPane.setBackgroundColor(PAPER);
        rightPane=new LinearLayout(this);rightPane.setOrientation(1);rightPane.setBackgroundColor(PAPER);
        LinearLayout col=leftPane;
        TextView title=txt(t("SQL 助手"),28,INK);title.setTypeface(sans,Typeface.BOLD);add(col,title,0);
        modelName=txt("",14,INK);modelName.setTypeface(sans,Typeface.BOLD);
        LinearLayout modelBar=row(col);modelBar.setGravity(Gravity.CENTER_VERTICAL);modelBar.addView(modelName,new LinearLayout.LayoutParams(0,-2,1));manage=button(t("管理模型"),false);manage.setOnClickListener(v->modelMenu());modelBar.addView(manage);
        downloadActions=row(col);downloadButton=button(t("下载推荐模型 · 1.12 GB"),true);downloadActions.addView(downloadButton);
        cancelDownload=button(t("取消下载"),false);LinearLayout.LayoutParams cancelLp=new LinearLayout.LayoutParams(-2,-2);cancelLp.leftMargin=dp(8);downloadActions.addView(cancelDownload,cancelLp);
        downloadProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);downloadProgress.setMax(1000);downloadProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));add(col,downloadProgress,8);
        downloadDetail=txt("",13,MUTED);add(col,downloadDetail,6);
        downloadButton.setOnClickListener(v->{int ix=downloadState.phase==ModelDownloads.Phase.FAILED?downloadState.index:0;startDownload(ix);});
        cancelDownload.setOnClickListener(v->new AlertDialog.Builder(this).setMessage(t("取消此下载并移除未完成的文件？")).setPositiveButton(t("取消下载"),(d,i)->{try{downloads.discard();reloadDownloads();}catch(RuntimeException e){status.setText(t("无法访问系统下载服务，请检查系统设置后重试。"));}}).setNegativeButton(t("继续"),null).show());
        add(col,txt(t("任务"),13,MUTED),20);modes=row(col);
        String[][] choices={{"generate",t("写 SQL")},{"explain",t("解释")},{"fix",t("纠错")}};
        for(String[] x:choices){Button bt=button(x[1],false);bt.setTag(x[0]);bt.setOnClickListener(v->{if(busy||mode.equals(v.getTag()))return;mode=(String)v.getTag();response="";resultMode="";status.setText("");refresh();});weighted(modes,bt);}
        contextLabel=txt("",14,ACCENT);add(col,contextLabel,12);contextLabel.setOnClickListener(v->showContext());
        request=new EditText(this);request.setGravity(Gravity.TOP|Gravity.START);request.setMinLines(3);request.setTypeface(sans);request.setMaxLines(6);request.setTextSize(16);request.setTextColor(INK);request.setHintTextColor(MUTED);request.setPadding(dp(12),dp(10),dp(12),dp(10));request.setBackground(box(SURFACE,EDGE));add(col,request,10);
        if(b!=null)request.setText(b.getString("request",""));
        go=button(t("生成 SQL"),true);addCompact(col,go,12);go.setOnClickListener(v->{if(busy)stopWork();else infer();});
        status=txt("",13,MUTED);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);add(col,status,10);
        LinearLayout hr=row(rightPane);TextView head=txt(t("模型输出"),21,INK);head.setTypeface(sans,Typeface.BOLD);hr.addView(head,new LinearLayout.LayoutParams(0,-2,1));historyButton=button(t("本次对话"),false);historyButton.setOnClickListener(v->openHistory());hr.addView(historyButton);
        clearQuote=button(t("清除引用"),false);clearQuote.setOnClickListener(v->{quotedQuestion="";quotedAnswer="";status.setText("");refresh();});add(col,clearQuote,6);
        result=txt("",15,INK);result.setTextIsSelectable(true);result.setTypeface(mono);result.setGravity(Gravity.TOP);result.setPadding(0,dp(8),0,dp(8));result.setBackgroundColor(PAPER);result.setMinHeight(dp(100));add(rightPane,result,12);
        actions=row(rightPane);use=button(t("放入编辑器"),true);copy=button(t("复制"),false);actions.addView(use,new LinearLayout.LayoutParams(-2,-2));LinearLayout.LayoutParams copyLp=new LinearLayout.LayoutParams(-2,-2);copyLp.leftMargin=dp(8);actions.addView(copy,copyLp);
        use.setOnClickListener(v->{setResult(RESULT_OK,new Intent().putExtra("sql",response));finish();});
        copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(t("模型输出"),response));status.setText(t("已复制。"));});
        layoutPanes(getResources().getConfiguration().screenWidthDp>=720);
        root.addOnLayoutChangeListener((v,l,t,r,bt,ol,ot,or,ob)->{int width=r-l;if(width>0){boolean wide=width>=dp(720);if(twoColumns==null||wide!=twoColumns)root.post(()->layoutPanes(wide));}});
        refresh();
    }
    private ScrollView scrollFor(LinearLayout pane,int color){
        ScrollView sc=new ScrollView(this);sc.setBackgroundColor(color);sc.setFillViewport(true);sc.addView(pane,new ScrollView.LayoutParams(-1,-2));return sc;
    }
    private void unparent(View v){if(v.getParent() instanceof ViewGroup)((ViewGroup)v.getParent()).removeView(v);}
    private void layoutPanes(boolean wide){
        if(twoColumns!=null && wide==twoColumns)return;twoColumns=wide;
        unparent(leftPane);unparent(rightPane);body.removeAllViews();
        int pad=dp(wide?28:16);leftPane.setPadding(pad,dp(wide?28:14),pad,dp(wide?32:16));rightPane.setPadding(pad,dp(wide?28:12),pad,dp(24));
        if(wide){
            result.setMinHeight(dp(96));body.setOrientation(LinearLayout.HORIZONTAL);
            body.addView(scrollFor(leftPane,PAPER),new LinearLayout.LayoutParams(0,-1,0.45f));View rule=new View(this);rule.setBackgroundColor(LINE);body.addView(rule,new LinearLayout.LayoutParams(dp(1),-1));
            body.addView(scrollFor(rightPane,PAPER),new LinearLayout.LayoutParams(0,-1,0.55f));
        }else{
            result.setMinHeight(dp(100));body.setOrientation(LinearLayout.VERTICAL);
            LinearLayout stack=new LinearLayout(this);stack.setOrientation(LinearLayout.VERTICAL);
            stack.addView(leftPane,new LinearLayout.LayoutParams(-1,-2));stack.addView(rightPane,new LinearLayout.LayoutParams(-1,-2));body.addView(scrollFor(stack,PAPER),new LinearLayout.LayoutParams(-1,-1));
        }
    }
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("mode",mode);b.putString("request",request.getText().toString());b.putString("resultMode",resultMode);b.putString("response",response);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setTypeface(sans);return t;}
    private GradientDrawable box(int fill,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(9));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    private LinearLayout row(LinearLayout parent){LinearLayout r=new LinearLayout(this);r.setOrientation(0);r.setGravity(Gravity.CENTER_VERTICAL);add(parent,r,4);return r;}
    private void add(LinearLayout p,View v,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(top);p.addView(v,lp);}
    private void addCompact(LinearLayout p,View v,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.topMargin=dp(top);p.addView(v,lp);}
    private void weighted(LinearLayout p,View v){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(p.getChildCount()>0)lp.leftMargin=dp(6);p.addView(v,lp);}
    private Button button(String s,boolean primary){Button b=new Button(this);b.setAllCaps(false);b.setTypeface(sans);b.setStateListAnimator(null);b.setText(s);b.setTextSize(14);b.setMinHeight(dp(44));b.setMinimumWidth(0);b.setMinWidth(0);b.setPadding(dp(16),dp(6),dp(16),dp(6));style(b,primary);return b;}
    private void style(Button b,boolean primary){b.setBackground(box(primary?ACCENT:SURFACE,primary?0:LINE));b.setTextColor(primary?ON_ACCENT:INK);}
    private void showContext(){
        String details=dialect+"\n\n"+(schema.isEmpty()?t("未附表结构"):schema)+(sql.isEmpty()?"":"\n\nSQL\n"+sql)+(error.isEmpty()?"":t("\n\n错误\n")+error);
        new AlertDialog.Builder(this).setTitle(t("本次数据库上下文")).setMessage(details).setPositiveButton(t("关闭"),null).show();
    }
    private void refresh(){
        LocalModels m=models;File f=m.selected();modelName.setText(f==null?t("尚未安装模型"):m.label(f));
        String issue=m.unsupported();String ctx=dialect+"  ·  "+(schema.isEmpty()?t("未附表结构"):t("已附表结构"));
        if(!"generate".equals(mode))ctx+="  ·  "+(sql.trim().isEmpty()?t("未附 SQL"):t("已附 SQL"));contextLabel.setText(ctx+"  ›");
        for(int i=0;i<modes.getChildCount();i++){Button b=(Button)modes.getChildAt(i);boolean selected=mode.equals(b.getTag());b.setBackground(box(selected?SOFT:SURFACE,selected?ACCENT:LINE));b.setTextColor(selected?ACCENT:MUTED);b.setSelected(selected);b.setEnabled(!busy);}
        go.setEnabled(busy||(issue==null&&f!=null));go.setAlpha(go.isEnabled()?1f:.55f);go.setText(busy?t("停止"):("generate".equals(mode)?t("生成 SQL"):"fix".equals(mode)?t("修正 SQL"):t("解释 SQL")));
        request.setHint("generate".equals(mode)?t("例如：统计每个城市的销售总额"):"fix".equals(mode)?t("补充错误现象（可选）"):t("想重点了解什么？（可选）"));request.setEnabled(!busy);manage.setEnabled(!busy);
        if(!busy&&issue!=null)status.setText(issue);
        result.setText(response.isEmpty()?t("输出会显示在这里。"):response);actions.setVisibility(!busy&&!response.isEmpty()?View.VISIBLE:View.GONE);
        use.setVisibility("explain".equals(resultMode)?View.GONE:View.VISIBLE);
        result.setTextColor(response.isEmpty()?MUTED:INK);
        historyButton.setVisibility(history.isEmpty()?View.GONE:View.VISIBLE);historyButton.setEnabled(!busy);
        clearQuote.setVisibility(quotedAnswer.isEmpty()?View.GONE:View.VISIBLE);clearQuote.setEnabled(!busy);
        displayDownload();
    }
    private void openHistory(){
        String[] items=new String[history.size()];
        for(int i=0;i<items.length;i++){String[] turn=history.get(i);items[i]=(i+1)+". "+turn[0];}
        new AlertDialog.Builder(this).setTitle(t("本次对话")).setItems(items,(d,i)->{
            String[] turn=history.get(i);
            new AlertDialog.Builder(this).setTitle(turn[0]).setMessage(turn[1]).setPositiveButton(t("继续追问"),(a,b)->{quotedQuestion=turn[0];quotedAnswer=turn[1];quotedMode=turn[2];request.setText("");request.requestFocus();refresh();status.setText(t("已附上一条问答；可继续输入问题。"));}).setNegativeButton(t("关闭"),null).show();
        }).setNegativeButton(t("关闭"),null).show();
    }
    private void modelMenu(){
        File[] items=models.files();File current=models.selected();ArrayList<String> opts=new ArrayList<>();
        opts.add(t("下载模型"));opts.add(t("从文件导入 GGUF"));for(File f:items)opts.add((f.equals(current)?"✓  ":"")+models.label(f)+"  ·  "+LocalModels.mib(f.length()));
        new AlertDialog.Builder(this).setTitle(t("设备上的模型")).setItems(opts.toArray(new String[0]),(d,i)->{
            if(i==0){if(downloadState.phase==ModelDownloads.Phase.IDLE||downloadState.phase==ModelDownloads.Phase.FAILED)downloadMenu();else status.setText(t("已有一个模型正在下载。"));}else if(i==1){Intent open=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");startActivityForResult(open,PICK);}
            else {File f=items[i-2];new AlertDialog.Builder(this).setTitle(models.label(f)).setMessage(LocalModels.mib(f.length())+"  ·  GGUF")
                .setPositiveButton(t("使用此模型"),(q,w)->{models.select(f);refresh();status.setText(t("模型已切换。"));})
                .setNeutralButton(t("移除…"),(q,w)->new AlertDialog.Builder(this).setMessage(t("从本应用移除该模型？原始导入文件将保留。")).setPositiveButton(t("移除"),(z,a)->{try{models.remove(f);refresh();}catch(IOException e){status.setText(e.getMessage());}}).setNegativeButton(t("取消"),null).show())
                .setNegativeButton(t("返回"),null).show();}
        }).setNegativeButton(t("关闭"),null).show();
    }
    private void downloadMenu(){
        new AlertDialog.Builder(this).setTitle(t("下载至手机")).setItems(new String[]{t("Qwen2.5 Coder 1.5B · 1.12 GB（推荐）"),t("Qwen2.5 Coder 0.5B · 491 MB（轻量）")},(d,i)->{
            startDownload(i);
        }).setNegativeButton(t("返回"),null).show();
    }
    private void startDownload(int index){
        if(busy)return;
        try{
            if(downloadState.phase==ModelDownloads.Phase.FAILED)downloads.discard();
            downloads.start(index);
            downloadState=new ModelDownloads.Snapshot(ModelDownloads.Phase.QUEUED,index,0,0);displayDownload();reloadDownloads();
        }catch(Exception e){
            status.setText("STORAGE".equals(e.getMessage())?t("下载失败：存储空间不足，请释放空间后重试。"):t("无法访问系统下载服务，请检查系统设置后重试。"));reloadDownloads();
        }
    }
    private void reloadDownloads(){
        main.removeCallbacks(pollDownload);
        if(!watchingDownloads||isFinishing()||isDestroyed())return;
        long stamp=++downloadSeq;
        downloadReader.execute(()->{
            ModelDownloads.Snapshot next=downloads.poll();
            ui(()->{
                if(!watchingDownloads||stamp!=downloadSeq)return;
                boolean ended=downloadState.phase!=ModelDownloads.Phase.IDLE&&next.phase==ModelDownloads.Phase.IDLE;
                downloadState=next;
                if(ended&&!busy){refresh();if(models.selected()!=null)status.setText(t("模型已就绪。"));}
                else displayDownload();
                if(next.phase!=ModelDownloads.Phase.IDLE && next.phase!=ModelDownloads.Phase.FAILED)main.postDelayed(pollDownload,850);
            });
        });
    }
    private void displayDownload(){
        ModelDownloads.Phase phase=downloadState.phase;
        boolean idle=phase==ModelDownloads.Phase.IDLE,failed=phase==ModelDownloads.Phase.FAILED,verifying=phase==ModelDownloads.Phase.VERIFYING;
        boolean offer=idle&&models.selected()==null&&models.unsupported()==null;
        downloadActions.setVisibility(offer||!idle&&!verifying?View.VISIBLE:View.GONE);
        downloadButton.setVisibility(offer||failed?View.VISIBLE:View.GONE);downloadButton.setEnabled(!busy);
        downloadButton.setText(failed?t("重试下载"):t("下载推荐模型 · 1.12 GB"));
        cancelDownload.setVisibility(!idle&&!verifying?View.VISIBLE:View.GONE);
        downloadProgress.setVisibility(!idle&&!failed?View.VISIBLE:View.GONE);
        downloadDetail.setVisibility(idle?View.GONE:View.VISIBLE);
        if(idle)return;
        int i=downloadState.index;long total=LocalModels.SIZES[i],n=Math.min(total,downloadState.bytes);
        downloadProgress.setIndeterminate(verifying||phase==ModelDownloads.Phase.QUEUED);downloadProgress.setProgress((int)(1000*n/total));
        String caption;
        if(failed){caption=downloadError(downloadState.reason);}
        else if(verifying){caption=t("正在校验模型文件…");}
        else{
            String state=phase==ModelDownloads.Phase.QUEUED?t("等待开始"):t("正在下载");
            if(phase==ModelDownloads.Phase.PAUSED){int r=downloadState.reason;state=r==DownloadManager.PAUSED_QUEUED_FOR_WIFI?t("等待 Wi-Fi"):r==DownloadManager.PAUSED_WAITING_TO_RETRY?t("网络暂时不可用，系统将重试"):t("等待网络");}
            caption=state+" · "+LocalModels.mib(n)+" / "+LocalModels.mib(total)+" ("+(100*n/total)+"%)\n"+t("下载会在后台继续");
        }
        downloadDetail.setText(caption);
    }
    private String downloadError(int reason){
        if(reason>=400&&reason<=599)return t("模型服务器返回 HTTP ")+reason;
        switch(reason){
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:return t("模型服务器重定向失败，请稍后重试。");
            case DownloadManager.ERROR_HTTP_DATA_ERROR:case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:return t("模型下载中断，请检查网络后重试。");
            case DownloadManager.ERROR_INSUFFICIENT_SPACE:return t("下载失败：存储空间不足，请释放空间后重试。");
            case DownloadManager.ERROR_CANNOT_RESUME:return t("下载失败：系统无法恢复此传输，可重新下载。");
            case ModelDownloads.BAD_FILE:return t("模型文件与官方版本不符，请重新下载。");
            case ModelDownloads.LOST_FILE:return t("找不到下载文件，请重试。");
            case ModelDownloads.STORAGE:case DownloadManager.ERROR_DEVICE_NOT_FOUND:case DownloadManager.ERROR_FILE_ERROR:return t("无法写入模型目录，请检查可用空间后重试。");
            case ModelDownloads.SERVICE:return t("无法访问系统下载服务，请检查系统设置后重试。");
            default:return t("下载失败，代码 ")+reason;
        }
    }
    @Override protected void onStart(){super.onStart();watchingDownloads=true;reloadDownloads();}
    @Override protected void onStop(){watchingDownloads=false;downloadSeq++;main.removeCallbacks(pollDownload);super.onStop();}
    @Override protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);if(code==PICK&&result==RESULT_OK&&data!=null&&data.getData()!=null)importModel(data.getData());}
    private void ui(Runnable r){main.post(()->{if(!isFinishing()&&!isDestroyed())r.run();});}
    private AtomicBoolean begin(String s){
        cancel=new AtomicBoolean(false);busy=true;getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);refresh();status.setText(s);return cancel;
    }
    private void done(String s){busy=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);refresh();status.setText(s);}
    private void stopWork(){cancel.set(true);Process p=process;if(p!=null)p.destroyForcibly();status.setText(t("正在停止…"));}
    private void importModel(Uri uri){
        if(busy)return;AtomicBoolean flag=begin(t("正在读取文件…"));
        worker.execute(()->{
            LocalModels.Job job=new LocalModels.Job(){public boolean cancelled(){return flag.get();}public void progress(String s){ui(()->status.setText(t("正在保存  ")+s));}};
            try{models.importUri(uri,job);ui(()->done(t("模型已就绪。")));}
            catch(Exception e){ui(()->done(flag.get()?t("已停止。"):t("模型未保存：")+msg(e)));}
        });
    }
    private static String msg(Exception e){String s=e.getMessage();return s==null?e.getClass().getSimpleName():s;}
    private void infer(){
        File model=models.selected();if(model==null){modelMenu();return;}
        String question=request.getText().toString().trim();
        if("generate".equals(mode)&&question.isEmpty()){request.setError(t("输入想查询什么。"));return;}
        String sqlForTask=!quotedAnswer.isEmpty()&&!"explain".equals(quotedMode)?quotedAnswer:sql;
        if(!"generate".equals(mode)&&sqlForTask.trim().isEmpty()){status.setText(t("请先在 SQL 编辑器输入语句，再打开助手。"));return;}
        String userRequest=quotedAnswer.isEmpty()?question:"Previous question: "+quotedQuestion+"\nPrevious answer: "+quotedAnswer+"\nFollow-up request: "+question;
        String prompt=SqlPrompt.make(mode,dialect,schema,sqlForTask,error,userRequest,labels.language);
        String turnTitle=question.isEmpty()?("fix".equals(mode)?t("修正 SQL"):t("解释 SQL"))+" · "+sqlForTask:question;
        String taskMode=mode;response="";resultMode="";AtomicBoolean flag=begin(t("正在手机上加载模型…"));
        worker.execute(()->{
            File in=null;
            try{
                in=File.createTempFile("sql-context-",".txt",getCacheDir());
                try(OutputStream out=new FileOutputStream(in)){out.write(prompt.getBytes(StandardCharsets.UTF_8));}
                if(flag.get())throw new InterruptedIOException();
                Process p=models.start(model,in.getPath(),Math.max(1,Math.min(4,Runtime.getRuntime().availableProcessors())));
                process=p;
                if(flag.get())p.destroyForcibly();
                p.getOutputStream().close(); StringBuilder tail=new StringBuilder();Thread logs=new Thread(()->{
                    try(Reader rd=new InputStreamReader(p.getErrorStream(),StandardCharsets.UTF_8)){char[] block=new char[1024];int n;while((n=rd.read(block))!=-1){tail.append(block,0,n);if(tail.length()>6000)tail.delete(0,tail.length()-4000);}}catch(IOException ignored){}
                },"fieldsql-model-log");logs.start();
                StringBuilder answer=new StringBuilder();long last=0;ui(()->status.setText(t("模型正在推理…")));
                try(Reader rd=new InputStreamReader(p.getInputStream(),StandardCharsets.UTF_8)){
                    char[] block=new char[256];int n;while((n=rd.read(block))!=-1){
                        answer.append(block,0,n);if(System.nanoTime()-last>120000000L){String s=answer.toString().trim();ui(()->{result.setTextColor(INK);result.setText(s);});last=System.nanoTime();}
                    }
                }
                int exit=p.waitFor();logs.join(); if(flag.get())throw new InterruptedIOException();
                if(exit!=0){String t=tail.toString().toLowerCase(Locale.ROOT);
                    String reason=(t.contains("too long")||t.contains("context size"))?t("输入超出 3072 token 上下文。请减少表结构或缩短 SQL。"):
                        (exit==137||t.contains("allocate")||t.contains("out of memory"))?t("可用内存不足。请关闭其他应用，或切换到较小模型。"):
                        t.contains("cannot link")?t("设备无法加载原生推理库。"):t("模型无法加载或推理退出（")+exit+t("）。请检查文件是否完整、是否为兼容的单文件指令 GGUF。");
                    throw new IOException(reason);
                }
                String clean=SqlPrompt.clean(answer.toString());if(clean.isEmpty())throw new IOException(t("模型没有返回文字。"));
                ui(()->{response=clean;resultMode=taskMode;history.add(new String[]{turnTitle,clean,taskMode});quotedQuestion="";quotedAnswer="";done("explain".equals(taskMode)?t("已在设备上完成。"):t("已生成草稿，请在运行前核对。"));});
            }catch(Exception e){ui(()->done(flag.get()?t("已停止。"):msg(e)));}
            finally{Process p=process;if(p!=null && p.isAlive())p.destroyForcibly();process=null;if(in!=null)in.delete();}
        });
    }
    @Override public void onBackPressed(){if(!busy){super.onBackPressed();return;}new AlertDialog.Builder(this).setMessage(t("停止当前任务并返回？")).setPositiveButton(t("停止并返回"),(d,i)->{stopWork();finish();}).setNegativeButton(t("继续"),null).show();}
    @Override protected void onDestroy(){cancel.set(true);Process p=process;if(p!=null)p.destroyForcibly();worker.shutdownNow();downloadReader.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();}
}
