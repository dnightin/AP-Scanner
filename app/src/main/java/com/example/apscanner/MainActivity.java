package com.example.apscanner;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private WifiManager wifiManager;
    private TextView statusText;
    private TextView totalCountText;
    private TextView strongestText;
    private TextView band24Text;
    private TextView band5Text;
    private TextView band6Text;
    private TextView lastScanText;
    private Button scanButton;
    private ListView resultsList;
    private ScanResultAdapter adapter;

    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            showScanResults(updated);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        statusText = findViewById(R.id.statusText);
        totalCountText = findViewById(R.id.totalCountText);
        strongestText = findViewById(R.id.strongestText);
        band24Text = findViewById(R.id.band24Text);
        band5Text = findViewById(R.id.band5Text);
        band6Text = findViewById(R.id.band6Text);
        lastScanText = findViewById(R.id.lastScanText);
        scanButton = findViewById(R.id.scanButton);
        resultsList = findViewById(R.id.resultsList);

        adapter = new ScanResultAdapter(this);
        resultsList.setAdapter(adapter);

        scanButton.setOnClickListener(v -> startWifiScan());
        statusText.setText("Ready. Tap Scan to find nearby access points.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(scanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        if (hasWifiScanPermissions()) {
            showScanResults(false);
        } else {
            requestWifiScanPermissions();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(scanReceiver);
    }

    private void startWifiScan() {
        if (!hasWifiScanPermissions()) {
            requestWifiScanPermissions();
            return;
        }

        if (!wifiManager.isWifiEnabled()) {
            statusText.setText("Wi-Fi is off. Turn on Wi-Fi and scan again.");
            lastScanText.setText("Wi-Fi disabled");
            Toast.makeText(this, "Wi-Fi is off", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.Panel.ACTION_WIFI));
            return;
        }

        statusText.setText("Scanning...");
        scanButton.setEnabled(false);
        scanButton.setText("Scanning");
        lastScanText.setText("Scan in progress");
        boolean started = wifiManager.startScan();
        if (!started) {
            statusText.setText("Showing cached results. Android may be throttling scans.");
            showScanResults(false);
        }
    }

    private boolean hasWifiScanPermissions() {
        boolean hasLocation = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return hasLocation && checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED;
        }
        return hasLocation;
    }

    private void requestWifiScanPermissions() {
        List<String> permissions = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }

        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && hasWifiScanPermissions()) {
            startWifiScan();
        } else if (requestCode == PERMISSION_REQUEST_CODE) {
            statusText.setText(getString(R.string.permission_needed));
        }
    }

    private void showScanResults(boolean freshResults) {
        if (!hasWifiScanPermissions()) {
            statusText.setText(getString(R.string.permission_needed));
            return;
        }

        List<ScanResult> results = new ArrayList<>(wifiManager.getScanResults());
        Collections.sort(results, Comparator.comparingInt((ScanResult result) -> result.level).reversed());

        adapter.setResults(results);
        resultsList.setSelection(0);
        updateSummary(results, freshResults);
        scanButton.setEnabled(true);
        scanButton.setText(R.string.scan);
        if (results.isEmpty()) {
            statusText.setText("No access points found. Make sure location is enabled, then scan again.");
        } else {
            String source = freshResults ? "fresh" : "cached";
            statusText.setText(results.size() + " access points found (" + source + " results).");
        }
    }

    private void updateSummary(List<ScanResult> results, boolean freshResults) {
        int band24 = 0;
        int band5 = 0;
        int band6 = 0;
        for (ScanResult result : results) {
            if (result.frequency < 3000) {
                band24++;
            } else if (result.frequency < 5955) {
                band5++;
            } else {
                band6++;
            }
        }

        totalCountText.setText(results.size() + (results.size() == 1 ? " AP" : " APs"));
        band24Text.setText("2.4 GHz  " + band24);
        band5Text.setText("5 GHz  " + band5);
        band6Text.setText("6 GHz  " + band6);

        if (results.isEmpty()) {
            strongestText.setText("No access points visible yet");
            lastScanText.setText("Tap Scan to refresh");
            return;
        }

        ScanResult strongest = results.get(0);
        String ssid = displaySsid(strongest);
        strongestText.setText("Strongest: " + ssid + " at " + strongest.level + " dBm");
        lastScanText.setText((freshResults ? "Fresh scan" : "Cached scan") + " sorted by strongest signal");
    }

    private static int channelFromFrequency(int frequencyMhz) {
        if (frequencyMhz >= 2412 && frequencyMhz <= 2472) {
            return (frequencyMhz - 2407) / 5;
        }
        if (frequencyMhz == 2484) {
            return 14;
        }
        if (frequencyMhz >= 5000 && frequencyMhz <= 5895) {
            return (frequencyMhz - 5000) / 5;
        }
        if (frequencyMhz >= 5955 && frequencyMhz <= 7115) {
            return ((frequencyMhz - 5950) / 5);
        }
        return -1;
    }

    private static String bandFromFrequency(int frequencyMhz) {
        if (frequencyMhz < 3000) {
            return "2.4 GHz";
        }
        if (frequencyMhz < 5955) {
            return "5 GHz";
        }
        return "6 GHz";
    }

    private static String displaySsid(ScanResult result) {
        return result.SSID == null || result.SSID.isEmpty() ? "<hidden SSID>" : result.SSID;
    }

    private static String signalQuality(int level) {
        if (level >= -55) {
            return "Excellent";
        }
        if (level >= -67) {
            return "Good";
        }
        if (level >= -75) {
            return "Fair";
        }
        return "Weak";
    }

    private static int signalPercent(int level) {
        int percent = 2 * (level + 100);
        return Math.max(0, Math.min(100, percent));
    }

    private static class ScanResultAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private final Context context;
        private final List<ScanResult> results = new ArrayList<>();

        ScanResultAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
        }

        void setResults(List<ScanResult> newResults) {
            results.clear();
            results.addAll(newResults);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return results.size();
        }

        @Override
        public Object getItem(int position) {
            return results.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder;
            if (view == null) {
                view = inflater.inflate(R.layout.list_item_scan_result, parent, false);
                holder = new ViewHolder();
                holder.ssidText = view.findViewById(R.id.ssidText);
                holder.signalText = view.findViewById(R.id.signalText);
                holder.signalBar = view.findViewById(R.id.signalBar);
                holder.channelText = view.findViewById(R.id.channelText);
                holder.bandText = view.findViewById(R.id.bandText);
                holder.qualityText = view.findViewById(R.id.qualityText);
                holder.detailsText = view.findViewById(R.id.detailsText);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            ScanResult result = results.get(position);
            String ssid = displaySsid(result);
            int channel = channelFromFrequency(result.frequency);
            String channelText = channel > 0 ? "Ch " + channel : "Ch ?";
            String band = bandFromFrequency(result.frequency);
            int levelColor = colorForSignal(result.level);

            holder.ssidText.setText(ssid);
            holder.signalText.setText(result.level + " dBm");
            holder.signalText.setTextColor(levelColor);
            holder.signalBar.setProgress(signalPercent(result.level));
            tintProgress(holder.signalBar, levelColor);
            holder.channelText.setText(channelText);
            holder.bandText.setText(band);
            holder.qualityText.setText(signalQuality(result.level));
            holder.qualityText.setTextColor(levelColor);
            holder.detailsText.setText(
                    result.frequency
                            + " MHz  |  "
                            + result.BSSID
                            + "  |  "
                            + result.capabilities);
            return view;
        }

        private int colorForSignal(int level) {
            if (level >= -67) {
                return context.getColor(R.color.signal_good);
            }
            if (level >= -75) {
                return context.getColor(R.color.signal_ok);
            }
            return context.getColor(R.color.signal_weak);
        }

        private void tintProgress(ProgressBar progressBar, int color) {
            Drawable drawable = progressBar.getProgressDrawable();
            if (drawable instanceof LayerDrawable) {
                Drawable progress = ((LayerDrawable) drawable).findDrawableByLayerId(android.R.id.progress);
                if (progress instanceof ClipDrawable) {
                    progress.setTint(color);
                }
            }
        }
    }

    private static class ViewHolder {
        TextView ssidText;
        TextView signalText;
        ProgressBar signalBar;
        TextView channelText;
        TextView bandText;
        TextView qualityText;
        TextView detailsText;
    }
}
