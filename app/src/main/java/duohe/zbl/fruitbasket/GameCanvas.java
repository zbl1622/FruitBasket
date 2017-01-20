package duohe.zbl.fruitbasket;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.shapes.CircleDef;
import org.jbox2d.collision.shapes.PolygonDef;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
/**
 * 游戏逻辑类,所有的程序逻辑请放此类.
 * @author offel
 * 创建时间:2012-8-6
 * 修改时间:2012-8-18 优化代码结构
 * 			2012-11-1	测试代码
 */
public class GameCanvas extends GameInfo implements SurfaceHolder.Callback{

	private static String TAG="GameCanvas";
	
	private static GameActivity gameact = null;//Activity对象
	private static TutorialThread gamethread = null;//游戏唯一线程.
	private static boolean isPhone = false;//来电处理.
	private static int gamekey = -1;//游戏的按键.请在ontouchevent中里保存按键信息.
	private static Sound sound;//音乐播放管理
	private static boolean isMusic=true;//音乐开关
	private static boolean isSound=true;//音效开关
	private static Random random=new Random();//随机数生成器
	private static SharedPreferences saveTool;//简单存储
	
	//物理引擎
	private final static float PI=3.14159f;//π
	private final static int RATE = 10;// 屏幕到现实世界的比例
	// 10px：1m;这里要注意，当我们根据android当中的坐标去定义刚体的位置时，我们需要将坐标除以这个比例获得世界当中的长度，用这个长度来进行定义。
	private AABB worldAABB; // 创建一个坐标系统
	private World world; // 创建一个世界
	private float timeStep; // 模拟的的频率
	private int iterations; // 迭代越大，模拟约精确，但性能越低
	private Vec2 gravity;
	private Body ball;
	private int[] data_radius={14,15,13,16,20};
	private int radius=14;
	int b_x;
	int b_y;
	int angle;
	
	//加速度传感器(重力感应)
	private SensorManager manager;
	private float g_x, g_y;//重力数据
	private SensorEventListener mySensorListener = new SensorEventListener() {
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
		public void onSensorChanged(SensorEvent event) {
			g_x = event.values[SensorManager.DATA_Y];
			g_y = event.values[SensorManager.DATA_X];
		}
	};
	
	/**
	 * 游戏中所有第一层状态
	 */
	public static final int STATE_LOADING = 0;//加载界面
	public static final int STATE_MENU = 1;//主菜单
	public static final int STATE_PLAY = 2;//游戏进行中
	public static final int STATE_MIDDLE_MENU=4;//中途菜单
	public static final int STATE_VERIFY=5;//回到主菜单确认
	public static final int STATE_HELP=6;//帮助界面
	public static final int STATE_REWARD=7;//奖励屋界面
	public static final int STATE_DIFFICULTY=8;//难度选择
	public static final int STATE_ROLE=9;//选择角色
	public static final int STATE_WIN=10;//游戏胜利
	public static final int STATE_OVER=11;//游戏失败
	public static final int STATE_ABOUT=13;//关于
	public static final int STATE_LEVEL=14;//关卡选择
	public static final int STATE_OPEN = 15;//开机画面
	//关卡选择状态
	public static final int LEVEL_NORMAL=0;//普通关卡
	public static final int LEVEL_MYSTERIOUS=1;//神秘关卡
	//游戏进行中状态常量
	public static final int PLAY_PREPARE=1;//准备
	public static final int PLAY_PLAY=2;//进行
	public static final int PLAY_SHOT=3;//刚刚击中
	public static final int PLAY_FAIL=4;//失败
	
	
	private static int gameState = STATE_LOADING;//记录游戏的状态
	private static int lastState = -1;//记录游戏的上一层状态
	private static int playState=-1;//游戏进行中状态
	private static int levelState=0;//关卡状态
	
	private LinkedList<Integer[][]> mapDataList=new LinkedList<Integer[][]>();//存放拼图数据的列表
	
	private LinkedList<Rect> hitDataList[]=new LinkedList[37];//存放碰撞数据的列表
	
	private Vector<Body> bodyVector=new Vector<Body>();//存放所有碰撞物的容器
	
	//地图中的物品
	private static final int ITEM_GOLD = 1;//金币
	private static final int ITEM_OIL=2;//油
	private static final int ITEM_PAPER=3;//纸团
	private static final int ITEM_WOOD=4;//木板
	private static final int ITEM_DEAD=6;//死亡区域
	private static final int ITEM_WIN=7;//胜利区域
	/**
	 * 物品类，封装坐标、半径、种类等信息
	 * @author user_zbl
	 *
	 */
	private class Item{
		/**
		 * 横坐标
		 */
		public int x;
		/**
		 * 纵坐标
		 */
		public int y;
		/**
		 * 半径
		 */
		public int r;
		/**
		 * 角度(角度制,0~360)
		 */
		public int angle=0;
		/**
		 * 范围矩形
		 */
		public Rect rect;
		/**
		 * 物品种类
		 */
		public int type=0;
		/**
		 * 是否激活
		 */
		public boolean isActive=false;
		/**
		 * 帧数计数
		 */
		public int frame=10;
		Item(int x,int y,int r,int type,boolean isActive,int angle){
			this(x,y,r,type,isActive);
			this.angle=angle;
		}
		Item(int x,int y,int r,int type,boolean isActive){
			this.x=x;
			this.y=y;
			this.r=r;
			this.type=type;
			this.isActive=isActive;
		}
		Item(int x,int y,int type,boolean isActive,int angle){
			this(x,y,type,isActive);
			this.angle=angle;
		}
		Item(int x,int y,int type,boolean isActive){
			this.x=x;
			this.y=y;
			this.type=type;
			this.isActive=isActive;
			switch(type){
			case ITEM_GOLD:
				r=25;
				break;
			case ITEM_OIL:
				r=16;
				break;
			case ITEM_PAPER:
				r=17;
				break;
			case ITEM_WOOD:
				r=20;
				break;
			case ITEM_DEAD:
				r=50;
				break;
			case ITEM_WIN:
				r=50;
				break;
			default:
				r=15;
				break;
			}
		}
	}
	
	private LinkedList<Item> itemDataList=new LinkedList<Item>();//存放金币、障碍物等
	private Item winItem;//存放胜利区域
	//坐标
	//预设坐标
	//loading状态的参数
	private int loadingindex = 0;
	//难度等级
	private int level=0;
	//每种拼图的数量
	private final int[] data_map_count={37,30,0,0};
	//每个关卡的拼图种类
	private final int[] data_map_type={0,0,0,0,0,0,0,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0};
	private int map_type=0;
	//关卡的背景图序号
	private final int[] data_bg_index={0,0,1,1,2,2,1,1,0,0,1,1,2,2,1,1,0,0,1,1,2,2,1,1};
	private int bg_index=0;
	//关卡map数据
	Integer[][] mapData;
	
	//绘图
	private boolean isSlide=false;
	private int offset_x=0;//地图偏移
	private int offset_y=0;
	private int save_x;//记忆地图偏移
	private int save_y;
	private int start_x=0;//滑动时记录初始点击位置
	private int start_y=0;
	private int bound_left=0;//地图边界
	private int bound_top=0;
	private int bound_right=0;
	private int bound_bottom=0;
	private int column=0;//地图切片行列数
	private int row=0;
	private static final int EDGE=120;
	//水果位置
	private int fruit_x;
	private int fruit_y;
	//选择关卡界面
	private final int LEVEL=14;//关卡总数
	private int page_num=0;//当前页码
	private int flag_slide=0;//是否正在滑动,0不滑动,1前进,-后退
	private int slide_x=0;//滑动计数
	//总时间
	private int left_time=90*1000;
	//显示时间
	private int time0;//十位
	private int time1;//个位
	//每次获得的积分
	private int score=1;
	//总得分
	private int score_sum=0;
	//显示得分
	private int score0;//十位
	private int score1;//个位
	//循环计时
	private long last_time;
	//勋章和胜利界面延时
	private int delay_time=0;
	//角色选择界面
	private int role_type=0;//0苹果、1橙子、2石榴、3哈密瓜、4西瓜
	//积分榜界面
	private int slide_y=0;//滑动偏移y
	private int[] role_score=new int[5];//每个角色对应的积分
	private int[] role_sort=new int[5];//角色排名顺序
	
	//图片引用
	private Bitmap logoBmp;//Logo界面背景
	
	private Bitmap openBmp;//开机界面背景
	
	private Bitmap loadingBmp;//加载界面背景
	private Bitmap loadingBmp0,loadingBmp1,loadingBmp2;
	
	private Bitmap mainBgBmp;//主背景
	
	private Bitmap button_returnBmp;//返回按钮图片
	private Bitmap button_aboutBmp;//关于按钮图片
	
	private Bitmap menuBmp;//主菜单背景
	private Bitmap sound0Bmp;//音乐关
	private Bitmap sound1Bmp;//音乐开
	private Bitmap pause0Bmp;//暂停暗
	private Bitmap pause1Bmp;//暂停亮
	private Bitmap menu_startBmp0,menu_startBmp1;
	private Bitmap menu_helpBmp0,menu_helpBmp1;
	private Bitmap menu_rewardBmp0,menu_rewardBmp1;
	private Bitmap menu_quitBmp0,menu_quitBmp1;
	
	private Bitmap phoneBmp;//来电暂停界面图片
	
	private Bitmap helpBmp;//帮助界面背景
	
	private Bitmap rewardBmp;//奖励屋背景
	private Bitmap reward1Bmp,reward2Bmp;//积分榜头部和底部
	
	private Bitmap verifyBmp;//返回主菜单确认
	
	private Bitmap difficultyBmp;//难度选择界面背景
	private Bitmap btn_forwardBmp;//前进箭头
	private Bitmap btn_backBmp;//后退箭头
	private Bitmap leafBmp;//叶子
	private Bitmap lockBmp;//锁
	private Bitmap difficulty_normalBmp0,difficulty_normalBmp1;
	private Bitmap difficulty_roleBmp0,difficulty_roleBmp1;
	private Bitmap difficulty_returnBmp0,difficulty_returnBmp1;
	
	private Bitmap middleMenuBmp;//中途菜单背景
	private Bitmap btn_menuBmp;//继续游戏
	private Bitmap btn_helpBmp;//游戏帮助
	private Bitmap maskBgBmp;//全屏变暗的mask
	
