package net.nkzn.android.gohome;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

/**
 * オーバーレイを表示するためのレイヤー<br>
 * 
 * @author YukiyaNakagawa
 *
 */
public class LocationOverlay extends ItemizedOverlay<OverlayItem> {
	/**
	 * ログ出力用タグ
	 */
	//private static final String TAG = "LOCATION_OVERLAY";
	
	/**
	 * 描画するためのPaint
	 */
	private Paint paint;
	
	/**
	 * 移動中の家の画像を保持する
	 */
    private Bitmap bitmap;

    /**
     * オーバーレイ用のオブジェクトを格納するが、中心点のオブジェクトが１つ入るのみである
     */
	private List<OverlayItem> items = new ArrayList<OverlayItem>();
	
    /**
     * コンストラクタでA004Activityから渡されたContextの保持
     */
    private Context context;

    /**
     * 家の中心点の座標
     */
    public GeoPoint homePoint;
    
    
	/**
	 * 円の半径に対応する緯度上の距離
	 */
	 int radiusLatitude;
	
	/**
	 * 円の半径
	 */
    private float radius = 0;
    
    /**
     * オーバーレイするオブジェクト
     */
    public static OverlayItem itemHome;
    
    /**
     * メートル表現上の距離
     */
    public float meter = 100;
    
    /**
     * 1度あたりのメートル距離
     */
    public static final double METER_PER_LATITUDE = 111133.3;
    
    /**
     * 家の当たり判定
     */
    private static final int HOME_TOUCH_AREA = 30;
        
    /**
     * 移動モード<br>
     * trueで移動中。
     */
    private boolean homeMovingMode=false;
    
    /**
     * エリア策定モード<br>
     * trueでエリア選択可
     */
    private boolean areaDefineMode = false;
    
    /**
     * コンストラクタ。<br>
     * オブジェクト(家)が静止しているときに表示される画像をhomeMarkerとして親クラスへ渡し、<br>
     * オブジェクトが移動中に表示する画像をcontextから呼び出してbitmapへ格納する。
     * @param context HomeSetActivity.this
     * @param homeMarker 家が静止している時に表示される画像
     */
	public LocationOverlay(Context context, Drawable homeMarker) {
		super(boundCenterBottom(homeMarker));
		
		this.context = context;
		
		Resources res = context.getResources();
		bitmap = BitmapFactory.decodeResource(res, R.drawable.home);
		populate();
		
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow){
		Point itemPoint = mapView.getProjection().toPixels(homePoint, null);		
		if(!shadow){
			//移動モード中だけ家を描画
			if(homeMovingMode){
				canvas.drawBitmap(bitmap, itemPoint.x-bitmap.getWidth()/2, itemPoint.y-bitmap.getHeight(), paint);
			}
		}
		super.draw(canvas, mapView, shadow);
	}
	
	
	/**
	 * スーパークラス・ItemziedOverlayから呼ばれる
	 */
	@Override
	protected OverlayItem createItem(int i) {
		itemHome = items.get(i);
		return items.get(i);
	}

	@Override
	public int size() {
		return items.size();
	}
	
	/**
	 * オーバーレイにオブジェクトを追加する
	 * @param item オブジェクト
	 */
	public void addOverlay(OverlayItem item){
		items.add(item);
		populate();
	}
	
	/**
	 * オーバーレイをクリアする
	 */
	public void clear(){
		items.clear();
		populate();
	}
	
	/**
	 * 家でも円のフチでもない場所をタップすると、家の場所まで画面を移動
	 */
	public boolean onTap(GeoPoint p, MapView mapView){
		mapView.getController().animateTo(this.homePoint);
		return false;
	}

