package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;



/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 *
 * Please read:
 *
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 *
 * before you start to get yourself familiarized with ContentProvider.
 *
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        try {
            Log.d("Sum", "Inside insert-Provider");
            //FileOutputStream object to write to file
            FileOutputStream fout = getContext().openFileOutput(values.get("key").toString(), Context.MODE_PRIVATE);
            //Writing values to file specified with key as name
            fout.write(values.get("value").toString().getBytes());
            fout.close();
        }
        catch (Exception e) {
            Log.e("Exception",e.toString());
        }
        return uri;

    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        try{
            Log.d("Sum", "Inside query-Provider");
            //FileInputStream object to read from file
            FileInputStream fin = getContext().openFileInput(selection);
            //FileInputStream fin = new FileInputStream(file);
            //BufferedReader object
            BufferedReader read = new BufferedReader(new InputStreamReader(fin));
            //Read value from file
            String value = read.readLine();
            //MatrixCursor object to return key value pair
            //Referred from https://developer.android.com/reference/android/database/MatrixCursor.html
            String[] str = new String[] {"key","value"};
            MatrixCursor cur = new MatrixCursor(str);
            //Add data to MatrixCursor object
            String[] curadd = new String[] {selection,value};
            cur.addRow(curadd);
            read.close();
            fin.close();
            return cur;
        }
        catch (Exception e) {
            Log.v("Exception", e.toString());
        }
        return null;
    }
}
