package net.nkzn.android.gohome;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class Main extends Activity implements SensorEventListener, LocationListener {
	
	private static final int REQUEST_CODE_HOME = 1;
	
	private boolean mRegisteredSensor;
	private SensorManager mSensorManager;
    
	private LocationManager mLocationManager;
    public static final double METER_PER_LATITUDE = 111133.3;
	
	private CompassView mView;
    private float[] mValues;
    private float home_bearing;
    private float my_bearing;
    private float distance;
    
    private Location myLocation;
    private Location homeLocation;
    
    private Build build;
    
    private int viewWidth;
    private int viewHeight;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		build = new Build();
		
        mView = new CompassView(this);
        setContentView(mView);
        
		
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
		SharedPreferences goHomeSettings = getSharedPreferences("GO_HOME", MODE_PRIVATE);
		
		int latitude = 0;
		int longuitude = 0;
		
		if((latitude = goHomeSettings.getInt("latitude", 0)) == 0 |
			(longuitude = goHomeSettings.getInt("longuitude", 0)) == 0	){
			Intent intent = new Intent(Main.this, HomeSetActivity.class);
			startActivityForResult(intent, REQUEST_CODE_HOME);
		} else {
			double latitude_f = (double)latitude / 1000000;
			double longuitude_f = (double)longuitude / 1000000;
			homeLocation = new Location("home");
			homeLocation.setLatitude(latitude_f);
			homeLocation.setLongitude(longuitude_f);
			Log.i("gohome", "Your Home: "+homeLocation.toString()+" /onCreate");
			Toast.makeText(this, "本体を地面と水平にしてご利用下さい", Toast.LENGTH_LONG).show();
		}
		
	}
	
    @Override
    protected void onResume()
    {
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        
        if (sensors.size() > 0) {
            Sensor sensor = sensors.get(0);
            mRegisteredSensor = mSensorManager.registerListener(this,
                sensor,
                SensorManager.SENSOR_DELAY_FASTEST);
        }
        
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        super.onResume();
    }
    
	@Override
	protected void onPause() {
        if (mRegisteredSensor) {
            mSensorManager.unregisterListener(this);
            mRegisteredSensor = false;
        }
        
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
        
		super.onPause();
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		viewWidth = mView.getWidth();
		viewHeight = mView.getHeight();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_CODE_HOME){
			Bundle extras = data.getExtras();
			homeLocation = new Location("home");
			if(resultCode == RESULT_OK){
				double latitude = (double)extras.getInt("HOME_LATITUDE");
				double longuitude = (double)extras.getInt("HOME_LONGUITUDE");
								
				homeLocation.setLatitude(latitude/1000000);
				homeLocation.setLongitude(longuitude/1000000);
				
				//Log.i("gohome", "Your Home: "+homeLocation.toString()+" /onActivityResult");
			}else if(resultCode == RESULT_CANCELED){
				double latitude = (double)extras.getInt("HERE_LATITUDE");
				double longuitude = (double)extras.getInt("HERE_LONGUITUDE");
								
				homeLocation.setLatitude(latitude/1000000);
				homeLocation.setLongitude(longuitude/1000000);
				
				//Log.i("gohome", "Your Home: "+homeLocation.toString()+" /onActivityResult");
			}
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		}
		
		Toast.makeText(this, "目的地の再設定は、メニューボタンから行えます", Toast.LENGTH_LONG).show();
		Toast.makeText(this, "本体を地面と水平にしてご利用下さい", Toast.LENGTH_LONG).show();
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 10, 0, "自宅位置を再設定").setIcon(android.R.drawable.ic_menu_preferences);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()){
		case 10:
			Intent intent = new Intent(Main.this, HomeSetActivity.class);
			startActivityForResult(intent, REQUEST_CODE_HOME);
			break;
		}		
		return super.onMenuItemSelected(featureId, item);
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		 mValues = event.values;
         if (mView != null) {
             mView.invalidate();
         }		
	}

	@Override
	public void onLocationChanged(Location location) {
		myLocation = location;
		if(homeLocation != null){
			home_bearing = myLocation.bearingTo(homeLocation);
			distance = myLocation.distanceTo(homeLocation);			
		}
		//Log.d("gohome","north:"+my_bearing);
		//Log.d("gohome", "bearing:"+home_bearing);
	}

	@Override
	public void onProviderDisabled(String location) {
		//Log.d("gohome", "onProviderDisabled");
	}

	@Override
	public void onProviderEnabled(String provider) {
		//Log.d("gohome", "onProviderEnabled");
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		//Log.d("gohome", "onStatusChanged:"+status);		
	}
	
    private class CompassView extends View {
        private Paint   mPaint = new Paint();
        private Path    mPath = new Path();
        private Path 	mPath2 = new Path();
        private Paint textPaint = new Paint();
        private Paint btnPaint = new Paint();
        private boolean mAnimate;
        private long    mNextTime;
        private boolean isBgWhite = true;
        private Rect btnRect = new Rect();

        public CompassView(Context context) {
        	super(context);
            // Construct a wedge-shaped path
            mPath.moveTo(0, -50);
            mPath.lineTo(-20, 60);
            mPath.lineTo(0, 50);
            mPath.lineTo(20, 60);
            mPath.close();
            
            mPath2.moveTo(0, -20);
            mPath2.lineTo(-8, 24);
            mPath2.lineTo(0, 20);
            mPath2.lineTo(8, 24);
            mPath2.close();
            
    		SharedPreferences goHomeSettings = getSharedPreferences("GO_HOME", MODE_PRIVATE);
    		isBgWhite = goHomeSettings.getBoolean("isBgWhite", false) ? true : false;
            
            setFocusable(true);
        }
        
            
        @Override 
        protected void onDraw(Canvas canvas) {
        	
            int w = viewWidth;
            int h = viewHeight;            
            int cx = w / 2;
            int cy = h / 2;
        	
        	// 矢印用Paint
            Paint paint = mPaint;
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);

            // テキスト用Paint
            Paint tpaint = textPaint;
    		tpaint.setStyle(Paint.Style.FILL);
    		tpaint.setTextSize(20);
    		tpaint.setAntiAlias(true);
    		
    		Paint bPaint = btnPaint;
    		bPaint.setStyle(Paint.Style.FILL);
    		bPaint.setAntiAlias(true);
    		
    		btnRect.set(w-80, h-80, w-20, h-20);
    		Point p = new Point(w-50,h-50);
    		
    		// 背景色変更
    		if(isBgWhite){
                canvas.drawColor(Color.WHITE);
                paint.setColor(Color.argb(255, 0, 0, 153));
        		tpaint.setARGB(255, 0, 0, 0);
        		
        		bPaint.setColor(Color.BLACK);
        		//canvas.drawRect(btnRect, bPaint);
        		canvas.drawCircle(p.x, p.y, 40, bPaint);
        		bPaint.setColor(Color.argb(255, 0, 102, 255));
    		}else{
                canvas.drawColor(Color.BLACK);
                paint.setColor(Color.argb(255, 0, 102, 255));
        		tpaint.setARGB(255, 255, 255, 255);
        		
        		bPaint.setColor(Color.WHITE);
        		//canvas.drawRect(btnRect, bPaint);
        		canvas.drawCircle(p.x, p.y, 40, bPaint);
        		bPaint.setColor(Color.argb(255, 0, 0, 153));
    		}
    		    		
    		canvas.drawText("前", cx, 40, tpaint);
    		canvas.drawText("後", cx, h-20, tpaint);
    		canvas.drawText("左", 20, cy, tpaint);
    		canvas.drawText("右", w-20, cy, tpaint);

    		canvas.drawText("家まで："+(int)distance+"m", 20, 20, tpaint);
    		
    		canvas.translate(p.x, p.y);
    		canvas.drawPath(mPath2, bPaint);
    		canvas.translate(-p.x, -p.y);
    		
            canvas.translate(cx, cy);
            if (mValues != null) {
            	
            	if(build.MODEL.equals("IS01")){
            		float f = mValues[0]+90;
            		my_bearing = f >= 360 ? f - 360 : f;
            		canvas.rotate(home_bearing-my_bearing);
            	}else{
                    canvas.rotate(home_bearing-mValues[0]);            		
            	}
            	
            }
            canvas.drawPath(mPath, mPaint);
        }
        
        @Override
        public boolean onTouchEvent(MotionEvent event) {
        	if(event.getX() >= btnRect.left && event.getX() <= btnRect.right &&
        	   event.getY() >= btnRect.top && event.getY() <= btnRect.bottom){
        		isBgWhite = !isBgWhite;
        		SharedPreferences goHomeSettings = getSharedPreferences("GO_HOME", MODE_PRIVATE);
				SharedPreferences.Editor editor = goHomeSettings.edit();
				editor.putBoolean("isBgWhite", isBgWhite);
				editor.commit();
        	}
        	
        	return super.onTouchEvent(event);
        }
    
        @Override
        protected void onAttachedToWindow() {
            mAnimate = true;
            super.onAttachedToWindow();
        }
        
        @Override
        protected void onDetachedFromWindow() {
            mAnimate = false;
            super.onDetachedFromWindow();
        }
        
    }

}
