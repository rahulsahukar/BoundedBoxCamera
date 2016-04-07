package customcamera.rahul.com.customcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    boolean previewing = false;
    private MySurfaceView mySurfaceView;
    private SurfaceHolder sh;
    private Camera c;
    private Button click;
    private Button stop;
    private RelativeLayout boundary;
    private RelativeLayout rl;
    private FrameLayout frameLayout;
    int[] boundaryLoc;
    int boundaryWidth;
    int boundaryHeight;
    boolean landscape;
    private int previewHeight;
    private int previewWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boundaryLoc = new int[2];
        getWindow().setFormat(PixelFormat.UNKNOWN);
        frameLayout = (FrameLayout) findViewById(R.id.rootframe);
        click = (Button) findViewById(R.id.click);
        stop = (Button) findViewById(R.id.stop);
        boundary = (RelativeLayout) findViewById(R.id.boundary);
        rl = (RelativeLayout) findViewById(R.id.rel);

        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                c.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        c.stopPreview();

                        byte[] croppedImg = getCroppedPic(data);

                        Intent intent = new Intent(getApplicationContext(), Preview.class);
                        intent.putExtra("PHOTO", croppedImg);
                        startActivity(intent);

                        overridePendingTransition(0, 0);
                    }
                });
            }
        });

        boundary.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        DisplayMetrics dm = new DisplayMetrics();
                        MainActivity.this.getWindowManager().getDefaultDisplay().getMetrics(dm);
                        int topOffset = dm.heightPixels - frameLayout.getMeasuredHeight();

                        boundary.getLocationInWindow(boundaryLoc);
                        boundaryLoc[1] -= topOffset;

                        previewHeight = frameLayout.getMeasuredHeight();
                        previewWidth = frameLayout.getMeasuredWidth();

                        boundaryWidth = boundary.getWidth();
                        boundaryHeight = boundary.getHeight();

                        boundary.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        mySurfaceView = new MySurfaceView(getApplicationContext(), boundaryLoc[0], boundaryLoc[1], boundaryWidth, boundaryHeight);

                        ScrollView.LayoutParams lp = new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        mySurfaceView.setLayoutParams(lp);
                        frameLayout.addView(mySurfaceView, 1);
                        rl.bringToFront();

                        sh = mySurfaceView.getHolder();
                        sh.addCallback(MainActivity.this);
                    }
                });
    }

    private byte[] getCroppedPic(byte[] data) {
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        Log.i("asdad", "" + bmp.getHeight() + " " + bmp.getWidth());
        Bitmap croppedImg = Bitmap.createBitmap(bmp, boundaryLoc[0], boundaryLoc[1], boundaryWidth, boundaryHeight);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        croppedImg.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        c = Camera.open();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        Camera.Parameters parameters = c.getParameters();
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            parameters.set("orientation", "portrait");
            c.setDisplayOrientation(90);
            parameters.setRotation(90);
            landscape = true;
            int temp = boundaryHeight;
            boundaryHeight = boundaryWidth;
            boundaryWidth = temp;

            temp = previewHeight;
            previewHeight = previewWidth;
            previewWidth = temp;

        } else {
            parameters.set("orientation", "landscape");
            c.setDisplayOrientation(0);
            parameters.setRotation(0);
            landscape = false;
        }


        if (previewing) {
            c.stopPreview();
            previewing = false;
        }

        if (c != null) {
            try {
                c.setPreviewDisplay(sh);
                c.startPreview();
                previewing = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        c.stopPreview();
        c.release();
        c = null;
    }
}
