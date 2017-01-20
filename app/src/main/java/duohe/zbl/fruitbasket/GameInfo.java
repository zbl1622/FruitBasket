package duohe.zbl.fruitbasket;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.os.Looper;
import android.view.SurfaceView;
import android.view.View;

/**
 * 继承view.
 * @author offel
 *创建时间:2012-8-6
 *修改时间:2012-8-18  修改drawbitmap错误,添加函数drawTranImg();//绘制旋转图片
 *        2012-8-19		添加函数setPaintSize();//设置字体大小
 *        2012-8-20		添加函数drawCrossImg绘制图片的淡入淡出
 *        2012-11-1	加入线程
 *        			修改程序通过时间运行.sleep的时间很短
 *        			修改类为abstract类.
 */
public abstract class GameInfo extends SurfaceView{

	/**
	 * 屏幕的宽高,请根据横竖屏进行修改.
	 */
	public static final int LCD_WIDTH = 854;//屏幕宽
    public static final int LCD_HEIGHT = 480;//屏幕高
    /**
     * 每个线程所间隔的时间,可以修改
     */
    public static int nextFrameTime = 100;
   
    /**
     * 字体的宽高.一般20*20.可调整.
     */
    public static int FONT_WIDTH = 20;//字体宽
    public static int FONT_HEIGHT = 20;//字体高
    /**
     * 锚点
     */
    public static final byte MODE_TOP = 0;//左上角
    public static final byte MODE_MID_TOP = 1;//上中
    public static final byte MODE_RIGHT = 2;//右上角
    public static final byte MODE_LEFT_MID = 3;//左中
    public static final byte MODE_MID_MID = 4;//中中.
    public static final byte MODE_RIGHT_MID = 5;//右中
    public static final byte MODE_LEFT_BOTTOM = 6;//左下角
    public static final byte MODE_MID_BOTTOM = 7;//下中
    public static final byte MODE_RIGHT_BOTTOM = 8;//右下角
    /**
     * 当前可用侦
     */
    public static int atFrame = 0;
    /**
     * 当前可用时间.
     */
    public static int atTime = 0;
    /**
     * 总的用的侦数
     */
    private static int nextframeindex = 0;
    /**
     * 上一次线程的时间点
     */
    private static long threadStartTime = 0;
    /**
     * 线程运行的总时间.
     */
    private static long threadTime = 0;
    /**
     * 取得随机数的对象
     */
    private static Random rand = new Random();
    /**
     * 画笔
     */
    private static Paint gamePaint = null;
    /**
     * 画布 和画布的图片
     */
    private static Canvas bufferCanvas = null;
    private static Bitmap bufferMap = null;
    /**
     * 获取字符串的宽度所需方框
     */
    private static Rect strRect = new Rect();
    /**
     * 缩放图片时需要的矩阵
     */
    private static Matrix bmpm = new Matrix();
    
    /**
     * 控制线程运行
     */
    public static boolean threadrun = true;
    /**
     * 控制游戏退出
     */
    public static boolean isexit = false;
    private Activity act = null;
    
	public GameInfo(Activity gameact) {
		super(gameact);
		act = gameact;
		//设置画布
		bufferMap = Bitmap.createBitmap(LCD_WIDTH, LCD_HEIGHT, Config.ARGB_8888);
		bufferCanvas = new Canvas(bufferMap);
		//设置画笔
		gamePaint = new Paint();
		gamePaint.setAntiAlias(true);
		gamePaint.setTextSize(20);
		gamePaint.setTextAlign(Paint.Align.LEFT);
	}
	
	
	class TutorialThread extends Thread {// 刷帧线程
		public TutorialThread() {// 构造器
			threadStartTime = 0;
		}
		public void run() {// 重写的方法
			long starttime = 0;
			while (threadrun) {// 循环绘制
//				try{
					if (isexit) {
						DialogExit();
						return;
					} else {
						starttime = System.currentTimeMillis();
						if(threadStartTime != 0)
						{
							atTime = (int)(starttime - threadStartTime);
							threadTime += atTime;
						}
						if(nextframeindex*nextFrameTime < threadTime)
						{
							nextframeindex++;
							while(nextframeindex*nextFrameTime < threadTime)
							{
								nextframeindex++;
							}
							atFrame = 1;
						}else
						{
							atFrame = 0;
						}
						threadStartTime = starttime;
						onTimer();
					}
					drawGame();
				try {
					Thread.sleep(10);// 睡眠span毫秒
				} catch (Exception e) {// 
					e.printStackTrace();// 打印异常堆栈信息
				}
			}
		}
	}
	
