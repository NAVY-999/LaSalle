package io.github.navy999.masalle;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;

// L'appli est la page web de MaSalle, servie depuis les fichiers de l'APK (aucune connexion nécessaire).
public class MainActivity extends Activity {
  private static final String HOST = "appassets.androidplatform.net";
  private WebView web;

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
    s.setAllowContentAccess(false);
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
    setContentView(web);
    web.loadUrl("https://" + HOST + "/assets/index.html");
  }

  // Retour : ferme d'abord la fenêtre ouverte dans l'appli, sinon quitte.
  @Override
  public void onBackPressed() {
    web.evaluateJavascript("(function(){try{if(ui.sheets.length){popSheet();return 1}}catch(e){}return 0})()",
        r -> { if (!"1".equals(r)) finish(); });
  }
}
