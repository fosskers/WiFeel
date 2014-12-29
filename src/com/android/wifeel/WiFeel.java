package com.android.wifeel;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.RadioButton;
import java.util.List;

// --- //

public class WiFeel extends Activity
{
    public enum Strength { WEAK, MEDIUM, STRONG };

    private static final int NOTIFICATION_ID = 1;
    private static final int GENERIC_NOTIFICATION = 2;
    private static Strength threshold = Strength.WEAK;
    private static boolean service_on = false;
    private static List<ScanResult> wifiFields = null;
    private static WifiManager wm;
    private static WifiLock wl;

    /* Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        wm = (WifiManager)getSystemService(WIFI_SERVICE);
        wl = wm.createWifiLock("WiFeelLock");
        IntentFilter i = new IntentFilter();
        i.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context c, Intent i) {
                if(service_on) {
                    // Code to execute when the event occurs.
                    List<ScanResult> rs = wm.getScanResults();
                    LinearLayout ll = (LinearLayout)findViewById(R.id.ssids);

                    ll.removeAllViews();

                    for(ScanResult sr : rs) {
                        if(inField(sr)) {
                            TextView t = new TextView(c);
                            t.setText(sr.SSID + " " + sr.level);
                            ll.addView(t);
                        }
                    }

                    if(enteredWifi(rs)) {
                        genericNotification("You are in a Wifi field!");
                    }

                    // Updated previous ScanResults
                    wifiFields = rs;

                    rescan();
                }
            }
        }, i);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    /* Create options menu */
    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        MenuInflater i = getMenuInflater();
        i.inflate(R.menu.actionbar, m);
        return super.onCreateOptionsMenu(m);
    }

    /* Turn functionality on/off and notify user */
    public void switchPress(View v) {
        if(((Switch)v).isChecked()) {
            service_on = true;
            wl.acquire();  // Lock the Wifi radio.
            rescan();
        } else {
            // Clear the previous scan results.
            wifiFields = null;
            service_on = false;
            wl.release();
        }
    }

    public void refresh(View v) {
        rescan();

        /* Eventually I'll need a WifiLock.
         * https://developer.android.com/reference/android/net/wifi/WifiManager.WifiLock.html
         * Use `acquire()` and `release()`.
         */
    }

    private void rescan() {
        if(service_on) {
            wm.startScan();
        }
    }

    public void strengthSelected(View v) {
        if(((RadioButton)v).isChecked()) {
            // Reset the Fields cache.
            wifiFields = null;

            switch(v.getId()) {
            case R.id.radio_strong:
                threshold = Strength.STRONG;
                break;
            case R.id.radio_medium:
                threshold = Strength.MEDIUM;
                break;
            default:
                threshold = Strength.WEAK;
                break;
            }

            rescan();
        }
    }

    private Strength strengthOf(ScanResult sr) {
        int i = sr.level;

        if(i < -90) {
            return Strength.STRONG;
        } else if(i < -50) {
            return Strength.MEDIUM;
        } else {
            return Strength.WEAK;
        }
    }

    private boolean alreadyInField(List<ScanResult> curr) {
        for(ScanResult sr : curr) {
            if(inField(sr)) {
                return true;
            }
        }

        return false;
    }
    
    private boolean inField(ScanResult sr) {
        if(threshold.ordinal() <= strengthOf(sr).ordinal()) {
            return true;
        }

        return false;
    }

    /* Testing if user was in this field in the last scan */
    private ScanResult previousCopy(ScanResult c) {
        for(ScanResult wf : wifiFields) {
            if(c.SSID == wf.SSID) {
                return wf;
            }
        }

        return null;
    }

    /* Can trigger on two conditions:
     * 1. A new SSID is detected with a Strength passing the threshold.
     * 2. The strength of a field the user was in before passes the threshold.
     */
    private boolean enteredWifi(List<ScanResult> curr) {
        ScanResult p = null;

        /* On the first scan */
        if(wifiFields == null) {
            if(alreadyInField(curr)) {
                return true;
            }
            return false;
        }

        // TODO: Around here.
        for(ScanResult c : curr) {
            p = previousCopy(c);

            if(p != null) {
                if(strengthOf(c) != strengthOf(p) && inField(c)) {
                    return true;
                }
            } else if(inField(c)) {
                return true;
            }
        }

        return false;
    }

    private void genericNotification(String msg) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        b.setSmallIcon(R.drawable.ic_logo);
        b.setAutoCancel(true);
        b.setContentTitle(getResources().getString(R.string.notify_title));
        b.setContentText(msg);

        NotificationManager nm =
            (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.notify(GENERIC_NOTIFICATION, b.build());
    }
}
