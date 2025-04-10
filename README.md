# Brief Documentation

## APK soon

## Overview
This Android application communicates with a Microcontroller via OTG USB connection. It receives data, stores it in an SQLite database, and visualizes the data using scatter plots.

## Features
- **USB Serial Communication**: Receives data from the STM32 microcontroller over OTG USB.
- **Data Parsing & Storage**: Parses received data and stores it in a SQLite database.
- **Real-time Graph Updates**: Displays and dynamically updates scatter plots.
- **Data Management**: Saves and retrieves historical data with options to clear data.

## Setup & Installation

### 0. Sample Arduino Code
Arduino code used for testing purpose can be found [here](https://github.com/Arsfake26/Arduino-test-code-for-portable-data-acquisition)

### 1. Dependencies
Added dependencies to `build.gradle`:
```gradle
implementation 'com.github.mik3y:usb-serial-for-android:3.4.0'
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

### 2. Permissions
Add the following permissions in `AndroidManifest.xml`:
```xml
<uses-feature android:name="android.hardware.usb.host" />
<uses-permission android:name="android.permission.USB_PERMISSION" />
```

### 3. USB Permission Handling
A `BroadcastReceiver` is implemented to handle USB permission requests and device attachment.

### 4. Connecting to Microcontroller via USB
```java
private void openDevice(UsbDevice device) {
    UsbDeviceConnection connection = usbManager.openDevice(device);
    UsbSerialPort port = UsbSerialProber.getDefaultProber().probeDevice(device).getPorts().get(0);

    try {
        port.open(connection);
        port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        
        byte[] buffer = new byte[64];
        int numBytesRead = port.read(buffer, 1000);
        Log.d("USB", "Read " + numBytesRead + " bytes.");
    } catch (IOException e) {
        Log.e("USB", "Error opening device", e);
    } finally {
        try {
            port.close();
        } catch (IOException e) {
            // Ignore
        }
    }
}
```

## Data Handling
### 1. Parsing Data Format
The sent data in the format:
```
{#msgCounter;data1;data2;data3;data4;data5;data6;data7;data8}
```
The App extracts values by removing `#` and splitting by `;`.

### 2. Storing Data in SQLite
A helper class (`HelperDatabase`) manages database operations:
```java
SQLiteDatabase db = this.getWritableDatabase();
ContentValues values = new ContentValues();
values.put("msgCounter", msgCounter);
values.put("data1", data1);
// Add remaining data fields
long newRowId = db.insert("data_table", null, values);
```

## Scatter Plot Visualization
### 1. Displaying Data in MPAndroidChart
Scatter plots are updated dynamically when new data arrives:
```java
ScatterDataSet scatterDataSet = new ScatterDataSet(entries, "Data1");
scatterDataSet.setColor(Color.RED);
scatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
ScatterData scatterData = new ScatterData(scatterDataSet);
scatterChart.setData(scatterData);
scatterChart.invalidate();
```

## Real-Time Updates
A `Handler` listens for new data and updates the graph dynamically.

## Recognized Microcontroller Devices
List of Microcontroller devices that can be used with the app available on `device_filter.xml`, add the Vendor ID and Product ID of the device to enable communication with the app.
