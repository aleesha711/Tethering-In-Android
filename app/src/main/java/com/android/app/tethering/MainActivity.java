package com.android.app.tethering;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    List<Client> result = new ArrayList<>();
    public static final String TAG =  MainActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getTetheringClientsList();
        String json = new Gson().toJson(result);
        Log.e("isTetheringEnabled",String.valueOf(isTetheringEnabled()));
        Log.e("result",json);
        result.clear();
        for (int i = 0; i < result.size(); i++) {
            Log.e("tetheringDeviceList", "ip : " + result.get(i).ipAddr + " " + "mac : " + result.get(i).hwAddr);
        }
    }

    private boolean isTetheringEnabled() {

        boolean isWifiAPenabled = false;

        final WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        Method[] wmMethods = wifi.getClass().getDeclaredMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("isWifiApEnabled")) {
                try {
                    isWifiAPenabled = (boolean) method.invoke(wifi);
                    Log.e("Tethering", String.valueOf(isWifiAPenabled));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }

        return isWifiAPenabled;
    }

    public List<Client> getTetheringClientsList() {
        if (!isTetheringEnabled()) {
            return null;
        }


        // Basic sanity checks
        Pattern macPattern = Pattern.compile("..:..:..:..:..:..");

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" +");
                if (parts.length < 6) {
                    continue;
                }

                String ipAddr = parts[0];
                String hwAddr = parts[3];
                String device = parts[5];

                String mac = parts[3];

                System.out.println("ip:" + ipAddr + " mac : " + hwAddr);


                if (!macPattern.matcher(parts[3]).find()) {
                    continue;
                }

                if (mac.matches("..:..:..:..:..:..")) {

                    boolean isReachable = InetAddress.getByName(parts[0]).isReachable(300);

                    if (InetAddress.getByName(parts[0]).isReachable(300)) {

                        System.out.println("Mac = " + parts[3] + " IP = " + parts[0] + " is Reachable = " + isReachable);
                        result.add(new Client(ipAddr, hwAddr));

                        Set<Client> withoutDuplicates = new LinkedHashSet<Client>(result);
                        result.clear(); // copying elements but without any duplicates primes.addAll(withoutDuplicates);

                        result.addAll(withoutDuplicates);
                    }
                    else {
                        Log.e("ip =" + parts[0],"disconnected");
                    }

                }

              /*  result.add(new Client(ipAddr, hwAddr));*/
            }
        } catch (IOException e) {
            Log.e(TAG, "", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }

        return result;
    }
}