	/**
	 * タッチイベントが起きた際に呼び出される。<br>
	 * trueを返した場合の処理のみが実行され、falseを返した場合はA004ActivityのMapActivityへ操作が移譲される。<br>
	 * 各種フラグによって、円の大きさを策定するためのモードと、家が移動している間のモードを持つ。
	 */
    public boolean onTouchEvent(MotionEvent event, MapView mapView){
    	/*
    	 * タッチイベントの種類
    	 */
    	int action = event.getAction();
    	
    	/*
    	 * 家の緯度上の(地図上の)位置から算出した画面上の位置
    	 */
		Point posHome = mapView.getProjection().toPixels(itemHome.getPoint(), null);
		
		/*
		 * タッチイベントから引き出した指の画面上の位置
		 */
		Point posFinger = new Point((int)event.getX(), (int)event.getY());
		
		/*
		 * ACTION_DOWN, ACTION_UP, ACTION_MOVEそれぞれの処理
		 */
		switch(action){
		
		/*
		 *  タッチされた際のイベント
		 */
		case MotionEvent.ACTION_DOWN:
			/*
			 *  家から30dip以内の距離にある時
			 *  要は家に触ってるとき
			 */
    		if(getDistance(posHome,posFinger)<HOME_TOUCH_AREA){
    			/*
    			 * フラグ。
    			 * 家に触ったので、移動モードと可視化モードをtrue。
    			 */
    			homeMovingMode = true;
        		        		
        		/*
        		 * タッチバイブ。家に触れたことが分かりやすいようにブルえます。
        		 */
        		Vibrator vibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        		vibrator.vibrate(100);
        		
        		/*
        		 * 移動開始に伴なうOverlayItemの消去。
        		 * この後、ACTION_MOVEでただの画像と化した家が出現する
        		 */
    	   		clear();
    		}
    		else{
    			return false;
    		}
    		
    		/*
    		 * 変更を更新する
    		 */
    		populate();
    		mapView.invalidate();
    		return true;
    	
    	
    	/*
    	 * タッチが離れた際のイベント	
    	 */
		case MotionEvent.ACTION_UP:
    		//Log.d("RADIUS","Touch Up");
    		/*
    		 * とりあえず指が離れたときにズームボタン表示
    		 */
    		mapView.displayZoomControls(true);
			//Log.d(TAG,"Area Define Mode is "+areaDefineMode);
			
			/*
			 * 移動モードの終了
			 * 指を話した時の画面上の座標から緯度・経度を算出して、新しいOverlayを生成する
			 * その後、移動モードのフラグをfalseにする
			 */
    		if(homeMovingMode){
        		GeoPoint newGP = mapView.getProjection().fromPixels((int)event.getX(), (int)event.getY());
        		OverlayItem newDroid = new OverlayItem(newGP,"","");
        		addOverlay(newDroid);
        		homePoint = newGP;
        		homeMovingMode = false;
    		}
    		else{
        		/*
        		 * 指を話した場所が家ではなかったとき、
        		 * falseを返してMapActivityにタッチイベントを返す
        		 */
        		return false;
    		}
    		
    		/*
    		 * 変更を更新する
    		 */
    		populate();
    		mapView.invalidate();    		
    		return true;
    	
    	/*
    	 * 移動中のイベント
    	 */
		case MotionEvent.ACTION_MOVE:
    		//Log.d("RADIUS","Move: "+posFinger.x+", "+posFinger.y);
    		
    		/*
    		 * 移動モード
    		 * drawメソッドで画像を表示させる
    		 */
    		if(homeMovingMode){
    			homePoint = mapView.getProjection().fromPixels((int)event.getX(), (int)event.getY());
    		}else{
    			/*
    			 * 家移動モードではないとき、処理をMapActivityへ返す
    			 */
    			return false;
    		}
    		
			/*
			 * 変更を更新する
			 */
    		populate();
    		mapView.invalidate();
    		return true;
    		
		}
    	return false;
    }
    
    /**
     * ２点間の距離を算出
     * @param p1 点１
     * @param p2 点２
     * @return 点１と点２の距離
     */
    private double getDistance(Point p1, Point p2){
    	return Math.sqrt(Math.pow(p2.x-p1.x, 2) + Math.pow(p2.y-p1.y, 2));
    }
    
}