	/**
	 * 退出的对话框
	 */
	public void DialogExit()
	{
		Looper.prepare();
		AlertDialog.Builder buider = new AlertDialog.Builder(act);
		buider.setMessage("确定退出游戏?");
		buider.setCancelable(false);
		buider.setPositiveButton("是", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int which) 
			{
				System.exit(0);
			}
		});
		buider.setNegativeButton("否", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int which) 
			{
				isexit = false;
				startThread();
				//				mHandler.sendEmptyMessage(0x0);
				//mHanler.getLooper().quit();
			}
		});
		AlertDialog ad = buider.create();
		ad.setOwnerActivity(act);
		ad.show();
		Looper.loop();
	}

	/**
	 * 设置透明度。请设置完成后再还原来0xff
	 * @param a
	 */
	public void setAlpha(int a)
	{
		gamePaint.setAlpha(a);
	}
	
	/**
	 * 设置线程的每一侦的间隔时间
	 * @param frametime
	 */
	public void setThreadFrame(int frametime)
	{
		nextframeindex = 0;
		nextFrameTime = frametime;
		threadTime = 0;
	}
	
	/**
	 * 设置字体大小
	 * @param size  设置后的字体大小
	 */ 
	public static final void setPaintSize(int size)
	{
		if(gamePaint == null || size <= 0)
			return;
		gamePaint.setTextSize(size);
		FONT_WIDTH = size;
		FONT_HEIGHT = size;
	}
	
	/**
	 * 绘制字符串.
	 * @param arg  字符串
	 * @param color 8位的字符串颜色.
	 * @param x X轴坐标
	 * @param y Y轴坐标
	 * @param mode 锚点位置
	 */
	public static final void drawString(String arg, int color, int x, int y, int mode)
	{
		if(arg == null || gamePaint == null || bufferCanvas == null)
		{
			System.err.println("drawString is null");
		}
		gamePaint.setColor(color);
		int w = getStrW(arg);
		switch(mode)
		{
		case MODE_TOP:
			y += FONT_HEIGHT;
			break;
		case MODE_MID_TOP:
			x -= (w>>1);
			y += FONT_HEIGHT;
			break;
		case MODE_RIGHT:
			x -= w;
			y += FONT_HEIGHT;
			break;
		case MODE_LEFT_MID:
			y += (FONT_HEIGHT>>1);
			break;
		case MODE_MID_MID:
			x -= (w>>1);
			y += (FONT_HEIGHT>>1);
			break;
		case MODE_RIGHT_MID:
			x -= w;
			y += (FONT_HEIGHT>>1);
			break;
		case MODE_LEFT_BOTTOM:
			break;
		case MODE_MID_BOTTOM:
			x -= (w>>1);
			break;
		case MODE_RIGHT_BOTTOM:
			x -= w;
			break;
		}
		bufferCanvas.drawText(arg, x, y, gamePaint);
	}
	
	/**
	 * 获取字符串宽度
	 * @param str 字符串
	 * @return 整个字符串的宽度
	 */
	public static int getStrW(String str)
    {
		if(str == null || gamePaint == null)
			return 0;
		gamePaint.getTextBounds(str, 0, str.length(), strRect);
		return strRect.width();
    }
	
	/**
	 * 创建图片
	 * @param view view对象
	 * @param name 图片名称:.png不需要添加
	 * @return
	 */
	public static Bitmap createImage(View view, String name)
    {
    	Bitmap map = null; 
        AssetManager am = view.getResources().getAssets();  
        try  
        {  
            InputStream is = am.open(name+".png");  
            map = BitmapFactory.decodeStream(is);  
            is.close();  
        }catch (IOException e)  
        {  
        	System.err.println(name + ".png is null");
        }
		return map;
    }
	
	/**
	 * 绘制图片---全图
	 * @param img 图片对象
	 * @param x X轴坐标
	 * @param y Y轴坐标
	 * @param mode 锚点
	 */
	public static void drawBitmap(Bitmap img, int x, int y, int mode)
	{
		if(img == null || gamePaint == null || bufferCanvas == null)
		{
			System.err.println("darwBitmap is null");
			return;
		}
		switch(mode)
		{
		case MODE_TOP:
			break;
		case MODE_MID_TOP:
			x -= (img.getWidth()>>1);
			break;
		case MODE_RIGHT:
			x -= img.getWidth();
			break;
		case MODE_LEFT_MID:
			y -= (img.getHeight()>>1);
			break;
		case MODE_MID_MID:
			x -= (img.getWidth()>>1);
			y -= (img.getHeight()>>1);
			break;
		case MODE_RIGHT_MID:
			x -= img.getWidth();
			y -= (img.getHeight()>>1);
			break;
		case MODE_LEFT_BOTTOM:
			y -= img.getHeight();
			break;
		case MODE_MID_BOTTOM:
			x -= (img.getWidth()>>1);
			y -= img.getHeight();
			break;
		case MODE_RIGHT_BOTTOM:
			x -= img.getWidth();
			y -= img.getHeight();
			break;
		}
		bufferCanvas.drawBitmap(img, x, y, gamePaint);
	}
	
	/**
	 * 分图的绘制
	 * @param img 图片对象
	 * @param x X轴
	 * @param y Y轴
	 * @param srcx 图片起始X轴
	 * @param srcy 图片起始Y轴
	 * @param srcw 需要绘制的宽度
	 * @param srch 需要绘制的高度
	 * @param mode 锚点
	 */
	public static void drawBitmap(Bitmap img, int x, int y, int srcx, int srcy, int srcw, int srch, int mode)
    {
		if(img == null || gamePaint == null || bufferCanvas == null)
		{
			System.err.println("darwBitmap is null");
			return;
		}
		switch(mode)
		{
		case MODE_TOP:
			break;
		case MODE_MID_TOP:
			x -= (srcw>>1);
			break;
		case MODE_RIGHT:
			x -= srcw;
			break;
		case MODE_LEFT_MID:
			y -= (srch>>1);
			break;
		case MODE_MID_MID:
			x -= (srcw>>1);
			y -= (srch>>1);
			break;
		case MODE_RIGHT_MID:
			x -= srcw;
			y -= (srch>>1);
			break;
		case MODE_LEFT_BOTTOM:
			y -= srch;
			break;
		case MODE_MID_BOTTOM:
			x -= (srcw>>1);
			y -= srch;
			break;
		case MODE_RIGHT_BOTTOM:
			x -= srcw;
			y -= srch;
			break;
		}
    	bufferCanvas.save();
    	bufferCanvas.clipRect(x, y, x+srcw, y+srch);
    	x -= srcx;
    	y -= srcy;
    	bufferCanvas.drawBitmap(img, x, y, gamePaint);
    	bufferCanvas.restore();
    }
	
	/**
	 * 以中心点位置绘制淡入淡出的图片。
	 * 
	 * @param img  图片名
	 * @param x  图片的X轴（中心点位置）
	 * @param y  图片的Y轴（中心点位置）
	 * @param atframe  当前侦数
	 * @param allframe 从开始到最后的侦数总和。
	 */
	public static void drawCrossImg(Bitmap img, int x, int y, int atframe, int allframe)
	{
		float ratio = 1f;//缩放的比率
		int alpha = 0xff;//透明度
		if(atframe <= 0 || allframe < 0 || img == null)
		{
			return;
		}
		if(atframe >= allframe)
		{
			drawBitmap(img, x, y, MODE_MID_MID);
		}
		ratio = (float)atframe / allframe;
		alpha = atframe * alpha / allframe;
    	bmpm.reset();
    	bmpm.postScale(ratio, ratio);
    	bmpm.postTranslate(x-img.getWidth()*atframe/allframe/2, y-img.getHeight()*atframe/allframe/2);//最后平移
    	gamePaint.setAlpha(alpha);
    	bufferCanvas.drawBitmap(img, bmpm, gamePaint);
    	gamePaint.setAlpha(0xff);
	}
	
	/**
	 * 绘制旋转的图片 --以图片中心点旋转
	 * @param img 图片指针
	 * @param x 图片左上角X轴位置
	 * @param y 图片左上角Y轴位置
	 * @param tran 旋转的角度 0-360度
	 */
	public static void drawTranImg(Bitmap img, int x, int y, int tran)
    {
    	bmpm.reset();
    	bmpm.postRotate(tran, img.getWidth()/2, img.getHeight()/2);//再旋转
    	bmpm.postTranslate(x, y);//最后平移
    	bufferCanvas.drawBitmap(img, bmpm, gamePaint);
//    	bitmp = null;
    }
	
	/**
	 * 重绘屏幕.
	 * @param g  传入view所取得的canvas
	 * @param srcw  传入取得的手机屏幕宽度
	 * @param srch 传入取得的手机屏幕高度
	 */
	public static void drawScreen(Canvas g, int srcw, int srch)
	{
		if(gamePaint == null || bufferCanvas == null || bufferMap == null || g == null)
		{
			return;
		}
		float w = (float)srcw / LCD_WIDTH;
    	float h = (float)srch / LCD_HEIGHT;
    	bmpm.reset();
    	bmpm.postScale(w, h);
		g.drawBitmap(bufferMap,bmpm, gamePaint);
	}
	
	/**
	 * 填充矩形
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @param color 颜色值
	 */
	public static void fillRect(int x, int y, int w, int h, int color)
    {
		if(gamePaint == null || bufferCanvas == null)
		{
			return;
		}
		gamePaint.setColor(color);
    	gamePaint.setStyle(Paint.Style.FILL);
    	bufferCanvas.drawRect(x, y, x + w, y + h, gamePaint);
    }
    
	/**
	 * 绘制矩形
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @param color 颜色值
	 */
    public static void drawRect(int x, int y, int w, int h, int color)
    {
    	if(gamePaint == null || bufferCanvas == null)
    		return;
    	gamePaint.setColor(color);
    	gamePaint.setStyle(Paint.Style.STROKE);
    	bufferCanvas.drawRect(x, y, x+w, y+h, gamePaint);
    }
    
	/**
	 * 取得最小值-最大值之间的随机数
	 * @param minnumber 最小值
	 * @param maxnumber 最大值
	 * @return
	 */
	public static int getRandom(int minnumber, int maxnumber)
	{
    	if(minnumber >= maxnumber)
    		return minnumber;
    	else
    		return minnumber + Math.abs(rand.nextInt())%(maxnumber-minnumber);
	}
	/**
	 * 点和面的碰撞检测
	 * @param x
	 * @param y
	 * @param x1
	 * @param y1
	 * @param w
	 * @param h
	 * @return
	 */
	protected boolean isHit(int x, int y, int x1, int y1, int w, int h)
    {
    	if(x < x1)
    		return false;
    	if(x > x1 + w)
    		return false;
    	if(y < y1)
    		return false;
    	if(y > y1 + h)
    		return false;
    	return true;
    }
    
	/**
	 * 面和面的碰撞检测1
	 * @param x1
	 * @param y1
	 * @param w1
	 * @param h1
	 * @param x2
	 * @param y2
	 * @param w2
	 * @param h2
	 * @return
	 */
    public static boolean isHit(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2)
    {
    	if(x1 + w1 < x2)
    		return false;
    	if(x1 > x2 + w2)
    		return false;
    	if(y1 + h1 < y2)
    		return false;
    	if(y1 > y2 + h2)
    		return false;
    	return true;
    }
    
    /**
     * 设置裁剪区域
     * @param x X轴
     * @param y Y轴
     * @param w 宽度
     * @param h 高度
     */
    public void setClip(int x, int y, int w, int h)
    {
    	bufferCanvas.clipRect(x, y, x+w, y+h);
    }
    
    /**
     * 在原裁剪区域基础上,设置一个与他相交的区域
     * @param x 相交的裁决区域X轴
     * @param y 相交的裁决区域Y轴
     * @param w 相交的裁决区域宽度
     * @param h 相交的裁决区域高度
     */
    public void clipRect(int x, int y, int w, int h)
    {
    	Rect clipRect = bufferCanvas.getClipBounds();
    	if(clipRect.left<x)
    	{
    		clipRect.left = x;
    	}
    	if(clipRect.right > x+w)
    	{
    		clipRect.right = x+w;
    	}
    	if(clipRect.top < y)
    	{
    		clipRect.top = y;
    	}
    	if(clipRect.bottom > y + h)
    	{
    		clipRect.bottom = y + h;
    	}
    	bufferCanvas.clipRect(clipRect);
    }
    
    /**
	 * 游戏的逻辑,抽像方法,需要实例化
	 */
	public abstract void onTimer();
	/**
	 * 游戏的绘制,抽像方法,需要实例化
	 */
	public abstract void drawGame();
	/**
	 * 启动线程,抽像方法,需要实例化
	 */
	public abstract void startThread();
}
