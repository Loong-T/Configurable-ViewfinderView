package in.nerd_is.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;

import java.util.List;

import in.nerd_is.configurableviewfinder.ViewfinderView;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSION_CAMERA = 0;

    private BarcodeView barcodeView;
    private ViewfinderView viewfinderView;
    private TextView textView;
    private Button scanBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barcodeView = (BarcodeView) findViewById(R.id.barcode_view);
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        textView = (TextView) findViewById(R.id.text);
        scanBtn = (Button) findViewById(R.id.button);

        viewfinderView.setCameraPreview(barcodeView);

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                decode();
            }
        });

        final String cameraPermission = Manifest.permission.CAMERA;
        if (ContextCompat.checkSelfPermission(this, cameraPermission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{cameraPermission},
                    REQ_PERMISSION_CAMERA);
        } else {
            decode();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQ_PERMISSION_CAMERA:
                decode();
                break;
            default:
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    private void decode() {
        scanBtn.setEnabled(false);
        viewfinderView.drawViewfinder();
        textView.setText("Scanning...");
        barcodeView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                textView.setText("Scan result: " + result.getText());
                viewfinderView.drawResultBitmap(result.getBitmap());
                scanBtn.setEnabled(true);
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                viewfinderView.swapPossibleResultPoints(resultPoints);
            }
        });
    }

    @Override
    protected void onPause() {
        barcodeView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }
}
