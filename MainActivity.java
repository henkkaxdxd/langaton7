package bluetooth.db;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import android.media.MediaRecorder;
import android.os.Handler;
import android.widget.TextView;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice device;

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private String address = "B8:27:EB:9C:C6:DA";

    TextView mStatusView;
    TextView mStatusView2;
    MediaRecorder mRecorder;
    Thread runner;
    private static double mEMA = 0.0;
    static final private double EMA_FILTER = 0.6;

    final Runnable updater = new Runnable() {

        public void run() {
            updateTv();
        }
    };
    final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TAG", "onCreate");
        setContentView(R.layout.activity_main);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mStatusView = (TextView) findViewById(R.id.status);
        mStatusView2 = (TextView) findViewById(R.id.status2);

        if (runner == null) {
            runner = new Thread() {
                public void run() {
                    while (runner != null) {
                        try {
                            Thread.sleep(1000);
                            Log.i("Noise", "Tock");
                        } catch (InterruptedException e) {
                        }
                        ;
                        mHandler.post(updater);
                    }
                }
            };
            runner.start();
            Log.d("Noise", "start runner()");
        }
    }

    public void stop(View v){
        Log.d("TAG", "Stop");
        stopRecorder();
        mConnectedThread.cancel();
    }

    public void connectbtn(View view) {
        Log.d("TAG", "connect btn");

        device = mBluetoothAdapter.getRemoteDevice(address);
        if (device != null) {
            connect(device);
        }
    }

    public void connect(BluetoothDevice device) {
        Log.d("TAG", "connect");
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }


    public void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d("TAG", "connected" +device.getName());

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }

    class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            try {
                Method method;
                method = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                tmp = (BluetoothSocket) method.invoke(device, 1);

            } catch (Exception e) {
                Log.d("TAG", "ConnectThread Execption");
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.d("TAG", "connect thread run");
            try {
                mmSocket.connect();
            } catch (IOException e) {
                Log.d("TAG", "mmSocket not connected");
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.d("TAG", "ConnectThread Execption2");
                }
            }
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                Log.d("TAG", "mConnectThread mmSocket Close");
                mmSocket.close();
            } catch (IOException e) {
                Log.d("TAG", "close() of connect socket failed");
            }
        }
    }

    class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;


        public ConnectedThread(BluetoothSocket socket) {
            Log.d("TAG", "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                Log.d("TAG", "Get the BluetoothSocket input and output streams");
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("TAG", "temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d("TAG", "BEGIN mConnectedThread");
            // Keep listening to the InputStream while connected
            while (true) {
                Log.d("TAG" , "output");
                try {
                    Thread.sleep(1000);
                    Log.d("TAG", "write test");
                    String arvo = String.valueOf(mStatusView.getText());
                    mmOutStream.write(arvo.getBytes());

                } catch (IOException e) {
                    Log.d("TAG", "disconnected");
                    e.printStackTrace();
                    cancel();
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(String s) {
            while (true) {
                Log.d("TAG" , "output");
                try {
                    Log.d("TAG", "write test");
                    mmOutStream.write(s.getBytes());

                } catch (IOException e) {
                    Log.d("TAG", "disconnected");
                    e.printStackTrace();
                    cancel();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                Log.d("TAG" , "mmSocket.close");
                mmSocket.close();
            } catch (IOException e) {
                Log.d("TAG", "close() of connect socket failed");
            }
        }
    }

    /*
     *
     * Tästä alkaa dB mittari
     *
     */

    public void onResume() {
        super.onResume();
        startRecorder();
    }

    public void onPause() {
        super.onPause();
        stopRecorder();
    }

    public void startRecorder() {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            try {
                mRecorder.prepare();
            } catch (java.io.IOException ioe) {
                android.util.Log.e("[Monkey]", "IOException: " +
                        android.util.Log.getStackTraceString(ioe));

            } catch (java.lang.SecurityException e) {
                android.util.Log.e("[Monkey]", "SecurityException: " +
                        android.util.Log.getStackTraceString(e));
            }
            try {
                mRecorder.start();
            } catch (java.lang.SecurityException e) {
                android.util.Log.e("[Monkey]", "SecurityException: " +
                        android.util.Log.getStackTraceString(e));
            }
        }

    }

    public void stopRecorder() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public void updateTv() {
        double answer = (soundDb(Math.pow(1,1))) - 35;
        //double answer = getAmplitudeEMA();
        mStatusView.setText(new DecimalFormat("##").format(answer));
    }

    public double soundDb(double ampl){
        return  20 * Math.log10(getAmplitudeEMA() / ampl);
    }
    public double getAmplitude() {
        if (mRecorder != null)
            return  (mRecorder.getMaxAmplitude());
        else
            return 0;

    }

    public double getAmplitudeEMA() {
        double amp =  getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }
}