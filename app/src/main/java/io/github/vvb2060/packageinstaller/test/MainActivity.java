package io.github.vvb2060.packageinstaller.test;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
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
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private TextView textView;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildView());
        textView.setText(getInfo());
        //noinspection SetTextI18n
        button.setText("install self");
        APKInstall.register(getApplicationContext());
        var apk = new File(getApplicationInfo().sourceDir);
        button.setOnClickListener((v) -> executor.submit(() -> APKInstall.installapk(this, apk)));
    }

    private View buildView() {
        var rootView = new RelativeLayout(this);
        var rootParams = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        rootView.setLayoutParams(rootParams);
        rootView.setFitsSystemWindows(true);

        textView = new TextView(this);
        var textParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        textParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        textParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        //noinspection ResourceType
        textView.setId(42);
        rootView.addView(textView, textParams);

        button = new Button(this);
        var buttonParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        buttonParams.addRule(RelativeLayout.BELOW, 42);
        buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        int margin = 20;
        buttonParams.setMargins(margin, margin, margin, margin);
        rootView.addView(button, buttonParams);

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
