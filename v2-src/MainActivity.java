package com.xiaozai.photoscreen;
import android.app.*;import android.os.*;import android.content.*;import android.database.Cursor;import android.provider.OpenableColumns;import android.net.*;import android.graphics.*;import android.graphics.drawable.*;import android.view.*;import android.widget.*;import java.io.*;import java.net.*;import java.util.*;import com.google.zxing.*;import com.google.zxing.common.BitMatrix;import fi.iki.elonen.NanoHTTPD;
public class MainActivity extends Activity{
 LinearLayout root,side,body;ImageView hero,fullScreenImage;Dialog fullScreenDialog;List<File> pics=new ArrayList<>();int page=0,pos=0,delay=5;boolean en=false,playing=true,shuffle=false,selectionMode=false;Set<String> selectedPaths=new HashSet<>();Handler h=new Handler();PhotoServer server;
 String[] zh={"首页","图片管理","手机上传","播放设置","系统设置","关于"},us={"Home","Gallery","Phone Upload","Slideshow","System","About"};
 File dir(){File f=new File(getFilesDir(),"photos");f.mkdirs();return f;}String s(String a,String b){return en?b:a;}int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
 TextView txt(String x,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(c);if(bold)t.setTypeface(null,1);return t;}
 android.graphics.drawable.GradientDrawable shape(int c,int stroke){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(c);d.setCornerRadius(dp(12));if(stroke!=0)d.setStroke(dp(2),stroke);return d;}
 public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(128);getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);en=getPreferences(0).getBoolean("en",false);delay=getPreferences(0).getInt("delay",5);load();layout();server();h.post(tick);}
 Runnable tick=new Runnable(){public void run(){if(page==0&&playing&&!pics.isEmpty()){pos=shuffle?new Random().nextInt(pics.size()):(pos+1)%pics.size();showPhoto(true);}h.postDelayed(this,delay*1000L);}};
 void load(){pics.clear();File[] a=dir().listFiles();if(a!=null)for(File f:a)if(f.getName().matches("(?i).+\\.(jpg|jpeg|png|webp|gif|bmp)"))pics.add(f);Collections.sort(pics,(a1,b)->Long.compare(a1.lastModified(),b.lastModified()));if(pos>=pics.size())pos=0;}
 void layout(){root=new LinearLayout(this);root.setPadding(dp(14),dp(12),dp(14),dp(10));root.setBackgroundColor(0xff081326);setContentView(root);side=new LinearLayout(this);side.setOrientation(1);root.addView(side,new LinearLayout.LayoutParams(dp(155),-1));body=new LinearLayout(this);body.setOrientation(1);root.addView(body,new LinearLayout.LayoutParams(0,-1,1));render();}
 TextView btn(String x,Runnable r){TextView t=txt(x,15,-1,true);t.setGravity(17);t.setFocusable(true);t.setFocusableInTouchMode(true);t.setPadding(dp(8),dp(12),dp(8),dp(12));t.setBackground(shape(0xff193b61,0xff356c9e));t.setOnFocusChangeListener((v,has)->{v.animate().scaleX(has?1.045f:1f).scaleY(has?1.045f:1f).translationZ(has?dp(5):0).setDuration(160).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();});t.setOnClickListener(v->{v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(65).withEndAction(()->v.animate().scaleX(v.hasFocus()?1.045f:1f).scaleY(v.hasFocus()?1.045f:1f).setDuration(110).start()).start();r.run();});return t;}
 void render(){side.removeAllViews();body.removeAllViews();TextView logo=txt("小李视觉",25,-1,true);logo.setGravity(17);side.addView(logo,new LinearLayout.LayoutParams(-1,dp(72)));for(int i=0;i<6;i++){final int n=i;TextView b=btn((en?us[i]:zh[i]),()->{page=n;render();});b.setBackground(shape(page==i?0xff15538d:0xff14243b,page==i?0xff53b5ff:0));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.setMargins(dp(5),dp(5),dp(5),dp(5));side.addView(b,p);}TextView head=txt((en?us[page]:zh[page])+"                         Wi-Fi  ·  "+new java.text.SimpleDateFormat("HH:mm  yyyy/MM/dd",Locale.getDefault()).format(new Date()),17,-1,true);body.addView(head,new LinearLayout.LayoutParams(-1,dp(45)));if(page==0)home();if(page==1)gallery();if(page==2)upload();if(page==3)settings();if(page==4)system();if(page==5)body.addView(txt("小李视觉 V2.0\nAndroid TV 图片管理与展示",20,-1,true));body.setAlpha(0f);body.setTranslationY(dp(9));body.animate().alpha(1f).translationY(0f).setDuration(230).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
 Bitmap bitmap(File f,int size){try{BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getPath(),o);int n=1;while(o.outWidth/n>size*2||o.outHeight/n>size*2)n*=2;o.inJustDecodeBounds=false;o.inSampleSize=n;return BitmapFactory.decodeFile(f.getPath(),o);}catch(Exception e){return null;}}
 void home(){
  if(pics.isEmpty()){
   TextView empty=txt("还没有图片\\n从手机上传、U盘或电视本地导入图片",24,-1,true);empty.setGravity(17);
   body.addView(empty,new LinearLayout.LayoutParams(-1,0,1));
   body.addView(btn("添加图片",()->pick()));
  }else{
   hero=new ImageView(this);hero.setScaleType(ImageView.ScaleType.FIT_CENTER);hero.setBackground(shape(0xff101d30,0xff254568));hero.setFocusable(true);hero.setContentDescription("点击或按确认键全屏查看图片");
   LinearLayout.LayoutParams heroParams=new LinearLayout.LayoutParams(-1,0,1f);heroParams.setMargins(dp(2),dp(4),dp(2),dp(8));body.addView(hero,heroParams);
   hero.setOnClickListener(v->openFullScreen());
   hero.setOnKeyListener((v,k,e)->{if(e.getAction()==KeyEvent.ACTION_UP&&(k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER)){openFullScreen();return true;}return false;});
   hero.setImageBitmap(bitmap(pics.get(pos),1400));hero.setAlpha(0f);hero.animate().alpha(1f).setDuration(280).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
   HorizontalScrollView thumbScroll=new HorizontalScrollView(this);thumbScroll.setHorizontalScrollBarEnabled(false);thumbScroll.setFillViewport(false);
   LinearLayout strip=new LinearLayout(this);strip.setOrientation(0);strip.setGravity(Gravity.CENTER_VERTICAL);strip.setPadding(dp(2),dp(4),dp(2),dp(4));thumbScroll.addView(strip,new HorizontalScrollView.LayoutParams(-2,-1));
   LinearLayout.LayoutParams stripParams=new LinearLayout.LayoutParams(-1,dp(112));stripParams.setMargins(0,0,0,dp(4));body.addView(thumbScroll,stripParams);
   int count=Math.min(10,pics.size());
   for(int i=0;i<count;i++){
    final int n=i;ImageView im=new ImageView(this);im.setImageBitmap(bitmap(pics.get(i),120));im.setScaleType(ImageView.ScaleType.CENTER_CROP);im.setFocusable(true);im.setBackground(shape(0xff14243b,0xff294765));im.setPadding(dp(2),dp(2),dp(2),dp(2));
    LinearLayout.LayoutParams thumbParams=new LinearLayout.LayoutParams(dp(128),dp(96));thumbParams.setMargins(dp(4),0,dp(4),0);strip.addView(im,thumbParams);
    im.setOnFocusChangeListener((v,has)->{v.animate().scaleX(has?1.08f:1f).scaleY(has?1.08f:1f).setDuration(140).start();v.setBackground(shape(0xff14243b,has?0xff53b5ff:0xff294765));});
    im.setOnClickListener(v->{pos=n;showPhoto(true);});
   }
  }
  body.addView(txt("图片数量："+pics.size()+"     "+(playing?"正在播放":"已暂停")+"     "+(pics.isEmpty()?"0/0":(pos+1)+"/"+pics.size()),13,0xffa9bfdc,false));
 }
 void showPhoto(boolean crossfade){if(pics.isEmpty()||page!=0)return;Bitmap next=bitmap(pics.get(pos),1200);if(next==null)return;BitmapDrawable nd=new BitmapDrawable(getResources(),next);if(hero!=null){Drawable previous=hero.getDrawable();if(crossfade&&previous!=null){TransitionDrawable td=new TransitionDrawable(new Drawable[]{previous,nd});td.setCrossFadeEnabled(true);hero.setImageDrawable(td);td.startTransition(520);}else hero.setImageDrawable(nd);}if(fullScreenImage!=null){Drawable previous=fullScreenImage.getDrawable();if(crossfade&&previous!=null){TransitionDrawable td=new TransitionDrawable(new Drawable[]{previous,new BitmapDrawable(getResources(),next)});td.setCrossFadeEnabled(true);fullScreenImage.setImageDrawable(td);td.startTransition(520);}else fullScreenImage.setImageBitmap(next);}}
 void openFullScreen(){if(pics.isEmpty()||fullScreenDialog!=null)return;fullScreenDialog=new Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen);FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(0xff000000);fullScreenImage=new ImageView(this);fullScreenImage.setScaleType(ImageView.ScaleType.FIT_CENTER);fullScreenImage.setFocusable(true);fullScreenImage.setFocusableInTouchMode(true);fullScreenImage.setImageBitmap(bitmap(pics.get(pos),1600));frame.addView(fullScreenImage,new FrameLayout.LayoutParams(-1,-1));fullScreenImage.setOnClickListener(v->closeFullScreen());fullScreenImage.setOnKeyListener((v,k,e)->{if(e.getAction()==KeyEvent.ACTION_UP&&(k==KeyEvent.KEYCODE_BACK||k==KeyEvent.KEYCODE_ESCAPE)){closeFullScreen();return true;}return false;});fullScreenDialog.setContentView(frame);fullScreenDialog.setOnDismissListener(d->{fullScreenDialog=null;fullScreenImage=null;});Window w=fullScreenDialog.getWindow();if(w!=null){w.setLayout(-1,-1);w.setFlags(1024,1024);w.getDecorView().setSystemUiVisibility(5894|1024|512);}fullScreenDialog.show();if(w!=null)w.setLayout(-1,-1);fullScreenImage.requestFocus();}
 void closeFullScreen(){if(fullScreenDialog!=null)fullScreenDialog.dismiss();}
 void gallery(){
  LinearLayout intro=new LinearLayout(this);intro.setOrientation(1);intro.setPadding(dp(14),dp(10),dp(14),dp(12));intro.setBackground(shape(0xff10233b,0xff254568));
  TextView title=txt(selectionMode?"批量管理图片":"图片库",21,-1,true);intro.addView(title);
  TextView sub=txt(selectionMode?"已选择 "+selectedPaths.size()+" / "+pics.size()+" 张图片":"共 "+pics.size()+" 张图片 · 可进入批量选择模式",13,0xffb8cce4,false);
  LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(5);intro.addView(sub,sp);
  LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2);ip.bottomMargin=dp(10);body.addView(intro,ip);
  LinearLayout tools=new LinearLayout(this);tools.setGravity(Gravity.CENTER_VERTICAL);
  TextView count=txt("图库内容："+pics.size()+" 张",14,0xffd5e5f7,true);tools.addView(count,new LinearLayout.LayoutParams(0,dp(42),1));
  if(selectionMode){
   TextView all=btn(selectedPaths.size()==pics.size()?"取消全选":"全选",()->{if(selectedPaths.size()==pics.size())selectedPaths.clear();else{selectedPaths.clear();for(File f:pics)selectedPaths.add(f.getAbsolutePath());}render();});
   tools.addView(all,new LinearLayout.LayoutParams(dp(90),dp(40)));
   TextView del=btn("删除所选 ("+selectedPaths.size()+")",()->deleteSelected());LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(dp(150),dp(40));dl.leftMargin=dp(6);tools.addView(del,dl);
   TextView cancel=btn("完成",()->{selectionMode=false;selectedPaths.clear();render();});LinearLayout.LayoutParams cl=new LinearLayout.LayoutParams(dp(86),dp(40));cl.leftMargin=dp(6);tools.addView(cancel,cl);
  }else{
   TextView select=btn("批量选择",()->{selectionMode=true;selectedPaths.clear();render();});tools.addView(select,new LinearLayout.LayoutParams(dp(112),dp(40)));
   TextView add=btn("＋ 导入图片",()->pick());LinearLayout.LayoutParams al=new LinearLayout.LayoutParams(dp(140),dp(40));al.leftMargin=dp(6);tools.addView(add,al);
  }
  LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.bottomMargin=dp(8);body.addView(tools,tp);
  ScrollView sc=new ScrollView(this);sc.setClipToPadding(false);sc.setPadding(dp(4),dp(4),dp(4),dp(8));body.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
  GridLayout g=new GridLayout(this);g.setColumnCount(4);g.setUseDefaultMargins(false);sc.addView(g);
  for(int i=0;i<pics.size();i++){
   final int n=i;File photo=pics.get(i);boolean chosen=selectedPaths.contains(photo.getAbsolutePath());
   LinearLayout cell=new LinearLayout(this);cell.setOrientation(1);cell.setPadding(dp(6),dp(6),dp(6),dp(8));cell.setBackground(shape(chosen?0xff174c70:0xff12243b,chosen?0xff53b5ff:0xff263e5b));
   ImageView im=new ImageView(this);im.setImageBitmap(bitmap(photo,160));im.setScaleType(ImageView.ScaleType.CENTER_CROP);cell.addView(im,new LinearLayout.LayoutParams(-1,dp(96)));
   TextView name=txt((chosen?"✓ ":"")+photo.getName(),10,chosen?0xff8fd0ff:-1,true);name.setMaxLines(2);cell.addView(name);
   cell.setFocusable(true);cell.setOnFocusChangeListener((v,has)->v.animate().scaleX(has?1.035f:1f).scaleY(has?1.035f:1f).setDuration(150).start());
   cell.setOnClickListener(v->{
    if(selectionMode){if(selectedPaths.contains(photo.getAbsolutePath()))selectedPaths.remove(photo.getAbsolutePath());else selectedPaths.add(photo.getAbsolutePath());render();}
    else new AlertDialog.Builder(this).setTitle(photo.getName()).setItems(new String[]{"播放","加入批量选择","删除"},(x,w)->{
     if(w==0){pos=n;selectionMode=false;selectedPaths.clear();page=0;render();}
     else if(w==1){selectionMode=true;selectedPaths.add(photo.getAbsolutePath());render();}
     else new AlertDialog.Builder(this).setMessage("只删除图库副本，不影响原文件。").setNegativeButton("取消",null).setPositiveButton("删除",(q,z)->{photo.delete();load();render();}).show();
    }).show();
   });
   GridLayout.LayoutParams gp=new GridLayout.LayoutParams();gp.width=0;gp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);gp.setMargins(dp(5),dp(5),dp(5),dp(5));g.addView(cell,gp);
  }
 }
 void deleteSelected(){
  if(selectedPaths.isEmpty()){new AlertDialog.Builder(this).setMessage("请先选择要删除的图片。").setPositiveButton("知道了",null).show();return;}
  int amount=selectedPaths.size();
  new AlertDialog.Builder(this).setTitle("批量删除").setMessage("确定删除选中的 "+amount+" 张图片吗？\\n只删除小李视觉图库副本，不影响手机、U盘或其他位置的原文件。")
   .setNegativeButton("取消",null).setPositiveButton("确认删除",(d,w)->{
    int deleted=0;for(String path:new HashSet<>(selectedPaths)){File f=new File(path);if(f.exists()&&f.delete())deleted++;}
    selectedPaths.clear();selectionMode=false;load();
    new AlertDialog.Builder(this).setMessage("已删除 "+deleted+" 张图片。").setPositiveButton("确定",(a,b)->render()).setOnCancelListener(a->render()).show();
   }).show();
 }
 void pick(){Intent i=new Intent("android.intent.action.OPEN_DOCUMENT");i.addCategory("android.intent.category.OPENABLE");i.setType("image/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,5);}
 protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==5&&c==-1&&d!=null)new Thread(()->{try{if(d.getClipData()!=null)for(int i=0;i<d.getClipData().getItemCount();i++)copy(d.getClipData().getItemAt(i).getUri());else if(d.getData()!=null)copy(d.getData());}catch(Exception e){}runOnUiThread(()->{load();page=0;render();});}).start();}
 String pickedName(android.net.Uri u){String name=null;try(Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)name=c.getString(i);}}if(name==null||name.trim().isEmpty())name="image_"+System.currentTimeMillis()+".jpg";name=new File(name).getName().replaceAll("[^A-Za-z0-9._-]","_");if(!name.matches("(?i).+\\.(jpg|jpeg|png|webp|gif|bmp)$"))name+=".jpg";return System.currentTimeMillis()+"_"+UUID.randomUUID().toString().substring(0,6)+"_"+name;}
 void copy(android.net.Uri u)throws Exception{String name=pickedName(u);try(InputStream in=getContentResolver().openInputStream(u);OutputStream o=new FileOutputStream(new File(dir(),name))){if(in==null)throw new IOException("Cannot open image");byte[] b=new byte[8192];int n;while((n=in.read(b))>0)o.write(b,0,n);}}
 String ip(){try{for(Enumeration<NetworkInterface> e=NetworkInterface.getNetworkInterfaces();e.hasMoreElements();)for(Enumeration<InetAddress>a=e.nextElement().getInetAddresses();a.hasMoreElements();){InetAddress x=a.nextElement();if(x instanceof Inet4Address&&!x.isLoopbackAddress())return x.getHostAddress();}}catch(Exception e){}return "127.0.0.1";}
 void upload(){LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setGravity(Gravity.CENTER_HORIZONTAL);panel.setPadding(dp(24),dp(18),dp(24),dp(18));panel.setBackground(shape(0xff10233b,0xff294d70));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-1);pp.setMargins(dp(20),dp(14),dp(20),dp(14));body.addView(panel,pp);TextView title=txt("手机无线上传",22,-1,true);title.setGravity(Gravity.CENTER);panel.addView(title);TextView desc=txt("让手机和电视连接同一个 Wi-Fi 网络，然后扫描下方二维码",14,0xffb8cce4,false);desc.setGravity(Gravity.CENTER);LinearLayout.LayoutParams dp1=new LinearLayout.LayoutParams(-1,-2);dp1.topMargin=dp(8);dp1.bottomMargin=dp(14);panel.addView(desc,dp1);try{String u="http://"+ip()+":8765/?token="+uploadToken();BitMatrix m=new MultiFormatWriter().encode(u,BarcodeFormat.QR_CODE,350,350);Bitmap b=Bitmap.createBitmap(350,350,Bitmap.Config.RGB_565);for(int x=0;x<350;x++)for(int y=0;y<350;y++)b.setPixel(x,y,m.get(x,y)?0xff000000:-1);ImageView q=new ImageView(this);q.setImageBitmap(b);q.setBackgroundColor(-1);q.setPadding(dp(8),dp(8),dp(8),dp(8));panel.addView(q,new LinearLayout.LayoutParams(dp(190),dp(190)));TextView url=txt(u,16,0xff8fd0ff,true);url.setGravity(Gravity.CENTER);LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(-1,-2);up.topMargin=dp(14);panel.addView(url,up);}catch(Exception e){panel.addView(txt("二维码生成失败，请检查网络状态",14,0xffffb4a9,false));}TextView hint=txt("支持一次选择多张图片 · 上传完成后自动加入电视图库",13,0xffb8cce4,false);hint.setGravity(Gravity.CENTER);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(14);panel.addView(hint,hp);TextView refresh=btn("刷新二维码",()->render());LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(190),dp(44));rp.topMargin=dp(16);panel.addView(refresh,rp);}
 void settings(){body.addView(txt("播放间隔",17,-1,true));for(int n:new int[]{3,5,10,15,30,60})body.addView(btn(n+" 秒"+(delay==n?" ✓":""),()->{delay=n;getPreferences(0).edit().putInt("delay",n).apply();render();}));body.addView(btn("播放 / 暂停",()->{playing=!playing;render();}));body.addView(btn("顺序 / 随机播放",()->{shuffle=!shuffle;render();}));body.addView(btn("上一张",()->{if(!pics.isEmpty()){pos=(pos+pics.size()-1)%pics.size();render();}}));body.addView(btn("下一张",()->{if(!pics.isEmpty()){pos=(pos+1)%pics.size();render();}}));}
 void system(){long z=0;for(File f:pics)z+=f.length();body.addView(txt("图片数量："+pics.size()+"\n图库占用："+(z/1048576)+" MB\n剩余空间："+(dir().getFreeSpace()/1048576)+" MB",17,-1,false));body.addView(btn(en?"简体中文":"English",()->{en=!en;getPreferences(0).edit().putBoolean("en",en).apply();render();}));body.addView(btn("导入图片 / U盘",()->pick()));}
 String uploadToken(){String t=getPreferences(0).getString("upload_token",null);if(t==null){t=UUID.randomUUID().toString().replace("-","");getPreferences(0).edit().putString("upload_token",t).apply();}return t;}
 void server(){try{if(server!=null)server.stop();server=new PhotoServer(8765,dir(),uploadToken(),()->runOnUiThread(()->{load();if(page==0)render();}));server.start(NanoHTTPD.SOCKET_READ_TIMEOUT,false);}catch(Exception e){}}
 public boolean onKeyDown(int k,KeyEvent e){if(k==KeyEvent.KEYCODE_MENU){new AlertDialog.Builder(this).setItems(new String[]{"下一张","暂停 / 继续","图片管理"},(d,w)->{if(w==0&&!pics.isEmpty())pos=(pos+1)%pics.size();if(w==1)playing=!playing;if(w==2)page=1;render();}).show();return true;}return super.onKeyDown(k,e);}
 public void onBackPressed(){if(fullScreenDialog!=null&&fullScreenDialog.isShowing()){closeFullScreen();return;}if(selectionMode){selectionMode=false;selectedPaths.clear();render();return;}if(page!=0){page=0;render();}else super.onBackPressed();}protected void onDestroy(){h.removeCallbacks(tick);if(server!=null)server.stop();super.onDestroy();}
 static class PhotoServer extends NanoHTTPD{
  File dir;String token;Runnable changed;
  PhotoServer(int p,File f,String t,Runnable r)throws IOException{super(p);dir=f;token=t;changed=r;}
  boolean authorized(IHTTPSession s){return token.equals(s.getParms().get("token"));}
  public Response serve(IHTTPSession s){
   try{
    if(!authorized(s))return newFixedLengthResponse(Response.Status.FORBIDDEN,"text/plain","Access denied");
    if(Method.GET.equals(s.getMethod())){
     String html="<meta charset='utf-8'><meta name='viewport' content='width=device-width'><body style='background:#101827;color:white;font:18px Arial;padding:24px'><h2>小李视觉图片上传</h2><input id='photoFiles' type='file' accept='image/*' multiple><button onclick='go()'>开始上传</button><p id='status'>选择图片</p><script>async function go(){const files=document.getElementById('photoFiles').files;const status=document.getElementById('status');for(const x of files){const token=new URLSearchParams(location.search).get('token');const r=await fetch('/up?token='+encodeURIComponent(token)+'&name='+encodeURIComponent(x.name),{method:'POST',body:x});if(!r.ok){status.innerText='上传失败：'+x.name;return}status.innerText='已上传：'+x.name}status.innerText='上传完成'}</script>";
     return newFixedLengthResponse(html);
    }
    if(Method.POST.equals(s.getMethod())&&s.getUri().equals("/up")){
     Map<String,String> body=new HashMap<>();s.parseBody(body);String tmp=body.get("postData");
     if(tmp==null)return newFixedLengthResponse(Response.Status.BAD_REQUEST,"text/plain","empty");
     File temp=new File(tmp);
     if(!temp.isFile()||temp.length()==0||temp.length()>25L*1024*1024)return newFixedLengthResponse(Response.Status.BAD_REQUEST,"text/plain","Invalid file size");
     String n=s.getParms().get("name");if(n==null)n="photo.jpg";
     n=new File(n).getName().replaceAll("[^A-Za-z0-9._-]","_");
     if(!n.matches("(?i).+\\.(jpg|jpeg|png|webp|gif|bmp)$"))return newFixedLengthResponse(Response.Status.BAD_REQUEST,"text/plain","Unsupported image type");
     File out=new File(dir,System.currentTimeMillis()+"_"+UUID.randomUUID().toString().substring(0,6)+"_"+n);
     try(InputStream in=new FileInputStream(temp);OutputStream o=new FileOutputStream(out)){byte[] b=new byte[8192];int x;while((x=in.read(b))>0)o.write(b,0,x);}
     changed.run();return newFixedLengthResponse("OK");
    }
    return newFixedLengthResponse(Response.Status.NOT_FOUND,"text/plain","404");
   }catch(Exception e){return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,"text/plain","Upload failed");}
  }
 }
}