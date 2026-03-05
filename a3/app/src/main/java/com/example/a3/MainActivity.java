package com.example.a3;

import android.app.AlertDialog;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView imageView;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private final float[] accelerometerValues = new float[3]; // เก็บค่า X, Y, Z
    private boolean isShaking = false;

    private final Handler handler = new Handler();
    private final int shakeThreshold = 15; // ค่าการสั่นที่กำหนด
    private final int pollInterval = 100; // ช่วงเวลาในการตรวจจับ (ms)
    private Thread sensorThread;

    private int[] images = {R.drawable.zz1, R.drawable.zz2, R.drawable.zz3};
    private String[] predictions;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.image_view);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mediaPlayer = MediaPlayer.create(this, R.raw.shaking); // เสียงเขย่า
        predictions = getResources().getStringArray(R.array.predictions); // ดึงคำทำนาย
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        //handler.post(pollShakeTask); // เริ่มกระบวนการตรวจจับการสั่น
        // เริ่ม Thread สำหรับตรวจสอบค่าจากเซ็นเซอร์
        sensorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                checkShake();
                try {
                    Thread.sleep(100); // หน่วงเวลา 100ms ในแต่ละรอบการตรวจสอบ
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        sensorThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (sensorThread != null && sensorThread.isAlive()) {
            sensorThread.interrupt();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerValues, 0, event.values.length); // เก็บค่าจากเซ็นเซอร์
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ไม่ต้องใช้งาน
    }

    private void checkShake() {
        float x = accelerometerValues[0];
        float y = accelerometerValues[1];
        float z = accelerometerValues[2];

        double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        if (acceleration > shakeThreshold && !isShaking) {
            isShaking = true;
            runOnUiThread(() -> {
                playShakeSound();
                startShakeAnimation();
            });
        }
    }


    private void playShakeSound() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    private void startShakeAnimation() {
        int totalSwitches = images.length * 2;
        long delay = 300;

        for (int i = 0; i < totalSwitches; i++) {
            int finalI = i;
            handler.postDelayed(() -> {
                imageView.setImageResource(images[finalI % images.length]);
                if (finalI == totalSwitches - 1) {
                    showResultDialog();
                }
            }, i * delay);
        }
    }

    private void showResultDialog() {
        Random random = new Random();
        String prediction = predictions[random.nextInt(predictions.length)];

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.result_title)
                .setMessage(prediction)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    isShaking = false; // เปิดการตรวจจับการสั่นอีกครั้ง
                    imageView.setImageResource(R.drawable.zz1); // รีเซ็ตรูปภาพ
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        TextView messageTextView = dialog.findViewById(android.R.id.message);
        if (messageTextView != null) {
            messageTextView.setGravity(Gravity.LEFT);
            messageTextView.setTextSize(16);
        }

    }
}