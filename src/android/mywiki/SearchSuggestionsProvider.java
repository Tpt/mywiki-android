package android.mywiki;

import java.net.URLEncoder;

import org.json.JSONArray;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class SearchSuggestionsProvider extends ContentProvider {
    private static final String AUTHORITY = "android.mywiki.searchsuggestions";
    private static final String LOG_TAG = "Wikipedia-SearchSuggestions";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String query = uri.getLastPathSegment();
        try {
            String[] columnNames = {BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_INTENT_DATA};
            MatrixCursor cursor = new MatrixCursor(columnNames);
            String content = Api.getContent("http://" + Api.lang + "." + Api.wiki + "/w/api.php?action=opensearch&limit=10&namespace=0&format=json&search=" + URLEncoder.encode(query, "UTF-8"));
            JSONArray response = new JSONArray(content);
            JSONArray suggestions = response.getJSONArray(1);
            int lenght = suggestions.length();
            for(int i = 0; i < lenght; i++) {
                String suggestion = suggestions.getString(i);
                String[] row = {String.valueOf(i), suggestion, "http://" + Api.lang + ".m." + Api.wiki + "/wiki/" + suggestion};
                cursor.addRow(row);
            }
            return cursor;
        } catch(Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }
}
