package com.example.rekapdata;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ContentValues;
import android.provider.MediaStore;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView tvStatus;
    private Button btnConnect;
    private EditText etInput;
    private Button btnSend;
    private Button btnExport;
    private Button btnViewData;
    private Button btnViewGraph;
    private Button btnClearData;

    private UsbManager usbManager;
    private UsbSerialPort serialPort;
    private SerialInputOutputManager ioManager;
    private HelperDatabase databaseHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final int BUFFER_SIZE = 4096;

    private StringBuilder readBuffer = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStatus = findViewById(R.id.tvStatus);
        btnConnect = findViewById(R.id.btnConnect);
        etInput = findViewById(R.id.etInput);
        btnExport = findViewById(R.id.btnExport);
        btnViewData = findViewById(R.id.btnViewData);
        btnViewGraph = findViewById(R.id.btnViewGraph);
        btnClearData = findViewById(R.id.btnClearData);

        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        databaseHelper = new HelperDatabase(this);

        btnConnect.setOnClickListener(v -> connectToUsb());
        btnExport.setOnClickListener(v -> {
            showFileNameDialog();
        });

        btnViewData.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewDataActivity.class);
            startActivity(intent);
        });

        btnViewGraph.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LineGraphActivity.class);
            startActivity(intent);
        });

        btnClearData.setOnClickListener(v -> {
            boolean isCleared = databaseHelper.clearAllData();
            boolean idClear = databaseHelper.clearAllDataAndResetID();
            if (isCleared && idClear) {
                Toast.makeText(MainActivity.this, "All data cleared successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Failed to clear data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void connectToUsb() {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            tvStatus.setText("Status: No Device Found");
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();
        serialPort = driver.getPorts().get(0);

        if (!usbManager.hasPermission(device)) {
            tvStatus.setText("Status: No Permission");
            return;
        }

        try {
            serialPort.open(usbManager.openDevice(device));
            serialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            ioManager = new SerialInputOutputManager(serialPort, mListener);
            executor.submit(ioManager);
            tvStatus.setText("Status: Connected");
        } catch (IOException e) {
            tvStatus.setText("Status: Error Opening Device");
            try {
                serialPort.close();
            } catch (IOException e2) {
                // Ignore.
            }
            serialPort = null;
        }
    }

    private boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }




    private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {
        @Override
        public void onNewData(byte[] data) {
            runOnUiThread(() -> {
                if (data != null && data.length > 0) {
                    String receivedMessage = new String(data);
                    readBuffer.append(receivedMessage); // Append new data to the buffer

                    // Process buffer to extract complete messages
                    int startIdx;
                    int endIdx;
                    while ((startIdx = readBuffer.indexOf("{")) != -1 && (endIdx = readBuffer.indexOf("}", startIdx)) != -1) {
                        // Extract complete message and remove it from the buffer
                        String completeMessage = readBuffer.substring(startIdx, endIdx + 1);
                        readBuffer.delete(0, endIdx + 1);

                        Log.d(TAG, "Complete message received: " + completeMessage);
                        parseReceivedData(completeMessage);
                    }

                    // Log any partial data that cannot be processed yet
                    if (readBuffer.length() > 0 && startIdx == -1) {
                        Log.d(TAG, "Partial data waiting: " + readBuffer.toString());
                    }
                } else {
                    Log.e(TAG, "No data received or data is empty");
                }
            });
        }

        @Override
        public void onRunError(Exception e) {
            Log.e(TAG, "Runner stopped", e);
        }
    };


    private void parseReceivedData(String data) {
        // Trim any leading or trailing whitespace or newline characters
        data = data.trim();

        // Log the received data for debugging
        Log.d("ViewDataActivity", "Raw data received: " + data);

        // Check if the input starts and ends with '#'
        if (data.startsWith("{") && data.endsWith("}")) {
            // Remove the '{}'
            String trimmedData = data.substring(1, data.length() - 1);

            // Split the data using ";" as the delimiter
            String[] parts = trimmedData.split(";");
            Log.d("ViewDataActivity", "Parsed parts count: " + parts.length);
            // Validate the data length (should be 9 parts for your format: msgCounter to data9)
            if (parts.length == 9) {
                String msgCounter = parts[0].startsWith("#") ? parts[0].substring(1) : parts[0];
                String data1 = parts[1];
                String data2 = parts[2];
                String data3 = parts[3];
                String data4 = parts[4];
                String data5 = parts[5];
                String data6 = parts[6];
                String data7 = parts[7];
                String data8 = parts[8];

                // Insert the data into the database
                boolean isInserted = databaseHelper.insertData(msgCounter, data1, data2, data3, data4, data5, data6, data7, data8);
                if (!isInserted) {
                    Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Invalid data format", Toast.LENGTH_SHORT).show();
                Log.e("ViewDataActivity", "Invalid data format. Parts: " + parts.length + ", Data: " + data);
            }
        } else {
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            Log.e("ViewDataActivity", "Invalid input: " + data);
        }
    }




    private String pendingFileName = null;

    private void requestStoragePermissions(String fileName) {
        pendingFileName = fileName; // Store the file name temporarily
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            exportDataToCSV(fileName);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingFileName != null) {
                    exportDataToCSV(pendingFileName);
                    pendingFileName = null;
                }
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void showFileNameDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter file name (without extension)");

        new android.app.AlertDialog.Builder(this)
                .setTitle("Export CSV")
                .setMessage("Enter the name of the file you want to export:")
                .setView(input)
                .setPositiveButton("Export", (dialog, whichButton) -> {
                    String fileName = input.getText().toString().trim();
                    if (!fileName.isEmpty()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            exportDataToCSV(fileName);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "File name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, whichButton) -> dialog.cancel())
                .show();
    }


    private void exportDataToCSV(String fileName) {
        if (!fileName.endsWith(".csv")) {
            fileName += ".csv";
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/MyAppExports/");

        try (OutputStream outputStream = getContentResolver().openOutputStream(
                Objects.requireNonNull(getContentResolver().insert(MediaStore.Files.getContentUri("external"), values)))) {
            if (outputStream != null) {
                outputStream.write("ID,Timestamp,MessageCounter,ReceivedData,tes1,tes2,tes3,tes4,tes5,tes6\n".getBytes());
                Cursor cursor = databaseHelper.getAllData();
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String id = cursor.getString(0);
                        String msgCounter = cursor.getString(1);
                        String data1 = cursor.getString(2);
                        String data2 = cursor.getString(3);
                        String data3 = cursor.getString(4);
                        String data4 = cursor.getString(5);
                        String data5 = cursor.getString(6);
                        String data6 = cursor.getString(7);
                        String data7 = cursor.getString(8);
                        String data8 = cursor.getString(9);
                        String line = id + "," + msgCounter + "," + data1 + "," + data2 + "," + data3 + "," + data4 + "," +
                                data5 + "," + data6 + "," + data7 + "," + data8 + "\n";
                        outputStream.write(line.getBytes());
                    } while (cursor.moveToNext());
                    cursor.close();
                }
                Toast.makeText(this, "Data exported successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to export data", Toast.LENGTH_SHORT).show();
        }
    }

}

