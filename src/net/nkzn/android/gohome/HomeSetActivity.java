package net.nkzn.android.gohome;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class HomeSetActivity extends MapActivity implements LocationListener {
	
	private static MapView mapView;
	private MapController mapController;
	private MyLocationOverlay myLocationOverlay;
	private static LocationOverlay lo;
	private Drawable drawable;
	private static final int DEFAULT_LATITUDE = 35681382;
	private static final int DEFAULT_LONGUITUDE = 139766084;
	private LocationManager l;
	private boolean firstflag = true;
	protected Handler handler = new Handler();
	protected static ProgressDialog dialog;
	private Button setBtn;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homeset);
        
        mapView = (MapView)findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapController = mapView.getController();
		
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		mapView.getOverlays().add(myLocationOverlay);
		myLocationOverlay.enableMyLocation();
		myLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				mapController.animateTo(myLocationOverlay.getMyLocation());
			}
		});
		
		// マップ上のオブジェクト用画像の初期化
		drawable = getResources().getDrawable(R.drawable.home);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        // オーバーレイの初期化
        lo = new LocationOverlay(this, drawable);
        // 家の位置の初期化
        lo.homePoint = new GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGUITUDE);
        
        mapView.getOverlays().add(lo);
        mapView.getController().setCenter(lo.homePoint);
        
        addHomeDefault();
        
        setBtn = (Button)findViewById(R.id.setbutton);
        setBtn.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View v) {
				int home_latitude = lo.homePoint.getLatitudeE6();
				int home_longuitude = lo.homePoint.getLongitudeE6();
				Intent intent = new Intent();
				intent.putExtra("HOME_LATITUDE", home_latitude);
				intent.putExtra("HOME_LONGUITUDE", home_longuitude);
				
				saveGeoPoint(home_latitude,home_longuitude);
				
				setResult(RESULT_OK, intent);
				finish();
			}

			private void saveGeoPoint(int home_latitude, int home_longuitude) {
				SharedPreferences goHomeSettings = getSharedPreferences("GO_HOME", MODE_PRIVATE);
				
				SharedPreferences.Editor editor = goHomeSettings.edit();
				editor.putInt("latitude", home_latitude);
				editor.putInt("longuitude", home_longuitude);
				
				editor.commit();
			}
		});

        /*
         * GPSで位置情報を取得し始める
         */
        l = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        l.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
      //「現在地を検索中...」ダイアログを表示
        showProgressDialog();
    }
    
	@Override
    protected void onResume() {
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableCompass();
        mapView.invalidate();
        
        super.onResume();
    }
    
    @Override
    protected void onPause() {
    	myLocationOverlay.disableCompass();
        myLocationOverlay.disableMyLocation();
        super.onPause();
    }
    
    /**
     * 地図上の家オブジェクトを初期化
     */
    private void addHomeDefault(){
		GeoPoint gp = mapView.getMapCenter();		
		mapView.getController().setZoom(17);
		OverlayItem item = new OverlayItem(gp,"","");
		lo.addOverlay(item);
			
		mapView.invalidate();
    }
   

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private void showProgressDialog() {
		handler.post(new Runnable(){
			public void run(){
				dialog = ProgressDialog.show(HomeSetActivity.this, 
							"", 
							"現在地を検索中...", 
							true,
							true);
				
			}
		});
	}
	
	/**
	 * 位置計測中のダイアログを消す
	 */
	private void hideProgressDialog() {
		handler.post(new Runnable(){
			public void run(){
				if(dialog != null){
					dialog.dismiss();
				}
			}
		});	
	}
	
	@Override
	public void onLocationChanged(Location location) {
		if(firstflag){			
			Drawable drawable = getResources().getDrawable(R.drawable.home);
	        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
	        
	        
	        //lo = new LocationOverlay(this, drawable);
	        lo.clear();
	        
	        lo.homePoint = new GeoPoint((int)(location.getLatitude()*1E6),(int)(location.getLongitude()*1E6));
	        
	        mapView.getOverlays().add(lo);
	        mapView.getController().setCenter(lo.homePoint);
	        
	        addHomeDefault();
	        hideProgressDialog();
	        
			firstflag = false;
			l.removeUpdates(this);
			
			Toast.makeText(this, "ご自宅の位置まで家のアイコンを移動させ、セットボタンを押してください。", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if(event.getAction() == KeyEvent.ACTION_DOWN){
			switch(event.getKeyCode()){
			case KeyEvent.KEYCODE_BACK:

				GeoPoint gp = myLocationOverlay.getMyLocation();
				
				int here_latitude = gp.getLatitudeE6();
				int here_longuitude = gp.getLongitudeE6();
				
				Intent intent = new Intent();
				intent.putExtra("HERE_LATITUDE", here_latitude);
				intent.putExtra("HERE_LONGUITUDE", here_longuitude);
				
				setResult(RESULT_CANCELED,intent);
				finish();
			}
		}
		
		return super.dispatchKeyEvent(event);
	}
}