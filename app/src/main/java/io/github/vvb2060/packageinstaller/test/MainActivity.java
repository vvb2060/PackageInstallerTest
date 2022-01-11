package io.github.vvb2060.packageinstaller.test;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    static boolean Foreground;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private File apk;
    private boolean bgInstall;

    private TextView textView;
    private Button button;
    private Switch switchView;
    private TextView switchText;

    @Override
    @SuppressLint("SetTextI18n")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildView());
        apk = new File(getApplicationInfo().sourceDir);
        APKInstall.register(getApplicationContext());

        textView.setText(getInfo());
        button.setText("install self");
        button.setOnClickListener((v) -> executor.submit(() -> APKInstall.installapk(this, apk)));
        switchView.setOnCheckedChangeListener((v, isChecked) -> setBgInstall(isChecked));
        setBgInstall(bgInstall);
    }

    private void setBgInstall(boolean enable) {
        var text = enable ? "bg install on" : "bg install off";
        switchText.setText(text);
        button.setEnabled(!enable);
        bgInstall = enable;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Foreground = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Foreground = false;
        if (!bgInstall) return;
        executor.submit(() -> APKInstall.installapk(this, apk));
    }

    @SuppressLint("ResourceType")
    private View buildView() {
        var rootView = new RelativeLayout(this);
        var rootParams = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        rootView.setLayoutParams(rootParams);
        rootView.setFitsSystemWindows(true);

        textView = new TextView(this);
        var textParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        textParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        textParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        textView.setId(1);
        rootView.addView(textView, textParams);

        button = new Button(this);
        var buttonParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        buttonParams.addRule(RelativeLayout.BELOW, textView.getId());
        buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        int margin = 20;
        buttonParams.setMargins(margin, margin, margin, margin);
        button.setId(2);
        rootView.addView(button, buttonParams);

        var empty = new View(this);
        var emptyParams = new RelativeLayout.LayoutParams(0, 0);
        emptyParams.addRule(RelativeLayout.BELOW, button.getId());
        emptyParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        empty.setId(3);
        rootView.addView(empty, emptyParams);

        switchView = new Switch(this);
        var switchParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        switchParams.addRule(RelativeLayout.END_OF, empty.getId());
        switchParams.addRule(RelativeLayout.BELOW, button.getId());
        switchParams.setMargins(margin, margin, margin, margin);
        rootView.addView(switchView, switchParams);

        switchText = new TextView(this);
        var switchTextParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        switchTextParams.addRule(RelativeLayout.START_OF, empty.getId());
        switchTextParams.addRule(RelativeLayout.BELOW, button.getId());
        switchTextParams.setMargins(margin, margin, margin, margin);
        rootView.addView(switchText, switchTextParams);

        return rootView;
    }

    private String getInfo() {
        var sb = new StringBuilder();
        sb.append("sourceDir: ").append(getApplicationInfo().sourceDir).append("\n");
        var zone = ZoneId.systemDefault();
        var formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG);
        try {
            var info = getPackageManager().getPackageInfo(getPackageName(), 0);
            var instant = Instant.ofEpochMilli(info.firstInstallTime);
            var time = ZonedDateTime.ofInstant(instant, zone);
            sb.append("firstInstallTime: ").append(time.format(formatter)).append("\n");
            instant = Instant.ofEpochMilli(info.lastUpdateTime);
            time = ZonedDateTime.ofInstant(instant, zone);
            sb.append("lastUpdateTime: ").append(time.format(formatter)).append("\n");
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(MainActivity.class.getSimpleName(), e);
            sb.append(e.getMessage());
        }
        return sb.toString();
    }
}