	private Bitmap playBmp;//游戏进行中背景
	private Bitmap clockBmp;//闹钟
	private Bitmap basket0Bmp,basket1Bmp;//篮子图片及mask图
	private Bitmap gold0Bmp,gold1Bmp;//金币图片
	private Bitmap oil0Bmp,oil1Bmp;//油渍图片
	private Bitmap paper0Bmp,paper1Bmp,paper2Bmp;//纸团图片
	private Bitmap woodBmp;//木头图片
	
	//游戏背景
	private Bitmap[][] playBgBmp=new Bitmap[4][2];
	//管道拼图
	private Bitmap[][] mapBmp=new Bitmap[4][];
	//水果
	private Bitmap[][] fruitBmp=new Bitmap[5][2];
	
	private Bitmap[] num=new Bitmap[24];//关卡数字图片
	
	private Bitmap[] nBmp=new Bitmap[10];//数字
	
	//角色选择界面
	private Bitmap roleBmp[]=new Bitmap[5];//角色图片
	private Bitmap introduceBmp[]=new Bitmap[5];//介绍文字图片
	private Bitmap bar_chooseBmp;//选择背景高亮条
	private Bitmap role_frameBmp;//选择区域框
	private Bitmap role_bottomBmp;//底部的栅栏
	
	private Bitmap winBgBmp;//胜利界面背景
	private Bitmap failBgBmp;//失败界面背景
	
	private Bitmap signBgBmp;//获得勋章面板文字
	private Bitmap signBmp[]=new Bitmap[7];//勋章暗
	private Bitmap signedBmp[]=new Bitmap[7];//勋章亮
	
	
	//按钮矩形区域
	//主菜单
	private Rect button_menu_sound=new Rect(775,2,850,92);//声音开关
	private Rect button_start=new Rect(10,311,227,418);//开始游戏按钮
	private Rect button_help=new Rect(217,210,427,307);//游戏帮助按钮
	private Rect button_reward=new Rect(443,196,655,291);//奖励屋按钮
	private Rect button_quit=new Rect(624,298,844,398);//退出按钮
	private Rect button_about=new Rect(685,2,765,94);//关于按钮
	//帮助界面
//	private Rect button_help_sound=new Rect(390,24,467,101);//声音开关
	private Rect button_help_return=new Rect(4,4,80,80);//返回上一个界面
	//积分榜界面
	private Rect button_reward_return=new Rect(760,385,850,470);//返回
	//难度选择界面
	private Rect button_difficulty_normal=new Rect(365,90,626,158);//普通关卡
//	private Rect button_difficulty_mysterious=new Rect(365,184,626,253);//神秘关卡
	private Rect button_difficulty_role=new Rect(365,231,626,299);//角色选择
	private Rect button_difficulty_sound=new Rect(760,2,850,92);//声音开关
	private Rect button_difficulty_return=new Rect(365,370,626,437);//返回
	//关卡选择界面
	private Rect button_level_forward=new Rect(763,223,810,271);//前进
	private Rect button_level_back=new Rect(56,223,103,271);//后退
	private Rect button_level_return=new Rect(760,385,850,470);//返回
	//角色选择界面
	private Rect button_role_return=new Rect(760,385,850,470);//返回
	private Rect area_role_choose=new Rect(34,77,372,460);//选择角色的区域
	//副菜单界面
	private Rect button_middlemenu_pause=new Rect(758,79,834,154);//副菜单界面的返回游戏按钮
	private Rect button_middlemenu_menu=new Rect(758,165,834,242);//副菜单界面的奖励屋按钮
	private Rect button_middlemenu_help=new Rect(758,253,834,330);//副菜单界面的游戏帮助按钮
	private Rect button_middlemenu_music=new Rect(758,341,834,415);//副菜单界面的回到主菜单按钮
	//返回主菜单确认
	private Rect button_verify_ok=new Rect(219,264,393,380);
	private Rect button_verify_cancel=new Rect(462,264,635,380);
	//关于
	private Rect button_about_return=new Rect(760,4,850,93);//返回上一个界面
	//游戏进行中
	private Rect button_play_pause=new Rect(760,385,850,470);//游戏进行中暂停
	private Rect button_play_music=new Rect(760,3,850,95);
	//游戏胜利界面
	private Rect button_win_next=new Rect(314,266,539,334);//进入下一关
	private Rect button_win_menu=new Rect(314,344,539,411);//返回主菜单
	//游戏失败界面
	private Rect button_fail_retry=new Rect(314,266,539,334);//重玩本关
	private Rect button_fail_menu=new Rect(314,344,539,411);//返回主菜单
	
	public GameCanvas(GameActivity act) {
		super(act);
		getHolder().addCallback(this);//添加界面隐藏,消毁等函数的侦听
		setFocusableInTouchMode(true);//添加按键的侦听
		this.requestFocus();
		gameact = act;
		saveTool=gameact.getPreferences(Context.MODE_WORLD_WRITEABLE);//创建简单存储
		readRole();//读取角色
		sound=new Sound(gameact);//创建声音管理器
		logoBmp=createImage(this,"logo0");
		openBmp=createImage(this,"openBg");
		loadingBmp0=createImage(this,"loading0");
		loadingBmp1=createImage(this,"loading1");
		loadingBmp2=createImage(this,"loading2");
		// 加速度管理器初始化
		manager = (SensorManager) gameact.getSystemService(gameact.SENSOR_SERVICE);
		manager.registerListener(mySensorListener,manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_UI);
		// 创建这个世界的坐标范围,并且设定上下限，这里应该是按世界的长度来算的，也就是说这个范围是足够大的，我们只能在这个范围内创建刚体
		worldAABB = new AABB();
		worldAABB.lowerBound.set(-200.0f, -200.0f);
		worldAABB.upperBound.set(200.0f, 200.0f);
		// end
		gravity = new Vec2(0.0f, 0.0f); // 向量，用来标示当前世界的重力方向，第一个参数为水平方向，负数为做，正数为右。第二个参数表示垂直方向
		boolean doSleep = false; // 标示 是否睡眠
		world = new World(worldAABB, gravity, doSleep);// 创建世界
		timeStep = 5.0f / 60.0f; // 定义频率
		iterations = 10; // 定义迭代
		setGameState(STATE_LOADING);
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
	}

	public void surfaceCreated(SurfaceHolder holder) {
		startThread();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		threadrun = false;
		boolean is = true;
		while (is) {
            try {
            	gamethread.join();
            	is = false;
            } 
            catch (InterruptedException e) {//不断地循环，直到刷帧线程结束
            }
        }
		gamethread = null;
	}
	
	private void setGameState(int state)
	{
		lastState = gameState;
		gameState = state;
	}

	@Override
	public void drawGame() {
		Canvas c = getHolder().lockCanvas(null);
		onDraw(c);
		if(c != null)
			getHolder().unlockCanvasAndPost(c);
	}

	@Override
	public void onTimer() {
		if(isPhone)
		{
//			if(gamekey == 0)
//			{
//				gamekey = -1;
//				isPhone = false;
//			}
			return;
		}
		switch(gameState)
		{
		case STATE_MENU:
			onTimerMenu();
			break;
		case STATE_LOADING:
			initGame();
			break;
		case STATE_PLAY:
			onTimerPlay();
			break;
		case STATE_HELP:
			onTimerHelp();
			break;
		case STATE_DIFFICULTY:
			onTimerDifficulty();
			break;
		case STATE_MIDDLE_MENU:
			onTimerMiddlemenu();
			break;
		case STATE_OVER:
			onTimerGameover();
			break;
		case STATE_WIN:
			onTimerWin();
			break;
		case STATE_VERIFY:
			onTimerVerify();
			break;
		case STATE_ROLE:
			onTimerRole();
			break;
		case STATE_ABOUT:
			onTimerAbout();
			break;
		case STATE_LEVEL:
			onTimerLevel();
			break;
		default:
			break;
		}
	}

	@Override
	public void startThread() {
		threadrun = true;
		gamethread = new TutorialThread();
		gamethread.start();
	}
	
	/**
	 * 来电,按home键时的处理
	 */
	public void hideNotify()
	{
		if (sound!=null) {
			sound.stopAll();
		}
//		if(gameState==STATE_PLAY){
//			setGameState(STATE_MIDDLE_MENU);
//		}
		manager.unregisterListener(mySensorListener);
		isPhone=true;
	}
	/**
	 * 从暂停状态返回的处理
	 */
	public void resumeGame(){
		if (gameState!=STATE_LOADING&&gameState!=STATE_OPEN&&gameState!=-1&&sound!=null&&isMusic) {
			sound.start(Sound.SOUND_BG, isMusic, isSound)	;
		}
		manager.registerListener(mySensorListener,manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_UI);
		isPhone=false;
		last_time=System.currentTimeMillis();
	}
	/**
	 * 声音开关
	 */
	private boolean musicSwitch(){
		if (isMusic) {
//			sound.paused(Sound.SOUND_BG);
			sound.stopAll();
			isMusic=false;
		}else{
			isMusic=true;
			sound.start(Sound.SOUND_BG, isMusic, isSound);
		}
		return isMusic;
	}
	
	/**
	 * 计算两点间距
	 * 
	 */
	private double distance(int x1,int y1,int x2,int y2){
		double distance=Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
		return distance;
	}
	
