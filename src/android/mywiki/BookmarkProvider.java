package android.mywiki;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class BookmarkProvider extends ContentProvider {
    private static final String LOG_TAG = "Wikipedia-BookmarkProvider";
    private static final String AUTHORITY = "android.mywiki.bookmark";

    private static final String DATABASE_NAME = "wikipedia.db";
    private static final int DATABASE_VERSION = 1;
    private static final String PAGES_TABLE_NAME = "wikipedia_bookmarks";

    private static final int PAGES = 1;
    private static final int PAGES_LANG = 2;
    private static final int PAGE_ID = 3;
    private static final int PAGE_TITLE = 4;

    private DatabaseHelper helper;
    private static final UriMatcher sUriMatcher;

    public static final class Page implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/bookmark");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY;
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY;
        public static final String DEFAULT_SORT_ORDER = "title DESC";
        public static final String TITLE = "title";
        public static final String TEXT = "text";
        public static final String LANG = "lang";
        public static final String CREATED_DATE = "created";
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + PAGES_TABLE_NAME + " ("
                    + Page._ID + " INTEGER PRIMARY KEY,"
                    + Page.TITLE + " TEXT,"
                    + Page.TEXT + " TEXT,"
                    + Page.LANG + " TEXT,"
                    + Page.CREATED_DATE + " INTEGER"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to "    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + PAGES_TABLE_NAME);
            onCreate(db);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case PAGES:
            count = db.delete(PAGES_TABLE_NAME, selection, selectionArgs);
            break;
        case PAGES_LANG:
             count = db.delete(PAGES_TABLE_NAME, Page.LANG + " = '" + uri.getPathSegments().get(2) +  "'" + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
        break;
        case PAGE_ID:
             count = db.delete(PAGES_TABLE_NAME, Page._ID + " = '" + uri.getPathSegments().get(2) +  "'" + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
        break;
        case PAGE_TITLE:
            count = db.delete(PAGES_TABLE_NAME, Page.LANG + " = '" + uri.getPathSegments().get(2) + "' AND " + Page.TITLE + "=" + uri.getPathSegments().get(3) +  "'" + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
        break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case PAGES:
                return Page.CONTENT_TYPE;
            case PAGES_LANG:
                return Page.CONTENT_TYPE;
            case PAGE_TITLE:
                return Page.CONTENT_ITEM_TYPE;
             case PAGE_ID:
                return Page.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (sUriMatcher.match(uri) != PAGES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (values == null || values.containsKey(Page.TITLE) == false) {
            throw new IllegalArgumentException("You must insert a valide page");
        }
        Long now = Long.valueOf(System.currentTimeMillis());
        if (values.containsKey(Page.TEXT) == false) {
            values.put(Page.TEXT, values.getAsString(Page.TITLE));
        }
        if (values.containsKey(Page.LANG) == false) {
            values.put(Page.LANG, Api.lang);
        }
        if (values.containsKey(Page.CREATED_DATE) == false) {
            values.put(Page.CREATED_DATE, now);
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        long rowId = db.insert(PAGES_TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(Page.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        this.helper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,    String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(PAGES_TABLE_NAME);
        switch (sUriMatcher.match(uri)) {
            case PAGES_LANG:
                qb.appendWhere(Page.LANG +  " = '" + uri.getPathSegments().get(2) + "'");
                break;
            case PAGE_ID:
                qb.appendWhere(Page._ID + " = '" + uri.getPathSegments().get(2) + "'");
                break;
            case PAGE_TITLE:
                qb.appendWhere(Page.LANG + " = '" + uri.getPathSegments().get(2) + "' AND " + Page.TITLE + " = '" + uri.getPathSegments().get(3) + "'");
                break;
            case PAGES:
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        String orderBy;
        if(TextUtils.isEmpty(sortOrder)) {
            orderBy = Page.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case PAGES:
                count = db.update(PAGES_TABLE_NAME, values, selection, selectionArgs);
                break;
            case PAGES_LANG:
                count = db.update(PAGES_TABLE_NAME, values, Page.LANG + " = '" + uri.getPathSegments().get(2) + "'" + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;
            case PAGE_ID:
                count = db.update(PAGES_TABLE_NAME, values, Page._ID + " = '" + uri.getPathSegments().get(2) + "'" + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;
            case PAGE_TITLE:
                count = db.update(PAGES_TABLE_NAME, values, Page.LANG + " = '" + uri.getPathSegments().get(2) + "' AND " + Page.TITLE + " = '" + uri.getPathSegments().get(3) +  "'" + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "bookmark", PAGES);
        sUriMatcher.addURI(AUTHORITY, "bookmark/id/*", PAGE_ID);
        sUriMatcher.addURI(AUTHORITY, "bookmark/lang/*", PAGES_LANG);
        sUriMatcher.addURI(AUTHORITY, "bookmark/title/*/*", PAGE_TITLE);
    }
}