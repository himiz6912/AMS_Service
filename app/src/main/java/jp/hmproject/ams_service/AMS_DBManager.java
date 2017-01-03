package jp.hmproject.ams_service;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hm on 12/23/2016.
 */
public class AMS_DBManager {
    final String TAG = "AMS_DBManager";
    private SQLiteDatabase mDB;
    private Context context;
    private SharedPreferences sp;
    private dbHelper dh;

    public AMS_DBManager(Context ctx) {
        this.context = ctx;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        dh = new dbHelper(context);
        mDB = dh.getWritableDatabase();
    }

    public void close(){
        if(dh != null){
            dh.close();
        }
    }

    private static class dbHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "ams";
        private static final int DATABASE_VERSION = 3;
        dbHelper(Context context){
            super(context,DATABASE_NAME,null,DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db){
            db.execSQL(
                    "create table ams_table(" +
                            "aid INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "flag INTEGER NOT NULL," +
                            "event INTEGER NOT NULL," +
                            "accuracy REAL," +
                            "latitude REAL," +
                            "longitude REAL," +
                            "generation TEXT," +
                            "registration TEXT," +
                            "user TEXT," +
                            "location TEXT," +
                            "system TEXT," +
                            "operation TEXT," +
                            "remarks TEXT," +
                            "twopeople INTEGER," +
                            "dailysubcon INTEGER," +
                            "sending TEXT," +
                            "revise TEXT" +
                            ");"


            );
            db.execSQL("create index ams_table_index on ams_table(aid);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db,int oldVersion, int newVersion){
            Log.w("AMS_DBManager/dbHelper","onUpgrade:" + oldVersion + " > " + newVersion);
            db.execSQL("drop table if exsists ams_table");
            onCreate(db);
        }
    }

    private String DateToString(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(date);
    }
    private Date StringToDate(String str){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            return sdf.parse(str);
        }catch(ParseException e){
            return null;
        }
    }
    private String[] mSplit(String s){
        String REGEX_CSV_COMMA = ",(?=(([^\"]*\"){2})*[^\"]*$)";
        String REGEX_SURROUND_DOUBLEQUATATION = "^\"|\"$";
        String REGEX_DOUBLEQUOATATION = "\"\"";
        String[] res = null;
        try {
            Pattern cPattern = Pattern.compile(REGEX_CSV_COMMA);
            String[] cols = cPattern.split(s, -1);
            res	= new String[cols.length];
            for(int i=0,len=cols.length;i<len;i++) {
                String col = cols[i].trim();
                Pattern sdqPattern =	Pattern.compile(REGEX_SURROUND_DOUBLEQUATATION);
                Matcher matcher = sdqPattern.matcher(col);
                col = matcher.replaceAll("");
                Pattern dqPattern = Pattern.compile(REGEX_DOUBLEQUOATATION);
                matcher = dqPattern.matcher(col);
                col = matcher.replaceAll("\"");
                res[i] = col;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return res;
    }
    private String curToJsonString(Cursor cursor) {
        JSONArray resultSet = new JSONArray();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();
            for (int i = 0; i < totalColumn; i++) {
                if (cursor.getColumnName(i) != null) {
                    try {
                        rowObject.put(cursor.getColumnName(i),
                                cursor.getString(i));
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());
                    }
                }
            }
            resultSet.put(rowObject);
            cursor.moveToNext();
        }
        cursor.close();
        return resultSet.toString();
    }

//
// To create original procedures from the below.
//
    public boolean setTraceData(AMS_Data ams_data){
        boolean res = false;
        String sql = "insert into ams_table(flag,event,accuracy,latitude,longitude,generation," +
                "registration) values(?,?,?,?,?,?,?);";
        SQLiteStatement stmt = mDB.compileStatement(sql);
        stmt.bindLong(1, ams_data.flag);
        stmt.bindLong(2, ams_data.event);
        stmt.bindDouble(3, ams_data.accuracy);
        stmt.bindDouble(4, ams_data.latitude);
        stmt.bindDouble(5, ams_data.longitude);
        stmt.bindString(6, DateToString(ams_data.generation));
        stmt.bindString(7, DateToString(new Date()));
        if(stmt.executeInsert() > 0) res = true;
        stmt.clearBindings();
        stmt.close();
        return res;
    }

    public String getAllTraceData(){
        ArrayList<AMS_Data> al = new ArrayList<AMS_Data>();
        String sql = "select aid,accuracy,latitude,longitude,generation,registration" +
                " from ams_table where event = 4;";
        Cursor c = mDB.rawQuery(sql,null);
        return curToJsonString(c);
    }
}
