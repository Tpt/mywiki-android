package android.mywiki;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class Bookmarks extends ListActivity {
    private static final String LOG_TAG = "Wikipedia-BookmarkProvider";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        Cursor cursor = this.managedQuery(BookmarkProvider.Page.CONTENT_URI, new String[] {BookmarkProvider.Page._ID, BookmarkProvider.Page.TITLE, BookmarkProvider.Page.TEXT} , null, null, null);
        ListAdapter adapter = new SimpleCursorAdapter(this, R.layout.bookmark_row, cursor, new String[] {BookmarkProvider.Page.TEXT}, new int[] {android.R.id.text1});
        this.setListAdapter(adapter);
        registerForContextMenu(this.getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(LOG_TAG, "bad menuInfo", e);
            return;
        }
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bookmark, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(LOG_TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
            case R.id.menu_view:
                Cursor cursor = this.managedQuery(Uri.parse(BookmarkProvider.Page.CONTENT_URI + "/id/" + info.id), new String[] {BookmarkProvider.Page._ID, BookmarkProvider.Page.TITLE, BookmarkProvider.Page.LANG} , null, null, null);
                cursor.moveToFirst();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Api.getUrl("wiki/" + cursor.getString(1))), this, Main.class);
                this.startActivity(intent);
                return true;
            case R.id.menu_delete:
                this.getContentResolver().delete(Uri.parse(BookmarkProvider.Page.CONTENT_URI + "/id/" + info.id), null, null);
                return true;
        }
        return false;
    }

    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        Cursor cursor = this.managedQuery(Uri.parse(BookmarkProvider.Page.CONTENT_URI + "/id/" + id), new String[] {BookmarkProvider.Page._ID, BookmarkProvider.Page.TITLE, BookmarkProvider.Page.LANG} , null, null, null);
        cursor.moveToFirst();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + cursor.getString(2) + ".m." + Api.wiki + "/wiki/" + cursor.getString(1)), this, Main.class);
        this.startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        if (Api.isConnected) {
            menu.setGroupVisible(R.id.group_connected, true);
        }
        menu.findItem(R.id.menu_bookmarks).setVisible(false);
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