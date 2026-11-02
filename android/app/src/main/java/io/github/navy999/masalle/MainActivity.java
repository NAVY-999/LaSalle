package io.github.navy999.masalle;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;
import java.io.File;

// L'appli est la page web de MaSalle, servie depuis les fichiers de l'APK (aucune connexion nécessaire).
public class MainActivity extends Activity {
  private static final String HOST = "appassets.androidplatform.net";
  private static final int PICK_PHOTO = 42;
  private WebView web;
  private ValueCallback<Uri[]> pendingPhoto;
  private Uri cameraUri;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
        .build();
    web = new WebView(this);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(false);
    s.setAllowContentAccess(true);
    web.setWebViewClient(new WebViewClient() {
      @Override
      public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
        return loader.shouldInterceptRequest(r.getUrl());
      }

      @Override
      public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        Uri u = r.getUrl();
        if (HOST.equals(u.getHost())) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
        return true;
      }
    });
    // « Photo du plan » : propose l'appareil photo ou la galerie.
    web.setWebChromeClient(new WebChromeClient() {
      @Override
      public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (pendingPhoto != null) pendingPhoto.onReceiveValue(null);
        pendingPhoto = callback;
        Intent gallery = new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
        Intent chooser = Intent.createChooser(gallery, "Photo du plan");
        cameraUri = null;
        try {
          File dir = new File(getCacheDir(), "photos");
          dir.mkdirs();
          cameraUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".photos", new File(dir, "plan.jpg"));
          Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
              .putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
              .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
          chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { camera });
        } catch (Exception e) {
          cameraUri = null;
        }
        try {
          startActivityForResult(chooser, PICK_PHOTO);
        } catch (Exception e) {
          pendingPhoto = null;
          return false;
        }
        return true;
      }
    });
    setContentView(web);
    web.loadUrl("https://" + HOST + "/assets/index.html");
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request != PICK_PHOTO || pendingPhoto == null) return;
    Uri[] picked = null;
    if (result == RESULT_OK) {
      if (data != null && data.getData() != null) picked = new Uri[] { data.getData() };
      else if (cameraUri != null) picked = new Uri[] { cameraUri };
    }
    pendingPhoto.onReceiveValue(picked);
    pendingPhoto = null;
  }

  // Retour : ferme d'abord la fenêtre ouverte dans l'appli, sinon quitte.
  @Override
  public void onBackPressed() {
    web.evaluateJavascript("(function(){try{if(ui.sheets.length){popSheet();return 1}}catch(e){}return 0})()",
        r -> { if (!"1".equals(r)) finish(); });
  }
}
