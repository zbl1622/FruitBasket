package duohe.zbl.fruitbasket;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class LogoMediaView extends SurfaceView implements SurfaceHolder.Callback {

    public static boolean isClose = true;//
    MediaPlayer media = null;
    private GameActivity gameact = null;

    public LogoMediaView(GameActivity context) {
        super(context);
        gameact = context;
        if (!isClose) {
            media = new MediaPlayer();
            getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            getHolder().addCallback(this);//
            setFocusableInTouchMode(true);//
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        media.setDisplay(getHolder());
        try {
            AssetFileDescriptor afd = null;
            AssetManager am = this.getResources().getAssets();
            if (GameActivity.actWidth > GameActivity.actHeight)//
            {
                if (GameActivity.actWidth > 320) {
                    afd = am.openFd("logomedia.mp4");
                } else {
                    afd = am.openFd("logomedia.3gp");
                }
            } else {
                if (GameActivity.actHeight > 320) {
                    afd = am.openFd("logomedia1.mp4");
                } else {
                    afd = am.openFd("logomedia1.3gp");
                }
            }
            media.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
//			media.setDataSource("logomedia.mpg");
//			media.setDataSource("logomedia.3gp");
            media.prepare();
            media.setLooping(false);
//			media.prepare();
            media.start();
            media.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    media.release();
                    gameact.goGame();
                }
            });
        } catch (Exception e1) {
            gameact.goGame();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return true;
    }
}