	/**
	 * 读取刷新积分榜
	 */
	private void readScore(){
		for(int i=0;i<roleBmp.length;i++){
			role_score[i]=saveTool.getInt("score"+i, 0);
			Log.i(TAG, i+":   "+role_score[i]);
		}
	}
	/**
	 * 存储得分
	 */
	private void saveScore(String key,int score){
		SharedPreferences.Editor editor=saveTool.edit();
		editor.putInt(key, score);
		editor.commit();
	}
	/**
	 * 给积分榜排序
	 */
	private void sortScore(){
		int l=roleBmp.length;
		int[] temp_score=role_score.clone();
		
		for(int i=l-1;i>=0;i--){
			int max=-1;
			int index=0;
			for(int j=0;j<l;j++){
				if(temp_score[j]!=-1){
					if(temp_score[j]>max){
						max=temp_score[j];
						index=j;
					}
				}
			}
			temp_score[index]=-1;
			role_sort[l-i-1]=index;
		}
	}
	/**
	 * 读取角色
	 */
	private void readRole(){
		role_type=saveTool.getInt("role", 0);
	}
	/**
	 * 存储角色
	 */
	private void saveRole(int role){
		SharedPreferences.Editor editor=saveTool.edit();
		editor.putInt("role", role);
		editor.commit();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK)//返回键时的处理
		{
			isexit = true;
			return true;
		}
		if((keyCode == KeyEvent.KEYCODE_HOME 
				|| keyCode ==KeyEvent.KEYCODE_MENU) && event.getRepeatCount() == 0)//home键和menu键的处理
		{
			
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	/**
	 * 游戏的绘制.逻辑请放在onTimer()中.
	 */
	protected void onDraw(Canvas canvas) {
		fillRect(0, 0, LCD_WIDTH, LCD_HEIGHT, 0xffffffff);
		if(isPhone)
		{
			//绘制暂停的界面
			drawBitmap(phoneBmp,LCD_WIDTH>>1,LCD_HEIGHT>>1,MODE_MID_MID);
		}else
		{
			switch (gameState) {
			case STATE_LOADING:
				drawLoading();
				break;
			case STATE_MENU:
				drawMenu();
				break;
			case STATE_PLAY:
				drawPlay();
				break;
			case STATE_HELP:
				drawHelp();
				break;
			case STATE_REWARD:
				drawReward();
				break;
			case STATE_DIFFICULTY:
				drawDifficulty();
				break;
			case STATE_MIDDLE_MENU:
				drawMiddlemenu();
				break;
			case STATE_OVER:
				drawGameover();
				break;
			case STATE_WIN:
				drawWin();
				break;
			case STATE_VERIFY:
				drawVerify();
				break;
			case STATE_ROLE:
				drawRole();
				break;
			case STATE_ABOUT:
				drawAbout();
				break;
			case STATE_LEVEL:
				drawLevel();
				break;
			default:
				break;
			}
		}
		drawScreen(canvas, GameActivity.actWidth, GameActivity.actHeight);
	}
	
	@Override
	/**
	 * 游戏的触屏.请只用于保存按键.
	 */
	public boolean onTouchEvent(MotionEvent event) {
		if(isPhone)
		{
			if(event.getAction() == MotionEvent.ACTION_DOWN)
			{
//				gamekey = 0;
				resumeGame();
			}
			return true;
		}
		switch (gameState) {
		case STATE_MENU:
			onTouchMenu(event);
			break;
		case STATE_PLAY:
			onTouchPlay(event);
			break;
		case STATE_HELP:
			onTouchHelp(event);
			break;
		case STATE_REWARD:
			onTouchReward(event);
			break;
		case STATE_DIFFICULTY:
			onTouchDifficulty(event);
			break;
		case STATE_MIDDLE_MENU:
			onTouchMiddlemenu(event);
			break;
		case STATE_OVER:
			onTouchGameover(event);
			break;
		case STATE_WIN:
			onTouchWin(event);
			break;
		case STATE_VERIFY:
			onTouchVerify(event);
			break;
		case STATE_ROLE:
			onTouchRole(event);
			break;
		case STATE_ABOUT:
			onTouchAbout(event);
			break;
		case STATE_LEVEL:
			onTouchLevel(event);
			break;
		default:
			break;
		}
		return true;
	}
	
	/**
	 * 加载图片
	 */
	private void initGame()
	{
		gameState = STATE_LOADING;
		//音乐
		sound.createSound(Sound.SOUND_BG,R.raw.bg_music ,true);
		sound.createSound(Sound.SOUND_PRESS,R.raw.press ,false);
//		sound.createSound(Sound.SOUND_HELP, R.raw.help, false);
		sound.createSound(Sound.SOUND_GOLD, R.raw.gold, false);
		sound.createSound(Sound.SOUND_OIL, R.raw.oil, false);
		sound.createSound(Sound.SOUND_PAPER, R.raw.paper, false);
		sound.createSound(Sound.SOUND_WOOD, R.raw.wood, false);
		sound.createSound(Sound.SOUND_FAIL, R.raw.fail, false);
		sound.createSound(Sound.SOUND_LOSE, R.raw.lose, false);
		sound.createSound(Sound.SOUND_REWARD, R.raw.reward, false);
		sound.createSound(Sound.SOUND_WIN, R.raw.win, false);
		onTimerLoading(10);
		//主菜单
		menuBmp=createImage(this,"menu");
		sound0Bmp=createImage(this,"sound0");
		sound1Bmp=createImage(this,"sound1");
		pause0Bmp=createImage(this,"pause0");
		pause1Bmp=createImage(this,"pause1");
		menu_startBmp0=createImage(this,"menu_start0");
		menu_startBmp1=createImage(this,"menu_start1");
		menu_helpBmp0=createImage(this,"menu_help0");
		menu_helpBmp1=createImage(this,"menu_help1");
		menu_rewardBmp0=createImage(this,"menu_reward0");
		menu_rewardBmp1=createImage(this,"menu_reward1");
		menu_quitBmp0=createImage(this,"menu_quit0");
		menu_quitBmp1=createImage(this,"menu_quit1");
		//
		mainBgBmp=createImage(this,"mainBg");
		//
		button_returnBmp=createImage(this,"btn_return");
		button_aboutBmp=createImage(this,"btn_about");
		//帮助
		helpBmp=createImage(this,"help");
		//奖励屋
		rewardBmp=createImage(this,"reward");
		reward1Bmp=createImage(this,"reward1");
		reward2Bmp=createImage(this,"reward2");
		//获取勋章
		signBgBmp=createImage(this,"sign");
		//难度选择
		difficultyBmp=createImage(this,"difficulty");
		leafBmp=createImage(this,"leaf1");
		lockBmp=createImage(this,"lock");
		btn_forwardBmp=createImage(this,"btn_forward");
		btn_backBmp=createImage(this,"btn_back");
		difficulty_normalBmp0=createImage(this,"difficulty_normal0");
		difficulty_normalBmp1=createImage(this,"difficulty_normal1");
		difficulty_roleBmp0=createImage(this,"difficulty_role0");
		difficulty_roleBmp1=createImage(this,"difficulty_role1");
		difficulty_returnBmp0=createImage(this,"difficulty_return0");
		difficulty_returnBmp1=createImage(this,"difficulty_return1");
		//返回主菜单确认
		verifyBmp=createImage(this,"verify");
		//
		winBgBmp=createImage(this,"winBg");
		failBgBmp=createImage(this,"failBg");
		//来电暂停图片
		phoneBmp=createImage(this,"phone");
		onTimerLoading(20);
		//游戏进行中
		playBmp=createImage(this,"playBg");
		clockBmp=createImage(this,"clock");
		for (int i = 0; i < fruitBmp.length; i++) {
			fruitBmp[i][0] = createImage(this, "fruit"+i+"0");
			fruitBmp[i][1] = createImage(this, "fruit"+i+"1");
		}
		for (int i = 0; i < 4; i++) {
			playBgBmp[i]=new Bitmap[2];
			playBgBmp[i][0] = createImage(this, "playBg"+i+"0");
			playBgBmp[i][1] = createImage(this, "playBg"+i+"1");
		}
		onTimerLoading(30);
		middleMenuBmp=createImage(this,"middle_menu");
		btn_menuBmp=createImage(this,"btn_menu");
		btn_helpBmp=createImage(this,"btn_help");
		maskBgBmp=createImage(this,"maskBg");
		
		for(int i=0;i<roleBmp.length;i++){
			roleBmp[i]=createImage(this,"role"+i);//角色图片
			introduceBmp[i]=createImage(this,"introduce"+i);;//介绍文字图片
		}
		bar_chooseBmp=createImage(this,"bar_choose");//选择背景高亮条
		role_frameBmp=createImage(this,"role_frame");//选择区域框
		role_bottomBmp=createImage(this,"role_bottom");//底部
		onTimerLoading(40);
		
		initMapData();
		
		onTimerLoading(50);
		//载入拼图map
		for (int i = 0; i < data_map_count.length; i++) {
			mapBmp[i]=new Bitmap[data_map_count[i]];
			for (int j = 0; j < mapBmp[i].length; j++) {
				int n=j+1;
				mapBmp[i][j] = createImage(this, i+"map" +(n>9?"":"0")+n);
			}
		}
		onTimerLoading(60);
		for(int i=0;i<7;i++){
			signBmp[i]=createImage(this,"sign"+i);
			signedBmp[i]=createImage(this,"signed"+i);
		}
		onTimerLoading(70);
		for(int i=0;i<24;i++){
			num[i]=createImage(this,""+(i+1));
		}
		onTimerLoading(80);
		basket0Bmp=createImage(this,"basket0");
		basket1Bmp=createImage(this,"basket1");//篮子图片及mask图
		gold0Bmp=createImage(this,"gold0");
		gold1Bmp=createImage(this,"gold1");//金币图片
		oil0Bmp=createImage(this,"oil0");
		oil1Bmp=createImage(this,"oil1");//油渍图片
		paper0Bmp=createImage(this,"paper0");
		paper1Bmp=createImage(this,"paper1");
		paper2Bmp=createImage(this,"paper2");//纸团图片
		woodBmp=createImage(this,"wood");
		for(int i=0;i<10;i++){
			nBmp[i]=createImage(this,"num"+i);
		}
		onTimerLoading(90);
		sound.start(Sound.SOUND_BG, isMusic, isSound);
		gameState = STATE_MENU;
	}
	/**
	 * 初始化地图数据
	 */
	private void initMapData(){
		//载入地图拼图数组数
		mapDataList.add(0,new Integer[][]{
				{0x0000,0x0000,0x0003,0x0004,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0005,0x0007,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0005,0x0007,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0006,0x0008,0x000b,0x000b,0x000b,0x000b,0x000d,0x0000,0x0000},
				{0x0000,0x0000,0x000f,0x0011,0x0011,0x0011,0x0011,0x0013,0x0014,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0015,0x0014,0x0000,0x0000}});
		mapDataList.add(1, new Integer[][]{
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x000c,0x000c,0x000c,0x000c,0x000d,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0010,0x0010,0x0010,0x0013,0x0007,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0006,0x0007,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0005,0x0007,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0006,0x0008,0x000b,0x000b,0x000d,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x000f,0x0011,0x0011,0x0013,0x0014,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0006,0x0014,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0006,0x0008,0x000b,0x000b,0x000b,0x000b,0x000c},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x000f,0x0011,0x0011,0x0011,0x0011,0x0011,0x0012}});
		mapDataList.add(2,new Integer[][]{
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x000b,0x000b,0x000b,0x000b,0x000d,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0011,0x0011,0x0011,0x0013,0x0014,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0005,0x0007,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0005,0x0014,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0005,0x0007,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0005,0x0008,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x000f,0x0011,0x0011,0x0013,0x0018,0x0010,0x0010,0x0010,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x0019,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x0008,0x000b,0x000b,0x000b,0x000b,0x000d},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x000f,0x0011,0x0011,0x0011,0x0011,0x0013,0x001b},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b}});
		mapDataList.add(3, new Integer[][]{
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x000b,0x000b,0x000d,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0011,0x0013,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x0008,0x000b,0x000b,0x000b,0x000b,0x0000,0x0000,0x0000,0x0000,0x0000,0x000e,0x000b,0x000b,0x000b},
				{0x0000,0x000f,0x0011,0x0011,0x0011,0x0013,0x0002,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x0018,0x0011,0x0011},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x0008,0x000b,0x000b,0x000b,0x000b,0x000b,0x0009,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x000f,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0001,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000}});
		mapDataList.add(4, new Integer[][]{
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x000b,0x000b,0x000d,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x000e,0x000b,0x000b,0x000b,0x000b,0x000b},
				{0x0011,0x0013,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x0018,0x0011,0x0011,0x0011,0x0011},
				{0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x0008,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x0009,0x001b,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x0002,0x0011,0x0011,0x0011,0x0011,0x0011,0x0013,0x0018,0x0011,0x0011,0x0011,0x0001,0x0000,0x0000,0x0000,0x0000}});
		mapDataList.add(5, new Integer[][]{
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0011,0x0011,0x0011,0x0013,0x0018,0x0011,0x0011,0x0011,0x0011,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x000e,0x000b,0x000b,0x000b,0x000b},
				{0x0000,0x000b,0x000b,0x0009,0x0008,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000d,0x0000,0x001a,0x0018,0x0011,0x0011,0x0011},
				{0x0000,0x0011,0x0011,0x0013,0x0018,0x0011,0x0011,0x0011,0x0011,0x0011,0x0013,0x001b,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x000e,0x000b,0x000b,0x0009,0x001b,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x0002,0x0011,0x0011,0x0001,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x0008,0x000b,0x000b,0x000b,0x000b,0x0009,0x0008,0x000b,0x000b,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x000f,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0000}});
		mapDataList.add(6, new Integer[][]{
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x000e,0x000b,0x000b,0x000b,0x0009,0x0008,0x000b,0x000b,0x000b,0x000d,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x0016,0x0016,0x0016,0x0016,0x0016,0x0016,0x0016,0x0016,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x000f,0x0011,0x0011,0x0011,0x0013,0x0018,0x0011,0x0011,0x0011,0x0001,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x000e,0x000b,0x000b,0x000b},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x0008,0x000b,0x000b,0x000d,0x0000,0x0000,0x001a,0x0002,0x0011,0x0011},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x000f,0x0011,0x0011,0x0013,0x001b,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x000e,0x000b,0x000b,0x000b,0x0009,0x001b,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x001a,0x0018,0x0011,0x0011,0x0011,0x0001,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x001a,0x0008,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x0009,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x001a,0x0018,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0001,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000}});
		mapDataList.add(7, new Integer[][]{
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x000e,0x000b,0x000b,0x000b,0x000b,0x0009,0x0008,0x000b,0x000b,0x000b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x0018,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x000e,0x000b,0x000b,0x000b,0x000b,0x000d,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x0005,0x0002,0x0011,0x0011,0x0013,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x0008,0x000b,0x000b,0x0009,0x001b,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x000f,0x0011,0x0011,0x0011,0x0011,0x0001,0x0000,0x0000,0x001a,0x0008,0x000b,0x000b,0x000b,0x000b,0x000b,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x000f,0x0011,0x0011,0x0011,0x0013,0x0002,0x0011,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000}});
		mapDataList.add(8, new Integer[][]{
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x000e,0x000b,0x000b,0x000b,0x000b,0x0009,0x0008,0x000b,0x000b,0x000b,0x000b,0x000b,0x000d,0x0000,0x0000,0x0000},
				{0x001a,0x0018,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0013,0x001b,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x000e,0x000b,0x0009,0x0008,0x000b,0x000d,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x0005,0x0002,0x0011,0x0011,0x0013,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x0008,0x000b,0x000b,0x0009,0x001b,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x000f,0x0011,0x0013,0x0018,0x0011,0x0001,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x001a,0x0008,0x000b,0x000b,0x000b,0x000d,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x0018,0x0011,0x0011,0x0013,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000}});
		mapDataList.add(9, new Integer[][]{
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000d,0x0000},
				{0x0011,0x0011,0x0011,0x0013,0x0018,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0013,0x0007,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x0007,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x0007,0x0000},
				{0x000e,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x0009,0x0007,0x0000},
				{0x001a,0x0018,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0013,0x0018,0x0011,0x0011,0x0011,0x0001,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x0008,0x000b,0x000b,0x0009,0x0008,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000d,0x0000},
				{0x000f,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0013,0x001b,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000},
				{0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x0009,0x001b,0x0000},
				{0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0001,0x0000}});
		mapDataList.add(10, new Integer[][]{
				{0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x001a,0x0008,0x000b,0x000b,0x000b,0x000b,0x000b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x000f,0x0010,0x0010,0x0010,0x0013,0x0018,0x0010,0x0000,0x0000,0x0000,0x000e,0x000b,0x000b,0x000b,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x001a,0x0018,0x0010,0x0010,0x0000},
				{0x000e,0x000b,0x000b,0x000b,0x000b,0x0009,0x001b,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x001a,0x0002,0x0011,0x0011,0x0011,0x0013,0x001b,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x001a,0x001b,0x0000,0x0000,0x0000,0x001a,0x0008,0x000b,0x000b,0x000b,0x000b,0x0009,0x0008,0x000b,0x000b,0x000d},
				{0x001a,0x0008,0x000b,0x000b,0x0000,0x000f,0x0010,0x0010,0x0010,0x0010,0x0010,0x0010,0x0010,0x0010,0x0013,0x001b},
				{0x000f,0x0010,0x0010,0x0010,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b},
				{0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x0009,0x001b},
				{0x0010,0x0010,0x0010,0x0010,0x0010,0x0010,0x0010,0x0010,0x0010,0x0010,0x0010,0x0010,0x0010,0x0010,0x0010,0x0001}});
		mapDataList.add(11, new Integer[][]{
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x001a,0x0016,0x001b,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x001a,0x0016,0x001b,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x001a,0x0016,0x001b,0x0000},
				{0x000b,0x000b,0x000b,0x0009,0x0008,0x000b,0x000b,0x000b,0x0009,0x0016,0x001b,0x0000},
				{0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0013,0x001b,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000},
				{0x0000,0x0000,0x000e,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x0009,0x001b,0x0000},
				{0x0000,0x0000,0x001a,0x0018,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0001,0x0000},
				{0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x001a,0x0008,0x000b,0x000b,0x000b,0x000b,0x000b,0x000d,0x0000,0x0000},
				{0x0000,0x0000,0x001a,0x0018,0x0011,0x0011,0x0011,0x0011,0x0013,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x000e,0x000b,0x000b,0x000b,0x000b,0x000b,0x0009,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0005,0x0016,0x0016,0x0016,0x0016,0x0016,0x0016,0x001b,0x0000,0x0000},
				{0x0000,0x0000,0x0005,0x0018,0x0011,0x0011,0x0011,0x0011,0x0011,0x0001,0x0000,0x0000},
				{0x0000,0x0000,0x0005,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000}});
		mapDataList.add(12, new Integer[][]{
				{0x0000,0x0000,0x0000,0x001a,0x0007,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x0008,0x000b,0x000b,0x000b,0x000b,0x000d,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x0018,0x0011,0x0011,0x0011,0x0013,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x001a,0x001b,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x001a,0x0008,0x000b,0x0009,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x000f,0x0011,0x0011,0x0011,0x0001,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x0008,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000d,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x000f,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0013,0x001b,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x0009,0x0008,0x000b,0x000b,0x0000},
				{0x0000,0x0000,0x0011,0x0011,0x0013,0x0018,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x000b,0x000b,0x000b,0x0009,0x0008,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b},
				{0x0000,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011}});
		mapDataList.add(13, new Integer[][]{
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x000b,0x000b,0x000b,0x000b,0x000d,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0011,0x0011,0x0011,0x0013,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x000e,0x000b,0x000b,0x000b,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x001a,0x0018,0x0011,0x0011,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x001a,0x0008,0x000b,0x000b,0x0009,0x0008,0x000b,0x000b,0x0009,0x0008,0x000b,0x000b,0x0000},
				{0x0000,0x0000,0x0000,0x000f,0x0011,0x0011,0x0011,0x0013,0x0018,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x000e,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x0009,0x0008,0x000b,0x000b,0x000b,0x000b,0x0000,0x0000,0x0000},
				{0x001a,0x0018,0x0011,0x0011,0x0011,0x0011,0x0011,0x0013,0x0002,0x0011,0x0011,0x0011,0x0011,0x0000,0x0000,0x0000},
				{0x001a,0x0007,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x0007,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x001a,0x0007,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x000b,0x0009,0x0008,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000d},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0013,0x001b},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b},
				{0x0000,0x0000,0x0000,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x000b,0x0009,0x001b},
				{0x0000,0x0000,0x0000,0x0011,0x0011,0x0011,0x0013,0x0018,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0011,0x0001},
				{0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x001a,0x001b,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000,0x0000}});
