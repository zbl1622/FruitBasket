package duohe.zbl.fruitbasket;

import android.app.Activity;
import android.os.Bundle;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

/**
 * activety类
 * @author offel
 * 创建时间: 2012-8-6
 * 修改时间: 
 */
public class GameActivity extends Activity {
	LogoMediaView logomedia;
	public static int actWidth = 0;
	public static int actHeight = 0;
	private static GameCanvas gamecanvas = null;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreen();
//        if(gamecanvas == null)
//        	gamecanvas = new GameCanvas(this);
//        setContentView(gamecanvas);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//游戏运行中保持亮度
        if(LogoMediaView.isClose)
        {
        	goGame();
        }else if(logomedia == null)
        {
        	logomedia = new LogoMediaView(this);
            setContentView(logomedia);
        }
    }
    
    /**
     * 设置全屏和取得屏幕的宽高
     */
    public void setFullScreen()
    {
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN ,  
                  WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	
    	Display dis = getWindowManager().getDefaultDisplay();
    	actWidth = dis.getWidth();
    	actHeight = dis.getHeight();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	if (gamecanvas!=null) {
			gamecanvas.hideNotify();
		}
    }
    
    @Override
	protected void onResume() {
		super.onResume();
//		gamecanvas.resumeGame();
	}
    
    public void goGame()
    {
    	if(gamecanvas == null)
        {
    		LogoMediaView.isClose = true;
        	gamecanvas = new GameCanvas(this);
        	setContentView(gamecanvas);
        }
    }
}

