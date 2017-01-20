package duohe.zbl.fruitbasket;

import android.content.Context;
import android.media.MediaPlayer;

/**
 * 游戏音乐类
 * @author offel
 * 创建时间:2012-8-6
 * 修改时间:
 */
public class Sound {
	
	private Context context = null;
	//音乐
	public static final int SOUND_BG = 0;//背景音乐
	public static final int SOUND_PRESS= 1;//按键音效
	public static final int SOUND_REWARD = 2;//获得勋章
	public static final int SOUND_WIN = 3;//游戏胜利
	public static final int SOUND_FAIL= 4;//游戏失败
	public static final int SOUND_HELP = 5;//帮助语音
	public static final int SOUND_GOLD = 6;//金币
	public static final int SOUND_OIL = 7;//油渍
	public static final int SOUND_PAPER = 8;//纸团
	public static final int SOUND_WOOD = 9;//木板
	public static final int SOUND_LOSE = 10;//关卡失败
	public static final int SOUND_11 = 11;//
	public static final int SOUND_12 = 12;//
	public static final int MAXSOUND = 13;
	private static MediaPlayer sound[] = null;
	private static boolean isMusic[] = null;
//	private int soundLevel = 0;
	public Sound(Context context)
	{
		sound = new MediaPlayer[MAXSOUND];
		isMusic = new boolean[MAXSOUND];
		this.context = context;
//		setLevel(soundlevel);
	}
	
	/**
	 * 创建一个音乐或音效
	 * @param soundid ID号.
	 * @param soundname 名称.R文件中ID号.
	 * @param ismuick true:音乐,false 音效
	 */
	public void createSound(int soundid,int soundname, boolean ismusic)
	{
		if(soundid < 0 || soundid >= MAXSOUND)
			return;
		if(sound[soundid] == null)
		{
			sound[soundid] = MediaPlayer.create(context, soundname);
			isMusic[soundid] = ismusic;
		}
	}
	
	/**
	 * 播放音乐 
	 * @param soundid 音乐ID号
	 * @param isstart 音乐的开关
	 * @param isM 音效的开关
	 */
	public void start(int soundid, boolean isstart, boolean isM)
	{
		if(soundid < 0 || soundid >= MAXSOUND)
			return;
		if(isMusic[soundid])
		{
			if(!isM)
				return;
			sound[soundid].setLooping(true);
		}else
		{
			if(!isstart)
				return;
			sound[soundid].setLooping(false);
		}
		if(sound[soundid] != null && !sound[soundid].isPlaying())
		{
			sound[soundid].start();
		}
	}
	
	/**
	 * 暂停音乐
	 * @param soundid ID号.
	 */
	public void paused(int soundid)
	{
		if(soundid < 0 || soundid >= MAXSOUND)
			return;
		if(sound[soundid] != null)// && sound[soundid].isPlaying())
		{
			sound[soundid].pause();
		}
	}
	/**
	 * 设置音乐一直播放
	 * @param id
	 */
	public void setLooping(int id)
	{
		sound[id].setLooping(true);
	}
	
	/**
	 * 从头开始播放
	 * @param soundid id号
	 * @param isstart 音乐开关
	 * @param isM  音效开关
	 */
	public void reSet(int soundid, boolean isstart, boolean isM)
	{
		sound[soundid].seekTo(0);
		start(soundid, isstart, isM);
//		sound[soundid].reset();
	}
	
	/**
	 * 释放音乐.
	 */
	public void reseleAll()
	{
		int j;
		for(j = 0; j < sound.length; j++)
		{
			if(sound[j] != null)
			{
				if(sound[j].isPlaying())
				{
					sound[j].stop();
				}
				sound[j] = null;
			}
		}
	}
	/**
	 * 停止播放音乐
	 */
	public void stop(int soundid){
		if(sound[soundid]!=null){
			sound[soundid].pause();
			sound[soundid].seekTo(0);
		}
	}
	/**
	 * 停止所有音乐
	 */
	public void stopAll(){
		int j;
		for(j = 0; j < sound.length; j++)
		{
			if(sound[j] != null)
			{
				if(sound[j].isPlaying())
				{
					sound[j].pause();
					sound[j].seekTo(0);
				}
			}
		}
	}
}
