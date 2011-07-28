package android.mywiki;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class Preference extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        menu.findItem(R.id.menu_preferences).setVisible(false);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(Api.isConnected) {
        	 menu.setGroupVisible(R.id.group_connected, true);
        } else {
        	 menu.setGroupVisible(R.id.group_connected, false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.menu_home:
                intent = new Intent(Intent.ACTION_MAIN, null, this, Main.class);
                this.startActivity(intent);
                return true;
            case R.id.menu_random:
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Api.getUrl("wiki/::Random")), this, Main.class);
                this.startActivity(intent);
                return true;
            case R.id.menu_search:
                this.onSearchRequested();
                return true;
            case R.id.menu_bookmarks:
                intent = new Intent(Intent.ACTION_MAIN, Uri.parse(Api.getUrl("")), this, Bookmarks.class);
                this.startActivity(intent);
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
