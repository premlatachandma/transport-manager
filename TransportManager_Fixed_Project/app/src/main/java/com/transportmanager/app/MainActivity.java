package com.transportmanager.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.widget.Toast;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int REQ_SAVE = 1, REQ_PICK = 2;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingCsv;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(android.graphics.Color.rgb(36,52,71));
        w.setNavigationBarColor(android.graphics.Color.rgb(36,52,71));
        web = new WebView(this);
        web.setBackgroundColor(android.graphics.Color.rgb(243,245,248));
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void saveCsv(final String name, final String csv) {
                runOnUiThread(new Runnable() { public void run() {
                    pendingCsv = csv;
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("text/csv");
                    i.putExtra(Intent.EXTRA_TITLE, name);
                    startActivityForResult(i, REQ_SAVE);
                }});
            }
        }, "Android");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try { startActivityForResult(Intent.createChooser(i, "Select CSV file"), REQ_PICK); }
                catch (Exception e) { fileCallback = null; cb.onReceiveValue(null); }
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_SAVE && res == RESULT_OK && data != null && data.getData() != null && pendingCsv != null) {
            try (OutputStream os = getContentResolver().openOutputStream(data.getData())) {
                os.write(pendingCsv.getBytes("UTF-8"));
                Toast.makeText(this, "CSV saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not save file", Toast.LENGTH_LONG).show();
            }
            pendingCsv = null;
        } else if (req == REQ_PICK && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            fileCallback = null;
        }
    }

    @Override public void onBackPressed() { finish(); }
}