//		mapDataList.add(14, new Integer[][]);
//		mapDataList.add(15, new Integer[][]);
//		mapDataList.add(16, new Integer[][]);
//		mapDataList.add(17, new Integer[][]);
		
		//载入碰撞数据
		for(int i=0;i<hitDataList.length;i++){
			hitDataList[i]=new LinkedList<Rect>();
		}
		hitDataList[0].add(new Rect(0,31,48,48));
		hitDataList[0].add(new Rect(31,0,49,31));
		hitDataList[1].add(new Rect(32,30,49,80));
		hitDataList[1].add(new Rect(49,30,80,48));
		hitDataList[2].add(new Rect(31,19,48,80));
		hitDataList[2].add(new Rect(48,19,62,40));
		hitDataList[3].add(new Rect(30,40,49,80));
		hitDataList[4].add(new Rect(31,0,48,80));
		hitDataList[5].add(new Rect(31,0,48,80));
		hitDataList[6].add(new Rect(31,0,48,80));
		hitDataList[7].add(new Rect(31,0,48,48));
		hitDataList[7].add(new Rect(48,31,80,48));
		hitDataList[8].add(new Rect(0,31,48,48));
		hitDataList[8].add(new Rect(31,0,48,31));
		hitDataList[9].add(new Rect(0,31,80,48));
		hitDataList[9].add(new Rect(48,48,77,64));
		hitDataList[10].add(new Rect(0,31,80,48));
		hitDataList[11].add(new Rect(0,31,80,48));
		hitDataList[12].add(new Rect(0,31,48,48));
		hitDataList[12].add(new Rect(31,48,48,80));
		hitDataList[13].add(new Rect(31,31,48,80));
		hitDataList[13].add(new Rect(48,31,80,48));
		hitDataList[14].add(new Rect(31,0,48,48));
		hitDataList[14].add(new Rect(48,31,80,48));
		hitDataList[15].add(new Rect(0,31,80,48));
		hitDataList[16].add(new Rect(0,31,80,48));
		hitDataList[17].add(new Rect(0,31,80,48));
		hitDataList[18].add(new Rect(0,31,48,48));
		hitDataList[18].add(new Rect(31,48,48,80));
		hitDataList[19].add(new Rect(31,0,48,80));
		hitDataList[20].add(new Rect(31,0,48,80));
		hitDataList[20].add(new Rect(48,15,57,51));
		hitDataList[22].add(new Rect(31,0,48,80));
		hitDataList[23].add(new Rect(31,31,48,80));
		hitDataList[23].add(new Rect(48,31,80,48));
		hitDataList[24].add(new Rect(31,0,48,80));
		hitDataList[24].add(new Rect(22,20,31,52));
		hitDataList[25].add(new Rect(31,0,48,80));
		hitDataList[26].add(new Rect(31,0,48,80));
		hitDataList[28].add(new Rect(31,0,48,80));
		hitDataList[28].add(new Rect(22,20,31,52));
		hitDataList[29].add(new Rect(31,0,48,80));
		hitDataList[31].add(new Rect(31,0,48,80));
		hitDataList[31].add(new Rect(22,20,31,52));
		
	}
	/**
	 * 初始化游戏
	 */
	private void initPlay()
	{
		map_type=data_map_type[level];
		bg_index=data_bg_index[level];
		score_sum=0;
		
		readRole();
		
		gravity.set(0.0f, 0.0f);
		Iterator<Body> iterator=bodyVector.iterator();
		while(iterator.hasNext()){
			world.destroyBody(iterator.next());
		}
		world.setGravity(gravity);
		
		playState=PLAY_PREPARE;
	}
	/**
	 *  创建碰撞边界
	 * @param x
	 * @param y
	 * @param half_width
	 * @param half_height
	 */
	private void createBorder(float x, float y, float half_width,float half_height) {
		PolygonDef shape = new PolygonDef(); // 标识刚体的形状
		shape.density = 0; // 设置刚体的密度，应为这个是底边界，所以密度设为0，相当于没有质量的物体不受力
		shape.friction = 0.5f; // 摩擦力,学过物理吧….恩,就是这个意思…
		shape.restitution = 0.5f; // 弹力
		shape.setAsBox(half_width / RATE, half_height / RATE); // 设置刚体刚体的宽和高，要根据android坐标转换成世界当中的单位
		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(x / RATE, y / RATE); // 定义刚体的位置
		Body body = world.createBody(bodyDef); // 在世界中创建这个刚体
		body.createShape(shape); // 刚体形状
		body.setMassFromShapes(); // 计算质量
		bodyVector.add(body);
	}
	/**
	 * 创建滚动球
	 * @param x
	 * @param y
	 * @param radius
	 * @param i
	 */
	private void createBall(float x, float y, float radius, int i) {
		CircleDef shape = new CircleDef();
		shape.density = 7f;
		shape.friction = 11f;
		shape.restitution = 0.3f;
		shape.radius = radius / RATE;
		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(x / RATE, y / RATE);

		ball = world.createBody(bodyDef);
		ball.createShape(shape);
		ball.setMassFromShapes();
		bodyVector.add(ball);
	}
	/**
	 * 游戏中的初始化
	 */
	private void initFruit(){
		mapData=mapDataList.get(level);
		
		offset_x=0;
		offset_y=0;
		save_x=0;
		save_y=0;
		
//		column=data_xy_count[level][0];
//		row=data_xy_count[level][1];
		column=mapData[0].length;
		row=mapData.length;
		
		//初始化整个地图的边界
		bound_left=0;
		bound_top=0;
		bound_right=column*80+80;
		bound_bottom=row*80+80;
		//球体半径
		radius=data_radius[role_type];
		//创建碰撞边界
		bodyVector.clear();
		for (int i = 0; i < column; i++) {
			for (int j = 0; j < row; j++) {
				int n=mapData[j][i];
				if(n!=0){
					LinkedList<Rect> list = hitDataList[n - 1];
					int l = list.size();
					for (int k = 0; k < l; k++) {
						Rect rect = list.get(k);
						int half_w = rect.width() >> 1;
						int half_h = rect.height() >> 1;
						int x = i*80+rect.left + half_w;
						int y = j*80+rect.top + half_h;
						createBorder(x, y, half_w, half_h);
					}
				}
			}
		}
		//创建球体
		createBall(Common.data_fruit_position[level][0],Common.data_fruit_position[level][1],radius,0);
		
		//载入物品
		itemDataList.clear();//清空容器
		int[][] gold_data=Common.item_gold[level];//金币
		for(int i=0;i<gold_data.length;i++){
			itemDataList.add(new Item(gold_data[i][0],gold_data[i][1],ITEM_GOLD,true));
		}
		int[][] oil_data=Common.item_oil[level];//油渍
		for(int i=0;i<oil_data.length;i++){
			itemDataList.add(new Item(oil_data[i][0],oil_data[i][1],ITEM_OIL,true));
		}
		int[][] paper_data=Common.item_paper[level];//纸团
		for(int i=0;i<paper_data.length;i++){
			itemDataList.add(new Item(paper_data[i][0],paper_data[i][1],ITEM_PAPER,true));
		}
		int[][] wood_data=Common.item_wood[level];//纸团
		for(int i=0;i<wood_data.length;i++){
			itemDataList.add(new Item(wood_data[i][0],wood_data[i][1],ITEM_WOOD,true));
		}
		int[][] dead_data=Common.item_dead[level];//死亡区域
		for(int i=0;i<dead_data.length;i++){
			Item item=new Item(dead_data[i][0],dead_data[i][1],ITEM_DEAD,true);
			item.rect=new Rect(dead_data[i][0],dead_data[i][1],dead_data[i][0]+dead_data[i][2],dead_data[i][1]+dead_data[i][3]);
			itemDataList.add(item);
		}
		int[] win_data=Common.item_win[level];//胜利区域
		winItem=new Item(win_data[0],win_data[1],ITEM_WIN,true,win_data[2]);
		
		last_time=System.currentTimeMillis();
	}
	
	/**
	 * loading界面的逻辑
	 */
	private void onTimerLoading(int progress)
	{
		loadingindex=progress;
		if(loadingindex >= 100)
		{
			loadingindex = 100;
		}
		drawGame();
	}
	
	/**
	 * loading界面的绘制
	 */
	private void drawLoading()
	{
		fillRect(0, 0, LCD_WIDTH, LCD_HEIGHT, 0xff000000);
		int x = LCD_WIDTH-loadingBmp0.getWidth()>>1;
		int y = LCD_HEIGHT-loadingBmp0.getHeight()>>1;
		int w = loadingindex*loadingBmp1.getWidth()/100;
		drawBitmap(loadingBmp0, x, y, MODE_TOP);
		drawBitmap(loadingBmp1, x, y, 0,0,w,loadingBmp1.getHeight(),MODE_TOP);
		drawBitmap(loadingBmp2, x+w, y, MODE_MID_BOTTOM);
	}
	
	/**
	 * 菜单的逻辑
	 */
	private void onTimerMenu()
	{
		
	}
	
	/**
	 * 菜单的按键处理
	 * @param event
	 */
	private void onTouchMenu(MotionEvent event)
	{
		int x = (int)(event.getX()*LCD_WIDTH/GameActivity.actWidth);
		int y = (int)(event.getY()*LCD_HEIGHT/GameActivity.actHeight);
		if(event.getAction() == MotionEvent.ACTION_DOWN)
		{
			if(button_menu_sound.contains(x,y)){
				musicSwitch();
				sound.start(Sound.SOUND_PRESS,isMusic, isSound);
			}
			if(button_about.contains(x, y)){
				gamekey=1;
				sound.start(Sound.SOUND_PRESS,isMusic, isSound);
			}
			if (button_help.contains(x, y)) {
				gamekey=2;
				sound.start(Sound.SOUND_PRESS,isMusic, isSound);
			}else if(button_reward.contains(x,y)){
				gamekey=3;
				sound.start(Sound.SOUND_PRESS,isMusic, isSound);
			}else if(button_start.contains(x,y)){
				gamekey=4;
				sound.start(Sound.SOUND_PRESS,isMusic, isSound);
			}else if(button_quit.contains(x, y)){
				gamekey=5;
				sound.start(Sound.SOUND_PRESS,isMusic, isSound);
			}
		}
		if(event.getAction() == MotionEvent.ACTION_UP){
			if(gamekey==1){
				setGameState(STATE_ABOUT);
			}else if(gamekey==2){
				setGameState(STATE_HELP);
			}else if(gamekey==3){
				readScore();
				sortScore();
				slide_y=0;
				setGameState(STATE_REWARD);
			}else if(gamekey==4){
				setGameState(STATE_DIFFICULTY);
			}else if(gamekey==5){
				Log.i(TAG, "退出游戏");
				isexit=true;
			}
			gamekey=-1;
		}
	}
	
	/**
	 * 菜单的绘制
	 */
	private void drawMenu()
	{
		drawBitmap(menuBmp,0,0,MODE_TOP);
		drawBitmap(button_aboutBmp,button_about.left,button_about.top,MODE_TOP);
		
		drawBitmap(gamekey==4?menu_startBmp0:menu_startBmp1,button_start.left,button_start.top,MODE_TOP);
		drawBitmap(gamekey==2?menu_helpBmp0:menu_helpBmp1,button_help.left,button_help.top,MODE_TOP);
		drawBitmap(gamekey==3?menu_rewardBmp0:menu_rewardBmp1,button_reward.left,button_reward.top,MODE_TOP);
		drawBitmap(gamekey==5?menu_quitBmp0:menu_quitBmp1,button_quit.left,button_quit.top,MODE_TOP);
		
		drawBitmap(isMusic?sound1Bmp:sound0Bmp,button_menu_sound.left,button_menu_sound.top,MODE_TOP);
	}
	
	/**
	 * 游戏进行中绘制
	 */
	private void drawPlay()
	{
		int x=-offset_x;
		int y=-offset_y;
		//画背景
		drawBitmap(playBgBmp[bg_index][0],x,y,MODE_TOP);
		drawBitmap(playBgBmp[bg_index][0],x+LCD_WIDTH,y,MODE_TOP);
		drawBitmap(playBgBmp[bg_index][1],x,y+LCD_HEIGHT,MODE_TOP);
		drawBitmap(playBgBmp[bg_index][1],x+LCD_WIDTH,y+LCD_HEIGHT,MODE_TOP);
		drawBitmap(playBgBmp[bg_index][1],x,y+(LCD_HEIGHT<<1),MODE_TOP);
		drawBitmap(playBgBmp[bg_index][1],x+LCD_WIDTH,y+(LCD_HEIGHT<<1),MODE_TOP);
		drawBitmap(playBgBmp[bg_index][1],x,y+(LCD_HEIGHT*3),MODE_TOP);
		drawBitmap(playBgBmp[bg_index][1],x+LCD_WIDTH,y+(LCD_HEIGHT*3),MODE_TOP);
		//画管道
		for (int i = 0; i < column; i++) {
			for (int j = 0; j < row; j++) {
				int n=mapData[j][i];
				if(n!=0){
					drawBitmap(mapBmp[map_type][n-1],x+i*80,y+j*80,MODE_TOP);
				}
			}
		}
		//画物品(金币、油渍、纸团等)
		Iterator<Item> iterator=itemDataList.iterator();
		while(iterator.hasNext()){
			Item item=iterator.next();
			int i_x=item.x;
			int i_y=item.y;
			switch(item.type){
			case ITEM_GOLD:
				if(item.frame>0){
					if(item.isActive){
						drawBitmap(gold0Bmp,x+i_x,y+i_y,MODE_MID_MID);
					}else{
						if(item.frame<6){
							drawBitmap(gold0Bmp,x+i_x,y+i_y+(item.frame-10),MODE_MID_MID);
						}else{
							drawBitmap(gold1Bmp,x+i_x,y+i_y+(item.frame-10),MODE_MID_MID);
						}
						item.frame--;
					}
				}
				break;
			case ITEM_OIL:
				if (item.isActive) {
					drawBitmap(oil1Bmp, x + i_x, y + i_y, MODE_MID_MID);
				}else{
					drawBitmap(oil0Bmp, x + i_x, y + i_y, MODE_MID_MID);
				}
				break;
			case ITEM_PAPER:
				if(item.frame>0){
					if(item.isActive){
						drawBitmap(paper0Bmp, x + i_x, y + i_y, MODE_MID_MID);
					}else{
						if(item.frame<6){
							drawBitmap(paper2Bmp, x + i_x, y + i_y, MODE_MID_MID);
						}else{
							drawBitmap(paper1Bmp, x + i_x, y + i_y, MODE_MID_MID);
						}
						item.frame--;
					}
				}
				break;
			case ITEM_WOOD:
				drawBitmap(woodBmp, x + i_x, y + i_y, MODE_MID_MID);
				break;
			}
		}
		//画篮子口
		switch(winItem.angle){
		case 0:
			drawTranImg(basket1Bmp,x+winItem.x-36,y+winItem.y-35,0);
			break;
		case 90:
			drawTranImg(basket1Bmp,x+winItem.x-15,y+winItem.y-14,90);
			break;
		case 180:
			break;
		case 270:
			drawTranImg(basket1Bmp,x+winItem.x-60,y+winItem.y-15,270);
			break;
		}
		//画球体
		drawTranImg(fruitBmp[role_type][0], x+b_x-radius, y+b_y-radius, angle);
		//画篮子
		switch(winItem.angle){
		case 0:
			drawTranImg(basket0Bmp,x+winItem.x-40,y+winItem.y-35,0);
			break;
		case 90:
			drawTranImg(basket0Bmp,x+winItem.x-40,y+winItem.y-35,90);
			break;
		case 180:
			break;
		case 270:
			drawTranImg(basket0Bmp,x+winItem.x-40,y+winItem.y-35,270);
			break;
		}
//		drawBitmap(clockBmp,577,3,MODE_TOP);//剩余时间
//		drawBitmap(num[time0],660,10,MODE_TOP);
//		drawBitmap(num[time1],696,10,MODE_TOP);
//		drawBitmap(scoreBmp,300,3,MODE_TOP);//得分
//		drawBitmap(num[score0],390,10,MODE_TOP);
//		drawBitmap(num[score1],426,10,MODE_TOP);
		
		if (gameState==STATE_PLAY) {
			drawBitmap(pause1Bmp, button_play_pause.left,button_play_pause.top, MODE_TOP);
			drawBitmap(isMusic ? sound1Bmp : sound0Bmp, button_play_music.left,button_play_music.top, MODE_TOP);
		}
	}
	
	/**
	 * 游戏进行中逻辑
	 */
	private void onTimerPlay()
	{
		if(playState==PLAY_PREPARE){
			initFruit();
			playState=PLAY_PLAY;
			last_time=System.currentTimeMillis();
		}
		
		if (playState==PLAY_PLAY) {
			long current_time = System.currentTimeMillis();
			long time = current_time - last_time;
//			left_time-=time;
			last_time = System.currentTimeMillis();
			
//			time0=(int)(left_time/10000);
//			time1=(int)(left_time/1000)-time0*10;
			score0=(int)(score_sum/10);
			score1=score_sum-score0*10;
			
			gravity.set(g_x, g_y);
			world.setGravity(gravity);
			 // 开始模拟
			world.step(timeStep, iterations);
			//获取球的状态
			Vec2 position=ball.getPosition();
			b_x=(int) (position.x*RATE);
			b_y=(int) (position.y*RATE);
			float angle0=ball.getAngle();
			angle=(int) (360*(angle0/(PI*2)));
			//判断是否出边界
			if(b_x<bound_left||b_y<bound_top||b_x>bound_right||b_y>bound_bottom){
				setGameState(STATE_OVER);
				role_score[role_type]+=score_sum;
				saveScore("score"+role_type,role_score[role_type]);
				sound.start(Sound.SOUND_FAIL, isMusic, isSound);
			}
			//屏幕随球滚动
			rollScreenByBall();
			
			Iterator<Item> iterator=itemDataList.iterator();
			while(iterator.hasNext()){
				Item item=iterator.next();
				int i_x=item.x;
				int i_y=item.y;
				int r=item.r;
				double dis=distance(b_x, b_y, i_x, i_y);
				switch(item.type){
				case ITEM_GOLD:
					if(dis<r+radius&&item.isActive){
						item.isActive=false;
						score_sum++;
						sound.reSet(Sound.SOUND_GOLD, isMusic, isSound);
					}
					break;
				case ITEM_OIL:
					if(item.isActive){
						if(dis<r){
							Vec2 v=ball.getLinearVelocity();
							v.x=v.x*1.6f;
							v.y=v.y*1.6f;
							item.isActive=false;
							sound.start(Sound.SOUND_OIL, isMusic, isSound);
						}
					}else{
						if(dis>r){
							item.isActive=true;
						}
					}
					break;
				case ITEM_PAPER:
					if(item.isActive&&dis<r){
						Vec2 v=ball.getLinearVelocity();
						v.x=v.x/2f;
						v.y=v.y/2f;
						item.isActive=false;
						sound.reSet(Sound.SOUND_PAPER, isMusic, isSound);
					}
					break;
				case ITEM_WOOD:
					if(item.isActive){
						if(dis<r){
							Vec2 v=ball.getLinearVelocity();
							v.x=-v.x;
							v.y=-v.y;
							item.isActive=false;
							sound.reSet(Sound.SOUND_WOOD, isMusic, isSound);
						}
					}else{
						if(dis>r){
							item.isActive=true;
						}
					}
					break;
				case ITEM_DEAD:
					if(item.rect.contains(b_x, b_y)){
						setGameState(STATE_OVER);
						role_score[role_type]+=score_sum;
						saveScore("score"+role_type,role_score[role_type]);
						sound.start(Sound.SOUND_FAIL, isMusic, isSound);
					}
					break;
				case ITEM_WIN:
					break;
				}
			}
			if(distance(winItem.x, winItem.y, b_x, b_y)<winItem.r){
				setGameState(STATE_WIN);
				role_score[role_type]+=score_sum;
				saveScore("score"+role_type,role_score[role_type]);
				sound.start(Sound.SOUND_WIN, isMusic, isSound);
			}
		}
	}
	
	/**
	 * 游戏进行中点击处理
	 * @param event
	 */
	private void onTouchPlay(MotionEvent event)
	{
		int x = (int) (event.getX() * LCD_WIDTH / GameActivity.actWidth);
		int y = (int) (event.getY() * LCD_HEIGHT / GameActivity.actHeight);
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			
			if(button_play_pause.contains(x, y)){
				gamekey=1;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}else if(button_play_music.contains(x, y)){
				musicSwitch();
				sound.start(Sound.SOUND_PRESS,isMusic, isSound);
			}else{
				isSlide=true;
				start_x=x;
				start_y=y;
				save_x=offset_x;
				save_y=offset_y;
			}
			
		}
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (isSlide) {
				offset_x = save_x + (start_x - x);
				offset_y = save_y + (start_y - y);
				if (offset_x < bound_left) {
					offset_x = bound_left;
					save_x = offset_x;
					start_x = x;
				}
				if (offset_x > bound_right - LCD_WIDTH) {
					offset_x = bound_right - LCD_WIDTH;
					save_x = offset_x;
					start_x = x;
				}
				if (offset_y < bound_top) {
					offset_y = bound_top;
					save_y = offset_y;
					start_y = y;
				}
				if (offset_y > bound_bottom - LCD_HEIGHT) {
					offset_y = bound_bottom - LCD_HEIGHT;
					save_y = offset_y;
					start_y = y;
				}
				//屏幕随球滚动
				rollScreenByBall();
			}
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			if(gamekey==1){
				isSlide=false;
				setGameState(STATE_MIDDLE_MENU);
			}
			gamekey=-1;
			isSlide=false;
		}
	}
	
	/**
	 * 如果球到边界则相应地移动屏幕
	 */
	private void rollScreenByBall(){
		if(b_x-(bound_left+offset_x)<EDGE){
			offset_x=b_x-EDGE;
			if(offset_x<bound_left){
				offset_x=bound_left;
			}
		}
		if(b_y-(bound_top+offset_y)<EDGE){
			offset_y=b_y-EDGE;
			if(offset_y<bound_top){
				offset_y=bound_top;
			}
		}
		if(offset_x+LCD_WIDTH-b_x<EDGE){
			offset_x=b_x+EDGE-LCD_WIDTH;
			if(offset_x+LCD_WIDTH>bound_right){
				offset_x=bound_right-LCD_WIDTH;
			}
		}
		if(offset_y+LCD_HEIGHT-b_y<EDGE){
			offset_y=b_y+EDGE-LCD_HEIGHT;
			if(offset_y+LCD_HEIGHT>bound_bottom){
				offset_y=bound_bottom-LCD_HEIGHT;
			}
		}
	}
	/**
	 * 游戏帮助绘制
	 */
	private void drawHelp()
	{
		drawBitmap(helpBmp,0,0,MODE_TOP);
//		drawBitmap(isMusic?sound1Bmp:sound0Bmp,button_help_sound.left,button_help_sound.top,MODE_TOP);
	}
	
	/**
	 * 游戏帮助逻辑
	 */
	private void onTimerHelp()
	{
		
	}
	
	/**
	 * 游戏帮助点击处理
	 * @param event
	 */
	private void onTouchHelp(MotionEvent event)
	{
		int x = (int)(event.getX()*LCD_WIDTH/GameActivity.actWidth);
		int y = (int)(event.getY()*LCD_HEIGHT/GameActivity.actHeight);
		if(event.getAction() == MotionEvent.ACTION_DOWN)
		{
			if(button_help_return.contains(x,y)){
				gamekey=1;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
//			if(button_help_sound.contains(x, y)){
//				musicSwitch();
//				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
//			}
		}
		if(event.getAction() == MotionEvent.ACTION_UP){
			if(gamekey==1){
				setGameState(lastState);
				sound.stop(Sound.SOUND_HELP);
			}
			gamekey=-1;
		}
	}
	/**
	 *积分榜界面的按键处理
	 * @param event
	 */
	private void onTouchReward(MotionEvent event){
		int x = (int)(event.getX()*LCD_WIDTH/GameActivity.actWidth);
		int y = (int)(event.getY()*LCD_HEIGHT/GameActivity.actHeight);
		int o_x=238;
		int o_y=137;//列表锚点坐标
		int w=273;//每行宽度
		int h=110;//每行高度
		if(event.getAction() == MotionEvent.ACTION_DOWN)
		{
			if(button_reward_return.contains(x,y)){
				gamekey=1;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}else if(area_role_choose.contains(x, y)){
				
				int choose=(y+slide_y-o_y)/h;
				if(choose>=0&&choose<roleBmp.length){
					role_type=choose;
				}
				
			}
			if(x>178&&x<678){
				isSlide=true;
				save_y=slide_y;
				start_y=y;
			}
		}
		if(event.getAction()==MotionEvent.ACTION_MOVE){
			if (isSlide) {
				slide_y = save_y + start_y - y;
				if (slide_y < 0) {
					slide_y = 0;
				}
				if (slide_y > (roleBmp.length - 3) * h) {
					slide_y = (roleBmp.length - 3) * h;
				}
			}
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			if(gamekey==1){
				setGameState(lastState);
			}
			gamekey=-1;
			isSlide=false;
		}
	}
	/**
	 * 绘制积分榜界面
	 */
	private void drawReward(){
//		fillRect(0, 0, LCD_WIDTH, LCD_HEIGHT, 0xff000000);
		drawBitmap(rewardBmp,0,0,MODE_TOP);
		int o_x=238;
		int o_y=137;//列表锚点坐标
		int w=273;//每行宽度
		int h=110;//每行高度
		for(int i=0;i<roleBmp.length;i++){
			int y=o_y-slide_y+i*h;
			int score[]=new int[6];
			int temp=role_score[role_sort[i]];
			for(int j=score.length-1;j>0;j--){
				score[j]=(int) (temp/(Math.pow(10, j)));
				temp-=score[j]*(Math.pow(10, j));
			}
			score[0]=temp;
			if (y>o_y-70&&y+55<460) {
				drawBitmap(nBmp[i+1], o_x + 2 ,y +(h>>1),MODE_MID_MID);
				drawBitmap(roleBmp[role_sort[i]], o_x +70, y + (h >> 1), MODE_MID_MID);
				for(int j=0;j<score.length;j++){
					drawBitmap(nBmp[score[score.length-j-1]], o_x + 160 +j*36,y +(h>>1),MODE_MID_MID);
				}
			}
		}
		drawBitmap(reward1Bmp,133,10,MODE_TOP);
		drawBitmap(reward2Bmp,215,402,MODE_TOP);
	}
	/**
	 * 难度选择界面逻辑
	 */
	private void onTimerDifficulty(){
		
	}
	/**
	 * 难度选择界面点击事件处理
	 */
	private void onTouchDifficulty(MotionEvent event){
		int x = (int)(event.getX()*LCD_WIDTH/GameActivity.actWidth);
		int y = (int)(event.getY()*LCD_HEIGHT/GameActivity.actHeight);
		if(event.getAction() == MotionEvent.ACTION_DOWN)
		{
			if (button_difficulty_return.contains(x, y)) {
				gamekey=1;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
			if(button_difficulty_sound.contains(x, y)){
				musicSwitch();
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
			if (button_difficulty_normal.contains(x, y)) {
				gamekey=2;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
//			if (button_difficulty_mysterious.contains(x, y)) {
//				levelState=LEVEL_MYSTERIOUS;
//				setGameState(STATE_LEVEL);
//				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
//			}
			if (button_difficulty_role.contains(x, y)) {
				gamekey=3;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
		}
		if(event.getAction() == MotionEvent.ACTION_UP){
			if(gamekey==1){
				setGameState(STATE_MENU);
			}else if(gamekey==2){
				levelState=LEVEL_NORMAL;
				setGameState(STATE_LEVEL);
			}else if(gamekey==3){
				slide_y=role_type*110;
				if (slide_y < 0) {
					slide_y = 0;
				}
				if (slide_y > (roleBmp.length - 3) * 110) {
					slide_y = (roleBmp.length - 3) * 110;
				}
				setGameState(STATE_ROLE);
			}
			gamekey=-1;
		}
	}
	/**
	 * 难度选择界面绘制
	 */
	private void drawDifficulty(){
//		fillRect(0, 0, LCD_WIDTH, LCD_HEIGHT, 0xffffffff);
		drawBitmap(difficultyBmp,0,0,MODE_TOP);
		drawBitmap(gamekey==1?difficulty_returnBmp0:difficulty_returnBmp1,button_difficulty_return.left,button_difficulty_return.top,MODE_TOP);
		drawBitmap(gamekey==2?difficulty_normalBmp0:difficulty_normalBmp1,button_difficulty_normal.left,button_difficulty_normal.top,MODE_TOP);
		drawBitmap(gamekey==3?difficulty_roleBmp0:difficulty_roleBmp1,button_difficulty_role.left,button_difficulty_role.top,MODE_TOP);
		drawBitmap(isMusic?sound1Bmp:sound0Bmp,button_difficulty_sound.left,button_difficulty_sound.top,MODE_TOP);
	}
	/**
	 * 难度选择界面逻辑
	 */
	private void onTimerLevel(){
		if(flag_slide==1){
			slide_x+=50;
			if(slide_x>850){
				page_num--;
				flag_slide=0;
			}
		}else if(flag_slide==-1){
			slide_x -= 50;
			if (slide_x < -850) {
				page_num++;
				flag_slide=0;
			}
		}
	}
	/**
	 * 难度选择界面点击事件处理
	 */
	private void onTouchLevel(MotionEvent event){
		int x = (int)(event.getX()*LCD_WIDTH/GameActivity.actWidth);
		int y = (int)(event.getY()*LCD_HEIGHT/GameActivity.actHeight);
		if(event.getAction() == MotionEvent.ACTION_DOWN)
		{
			if (button_level_return.contains(x, y)) {
				gamekey=1;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
			if(button_difficulty_sound.contains(x, y)){
				musicSwitch();
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
			
			if(flag_slide==0){
				if(button_level_back.contains(x, y)){
					if(page_num>0){
						slide_x=0;
						flag_slide=1;
					}
					sound.start(Sound.SOUND_PRESS, isMusic, isSound);
				}
				if(button_level_forward.contains(x, y)){
					if(page_num<((LEVEL-1)/12)){
						slide_x=0;
						flag_slide=-1;
					}
					sound.start(Sound.SOUND_PRESS, isMusic, isSound);
				}
				int t_x=(x-198)/133;
				int t_y=(y-129)/86;
//				Log.i(TAG, "tx="+t_x+",ty="+t_y);
				if(x>198&&y>129&&t_x<4&&t_y<3&&x<(198+t_x*133+63)&&y<(129+t_y*133+66)){
					sound.start(Sound.SOUND_PRESS, isMusic, isSound);
					level=t_y*4+t_x+page_num*12;
					Log.i(TAG, "level="+level);
					if(level<LEVEL){
						initPlay();
						playState=PLAY_PREPARE;
						setGameState(STATE_PLAY);
					}
				}
			}
		}
		if(event.getAction() == MotionEvent.ACTION_UP){
			if(gamekey==1){
				setGameState(STATE_DIFFICULTY);
			}
			gamekey=-1;
		}
	}
	/**
	 * 难度选择界面绘制
	 */
	private void drawLevel(){
//		fillRect(0, 0, LCD_WIDTH, LCD_HEIGHT, 0xffffffff);
		drawBitmap(playBmp,0,0,MODE_TOP);
		drawBitmap(leafBmp,0,0,MODE_TOP);
		if(flag_slide==0){
			drawBitmap(btn_backBmp,button_level_back.left,button_level_back.top,MODE_TOP);
			drawBitmap(btn_forwardBmp,button_level_forward.left,button_level_forward.top,MODE_TOP);
			for(int i=0;i<12;i++){
				int x=198+(i%4)*133;
				int y=129+(i/4)*86;
				if((i+page_num*12)>=LEVEL){
					drawBitmap(lockBmp,x,y,MODE_TOP);
				}else{
					drawBitmap(num[i+page_num*12],x,y,MODE_TOP);
				}
			}
		}else{
			for(int i=0;i<12;i++){
				int x=198+(i%4)*133;
				int y=129+(i/4)*86;
				if (flag_slide==-1) {
					int convert_x=x + slide_x+ 854;
					if (convert_x<854) {
						if ((i + (page_num + 1) * 12)<LEVEL) {
							drawBitmap(num[i + (page_num + 1) * 12], convert_x,y, MODE_TOP);
						}else{
							drawBitmap(lockBmp, convert_x,y, MODE_TOP);
						}
					}
				}else if(flag_slide==1){
					int convert_x=x + slide_x- 854;
					if (convert_x>-50) {
						drawBitmap(num[i + (page_num - 1) * 12], convert_x, y, MODE_TOP);
					}
				}
				int current_x=x+slide_x;
				if (current_x>-50&&current_x<854) {
					if ((i + page_num * 12)<LEVEL) {
						drawBitmap(num[i + page_num * 12], x + slide_x, y,MODE_TOP);
					}else{
						drawBitmap(lockBmp,x + slide_x, y, MODE_TOP);
					}
				}
			}
		}
		
		drawBitmap(button_returnBmp,button_level_return.left,button_level_return.top,MODE_TOP);
		drawBitmap(isMusic?sound1Bmp:sound0Bmp,button_difficulty_sound.left,button_difficulty_sound.top,MODE_TOP);
	}
	
	/**
	 * 副菜单界面逻辑
	 */
	private void onTimerMiddlemenu(){
	}
	/**
	 * 副菜单界面点击逻辑
	 */
	private void onTouchMiddlemenu(MotionEvent event){
		int x = (int)(event.getX()*LCD_WIDTH/GameActivity.actWidth);
		int y = (int)(event.getY()*LCD_HEIGHT/GameActivity.actHeight);
		if(event.getAction() == MotionEvent.ACTION_DOWN)
		{
//			if(button_play_music.contains(x, y)){
//				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
//				musicSwitch();
//			}
			if (button_middlemenu_pause.contains(x, y)) {
				gamekey=1;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
			if (button_middlemenu_menu.contains(x, y)) {
				gamekey=2;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
			if (button_middlemenu_help.contains(x, y)) {
				gamekey=3;
				sound.start(Sound.SOUND_PRESS,isMusic, isSound);
			}
			if (button_middlemenu_music.contains(x, y)) {
				musicSwitch();
				sound.start(Sound.SOUND_PRESS,isMusic, isSound);
			}
		}
		if(event.getAction() == MotionEvent.ACTION_UP){
			if(gamekey==1){
				setGameState(STATE_PLAY);
				last_time=System.currentTimeMillis();
			}else if(gamekey==2){
				setGameState(STATE_VERIFY);
			}else if(gamekey==3){
				setGameState(STATE_HELP);
			}
			
			gamekey=-1;
		}
	}
	/**
	 * 副菜单界面绘制
	 */
	private void drawMiddlemenu(){
		drawPlay();
//		drawBitmap(middleMenuBmp, 0, 0, MODE_TOP);
//		drawBitmap(Common.isBgMusicPlay?play_musicBmp:play_music0Bmp,button_play_music.left,button_play_music.top,MODE_TOP);

		drawBitmap(maskBgBmp,0,0,MODE_TOP);
		drawBitmap(middleMenuBmp , 702, 11, MODE_TOP);
		drawBitmap(pause1Bmp,button_middlemenu_pause.left, button_middlemenu_pause.top, MODE_TOP);
		drawBitmap(btn_menuBmp,button_middlemenu_menu.left, button_middlemenu_menu.top, MODE_TOP);
		drawBitmap(btn_helpBmp,button_middlemenu_help.left, button_middlemenu_help.top, MODE_TOP);
		drawBitmap(isMusic?sound1Bmp:sound0Bmp,button_middlemenu_music.left, button_middlemenu_music.top, MODE_TOP);
	}
	
	/**
	 * 游戏结束点击事件
	 */
	private void onTouchGameover(MotionEvent event){
		int x = (int)(event.getX()*LCD_WIDTH/GameActivity.actWidth);
		int y = (int)(event.getY()*LCD_HEIGHT/GameActivity.actHeight);
		if(event.getAction() == MotionEvent.ACTION_DOWN){
			if (button_fail_retry.contains(x, y)) {
				gamekey=1;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
			if(button_fail_menu.contains(x, y)){
				gamekey=2;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
		}
		if(event.getAction() == MotionEvent.ACTION_UP){
			if(gamekey==1){
				initPlay();
				playState=PLAY_PREPARE;
				setGameState(STATE_PLAY);
			}else if(gamekey==2){
				setGameState(STATE_VERIFY);
			}
			gamekey=-1;
		}
	}
	/**
	 * 游戏结束逻辑
	 */
	private void onTimerGameover(){
	}
	/**
	 * 游戏结束界面的绘制
	 */
	private void drawGameover() {
		drawBitmap(failBgBmp,0,0,MODE_TOP);
		drawBitmap(nBmp[score0],430,170,MODE_TOP);
		drawBitmap(nBmp[score1],470,170,MODE_TOP);
	}
	
	/**
	 * 游戏胜利点击事件
	 */
	private void onTouchWin(MotionEvent event){
		if(delay_time>0)
			return;
		int x = (int)(event.getX()*LCD_WIDTH/GameActivity.actWidth);
		int y = (int)(event.getY()*LCD_HEIGHT/GameActivity.actHeight);
		if(event.getAction() == MotionEvent.ACTION_DOWN){
			if (button_win_next.contains(x, y)) {
				gamekey=1;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
			if(button_fail_menu.contains(x, y)){
				gamekey=2;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
			
		}
		if(event.getAction() == MotionEvent.ACTION_UP){
			if(gamekey==1){
				if (level<(LEVEL-1)) {
					level++;
					initPlay();
					playState = PLAY_PREPARE;
					setGameState(STATE_PLAY);
				}else{
					setGameState(STATE_MENU);
				}
			}else if(gamekey==2){
				setGameState(STATE_VERIFY);
			}
			gamekey=-1;
		}
	}
	/**
	 * 游戏胜利逻辑
	 */
	private void onTimerWin(){
	}
	/**
	 * 游戏胜利界面的绘制
	 */
	private void drawWin() {
		drawBitmap(winBgBmp,0,0,MODE_TOP);
		drawBitmap(nBmp[score0],370,165,MODE_TOP);
		drawBitmap(nBmp[score1],410,165,MODE_TOP);
//		drawBitmap(lightBmp[frame_light/3],854>>1,240,MODE_MID_MID);
//		frame_light++;
//		if(frame_light>8){
//			frame_light=0;
//		}
		
	}
	
	/**
	 * 游戏返回主菜单确认点击事件
	 */
	private void onTouchVerify(MotionEvent event){
		int x = (int)(event.getX()*LCD_WIDTH/GameActivity.actWidth);
		int y = (int)(event.getY()*LCD_HEIGHT/GameActivity.actHeight);
		if(event.getAction() == MotionEvent.ACTION_DOWN){
			if (button_verify_cancel.contains(x, y)) {
				setGameState(lastState);
			}
			if(button_verify_ok.contains(x, y)){
				setGameState(STATE_MENU);
			}
		}
	}
	/**
	 * 游戏返回主菜单确认逻辑
	 */
	private void onTimerVerify(){
		
	}
	/**
	 * 游戏返回主菜单界面的绘制
	 */
	private void drawVerify() {
//		drawBitmap(playBgBmp,0,0,MODE_TOP);
		drawBitmap(verifyBmp,0,0,MODE_TOP);
//		drawBitmap(verify_okBmp,button_verify_ok.left,button_verify_ok.top,MODE_TOP);
//		drawBitmap(verify_cancelBmp,button_verify_cancel.left,button_verify_cancel.top,MODE_TOP);
	}
	
	/**
	 * 角色选择点击事件
	 */
	private void onTouchRole(MotionEvent event){
		int x = (int)(event.getX()*LCD_WIDTH/GameActivity.actWidth);
		int y = (int)(event.getY()*LCD_HEIGHT/GameActivity.actHeight);
		
		int o_x=54;
		int o_y=94;//列表锚点坐标
		int w=273;//每行宽度
		int h=110;//每行高度
		
		if(event.getAction() == MotionEvent.ACTION_DOWN){
			if (button_role_return.contains(x, y)) {
				gamekey=1;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}else if(area_role_choose.contains(x, y)&&x<165){
				
				int choose=(y+slide_y-o_y)/h;
				if(choose>=0&&choose<roleBmp.length){
					role_type=choose;
					saveRole(role_type);
				}
				
			}
			if(x>area_role_choose.left&&x<area_role_choose.right){
				isSlide=true;
				save_y=slide_y;
				start_y=y;
			}
		}
		if(event.getAction()==MotionEvent.ACTION_MOVE){
			if (isSlide) {
				slide_y = save_y + start_y - y;
				if (slide_y < 0) {
					slide_y = 0;
				}
				if (slide_y > (roleBmp.length - 3) * h) {
					slide_y = (roleBmp.length - 3) * h;
				}
			}
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			if(gamekey==1){
				setGameState(STATE_DIFFICULTY);
			}
			gamekey=-1;
			isSlide=false;
		}
	}
	/**
	 * 角色选择逻辑
	 */
	private void onTimerRole(){
	}
	/**
	 * 角色选择界面绘制
	 */
	private void drawRole() {
		drawBitmap(playBmp,0,0,MODE_TOP);
		drawBitmap(role_frameBmp,29,21,MODE_TOP);
		drawBitmap(introduceBmp[role_type],442,147,MODE_TOP);

		int o_x=54;
		int o_y=94;//列表锚点坐标
		int w=273;//每行宽度
		int h=110;//每行高度
		for(int i=0;i<roleBmp.length;i++){
			int y=o_y-slide_y+i*h;
			if (y>area_role_choose.top-55&&y+55<area_role_choose.bottom) {
				if(i==role_type){
					drawBitmap(bar_chooseBmp,o_x+130,y+(h>>1),MODE_MID_MID);
				}
				drawBitmap(roleBmp[i], o_x + 50, y + (h >> 1), MODE_MID_MID);
			}
		}
		drawBitmap(leafBmp,0,0,MODE_TOP);
		drawBitmap(role_bottomBmp,0,371,MODE_TOP);
		drawBitmap(button_returnBmp,button_role_return.left,button_role_return.top,MODE_TOP);
	}
	
	/**
	 * 关于 点击事件
	 */
	private void onTouchAbout(MotionEvent event){
		int x = (int)(event.getX()*LCD_WIDTH/GameActivity.actWidth);
		int y = (int)(event.getY()*LCD_HEIGHT/GameActivity.actHeight);
		if(event.getAction() == MotionEvent.ACTION_DOWN){
			if (button_about_return.contains(x,y)) {
				gamekey=1;
				sound.start(Sound.SOUND_PRESS, isMusic, isSound);
			}
		}
		if(event.getAction() == MotionEvent.ACTION_UP){
			if(gamekey==1){
				setGameState(lastState);
			}
			gamekey=-1;
		}
	}
	/**
	 * 关于 逻辑
	 */
	private void onTimerAbout(){
		
	}
	/**
	 * 关于 界面绘制
	 */
	private void drawAbout() {
		drawBitmap(playBmp,0,0,MODE_TOP);
		drawBitmap(button_returnBmp,button_about_return.left,button_about_return.top,MODE_TOP);
	}
}
