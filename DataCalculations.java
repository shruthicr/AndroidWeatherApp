package com.example.shruthi.bluetoothsensor;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by shruthi on 06-12-2015.
 */
public class DataCalculations {
    public static double Temperature(BluetoothGattCharacteristic c) {
        int rawT = shortSignedAtOffset(c, 0);
        return ((double) rawT / 65536) * 165 - 40;
    }
    public static double Humidity(BluetoothGattCharacteristic c) {
        int a = shortUnsignedAtOffset(c, 2);
        return   ((double)a/ 65536)*100;
    }

    private static Integer shortSignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, offset + 1);

        return (upperByte << 8) + lowerByte;
    }

    private static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1);

        return (upperByte << 8) + lowerByte;
    }
}

