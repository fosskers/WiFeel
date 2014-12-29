package com.android.wifeel;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
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
    private static Strength currStrength = Strength.WEAK;
    private static boolean service_on = false;
    private static List<ScanResult> wifiFields = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        IntentFilter i = new IntentFilter();
        i.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context c, Intent i) {
                    // Code to execute when the event occurs!
                    WifiManager wm = (WifiManager)c
                        .getSystemService(Context.WIFI_SERVICE);
                    List<ScanResult> rs = wm.getScanResults();  // <List>!
                    LinearLayout ll = (LinearLayout)findViewById(R.id.masterLayout);

                    for(ScanResult sr : rs) {
                        TextView t = new TextView(c);
                        t.setText(sr.SSID + " " + sr.level);
                        ll.addView(t);
                    }

                    if(enteredWifi(rs)) {
                        genericNotification("You are in a Wifi field!");
                    }

                    // Updated previous ScanResults
                    wifiFields = rs;
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
            // TODO: Make a scan here.
            service_on = true;
            refresh(v);
        } else {
            // Clear the previous scan results.
            wifiFields = null;
            service_on = false;
        }
    }

    public void refresh(View v) {
        WifiManager wm;

        if(service_on) {
            wm = (WifiManager)getSystemService(WIFI_SERVICE);
            wm.startScan();
        }

        /* Eventually I'll need a WifiLock.
         * https://developer.android.com/reference/android/net/wifi/WifiManager.WifiLock.html
         * Use `acquire()` and `release()`.
         */
    }

    public void strengthSelected(View v) {
        if(((RadioButton)v).isChecked()) {
            // Reset the Fields cache.
            wifiFields = null;

            switch(v.getId()) {
            case R.id.radio_strong:
                currStrength = Strength.STRONG;
                break;
            case R.id.radio_medium:
                currStrength = Strength.MEDIUM;
                break;
            default:
                currStrength = Strength.WEAK;
                break;
            }

            refresh(v);
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
            if(strengthOf(sr) == currStrength) {
                return true;
            }
        }

        return false;
    }
    
    private boolean inField(ScanResult sr) {
        if(currStrength == strengthOf(sr)) {
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
