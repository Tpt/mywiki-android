package android.mywiki;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public final class Main extends Activity {
    protected WebView view;
    protected String url = "";
    protected String lang;
    protected Intent intent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.view = (WebView)findViewById(R.id.webView);
        this.intent = getIntent();
        if (Intent.ACTION_MAIN.equals(this.intent.getAction())) {
            Api.lang = PreferenceManager.getDefaultSharedPreferences(this).getString("language", this.getResources().getConfiguration().locale.getLanguage());
        }
        this.lang = Api.lang;
        Api.wiki = "wikipedia.org";
        final Main activity = this;
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            Api.isConnected = false;
        } else {
            State networkState = networkInfo.getState();
            Api.isConnected = (networkState.compareTo(State.CONNECTED) == 0);
        }

        if (Api.isConnected) {
            if (Intent.ACTION_SEARCH.equals(this.intent.getAction())) {
                this.url = "wiki?search=" + intent.getStringExtra(SearchManager.QUERY);
            } else if (Intent.ACTION_VIEW.equals(this.intent.getAction())) {
                this.url = intent.getData().getPath();
                this.lang = intent.getData().getAuthority().split("\\.")[0];
            }
            
            WebSettings webSettings = this.view.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setBuiltInZoomControls(true);

            this.view.setWebViewClient(new WebViewClient() {
                public ProgressDialog progressDialog;

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if(Uri.parse(url).getHost().contains("." + Api.wiki)) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url), activity, Main.class);
                        startActivity(intent);
                        return true;
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    try {
        				activity.view.loadData(Api.getContent(getResources().openRawResource(R.raw.noresult)), "text/html", "utf-8");
        			} catch (Exception e) {
        				activity.view.loadData("Error !", "text/html", "utf-8");
        			}
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    this.progressDialog = ProgressDialog.show(activity, "", getResources().getText(R.string.loading), true);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    activity.setTitle(activity.view.getTitle());
                    view.loadUrl("javascript:" +
                        "$('#searchbox').remove();" +
                        "$('#nav').remove();" +
                        "$('#footmenu').remove();"
                    );
                    this.progressDialog.dismiss();
                }
            });

            this.view.loadUrl("http://" + this.lang + ".m." + Api.wiki + "/" + url);
        } else {
            try {
				this.view.loadData(Api.getContent(getResources().openRawResource(R.raw.noresult)), "text/html", "utf-8");
			} catch (Exception e) {
				this.view.loadData("Error !", "text/html", "utf-8");
			}
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        if (Api.isConnected) {
            menu.setGroupVisible(R.id.group_connected, true);
            menu.setGroupVisible(R.id.group_page, true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.menu_home:
                intent = new Intent(Intent.ACTION_MAIN, Uri.parse(Api.getUrl("")), this, Main.class);
                this.startActivity(intent);
                return true;
            case R.id.menu_random:
                /* try {
                    String url = Api.getRedirect(this.getUrl("wiki/::Random"));
                    String url = Api.getContent("http://en.m.wikipedia.org/wiki/::Random");
                    Toast.makeText(this, url, Toast.LENGTH_LONG);
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MIMETYPE + "wiki/" + Uri.parse(url).getLastPathSegment()), this, Main.class);
                } catch (ApiException e) {
                    Log.e(LOG_TAG, e.getMessage());
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG);
                } */
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Api.getUrl("wiki/::Random")), this, Main.class);
                this.startActivity(intent);
                return true;
            case R.id.menu_search:
                this.onSearchRequested();
                return true;
            case R.id.menu_refresh:
                this.view.reload();
                return true;
            case R.id.menu_share:
                intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("plain/text");
                intent.putExtra(android.content.Intent.EXTRA_TEXT, this.getString(R.string.share_text) + " " + Api.getUrl(this.url));
                this.startActivity(Intent.createChooser(intent, this.getString(R.string.share)));
                return true;
            case R.id.menu_bookmarks:
                intent = new Intent(Intent.ACTION_MAIN, Uri.parse(Api.getUrl("")), this, Bookmarks.class);
                this.startActivity(intent);
                return true;
            case R.id.menu_add_bookmark:
                ContentValues values=new ContentValues(3);
                if (Intent.ACTION_MAIN.equals(this.intent.getAction())) {
                	values.put(BookmarkProvider.Page.TITLE, "");
                } else {
                	values.put(BookmarkProvider.Page.TITLE, Uri.parse(this.url).getLastPathSegment());
                }
                values.put(BookmarkProvider.Page.TEXT, this.view.getTitle());
                values.put(BookmarkProvider.Page.LANG, this.lang);
                getContentResolver().insert(BookmarkProvider.Page.CONTENT_URI,    values);
                return true;
            case R.id.menu_preferences:
                intent = new Intent(this, Preference.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
