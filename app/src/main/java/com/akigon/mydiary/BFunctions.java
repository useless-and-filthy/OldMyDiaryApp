package com.akigon.mydiary;

import android.text.format.DateFormat;
import android.util.Log;

import com.akigon.mydiary.db.Diary;

import java.util.Calendar;
import java.util.Locale;

public class BFunctions {

    public static final int PICK_IMAGE = 100;
    public static final int PICK_VIDEO = 101;
    public static final int PICK_AUDIO = 102;
    public static final int CHOOSE_CSV = 124;
    public static final int ADD_NOTE = 256;
    public static final int EDIT_NOTE = 257;
    public static final int SETTING_SEND = 258;

    public static final String SP_LAST_SYNC_STAMP = "LAST_SYNC_STAMP";
    public static final String SP_AUTO_SYNC_KEY = "AUTO_SYNC";
    public static final String SP_PASS_CODE_KEY = "PASS_CODE";
    public static final String SP_SECURITY_QUESTION_KEY = "QUESTION";

    public static final String ACTION_RESTORED = "RESTORED";
    public static final String ACTION_AUTO_SYNC = "AUTO_SYNC";
    public static final String LOGIN_BLOCKED_TILL = "LOGIN_BLOCKED_AT";

    public static final String feedback_url = "https://forms.gle/2Yf9V12yuhzf3TWD9";
    public static final String open_source_license_url = "https://akigon-journals.web.app/credits.html";
    public static final String terms_condition_url = "https://akigon-journals.web.app/termsandcondition.html";
    public static final String privacy_policy_url = "https://akigon-journals.web.app/privacy_policy.html";

    public static String[] getDiaryArray(Diary diary) {
        return new String[]{diary.getObjectId(),
                diary.getUpdatedAt() + "",
                diary.getCreatedAt() + "",
                diary.getTitle(),
                diary.getContent(),
                diary.getUid(),
                diary.getTags(),
                String.valueOf(diary.isHidden())
        };
    }

//    public static String getTimeString(long timeStamp) {
//        String date_format = "EEE MMM dd yyyy hh:mm a";
//        Calendar cal = Calendar.getInstance(Locale.getDefault());
//        cal.setTimeInMillis(timeStamp);
//        return DateFormat.format(date_format, cal).toString();
//    }

    public static String getTimeString(long timeStamp) {
        String date_format = "dd MMM yyyy";
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timeStamp);
        return DateFormat.format(date_format, cal).toString();
    }

    public static String getTagsFormatted(String s) {
        if (s.isEmpty()) {
            return s;
        } else {
            String[] tgs = s.split(";");
            StringBuilder m = new StringBuilder();
            for (String p : tgs) {
                if (!p.isEmpty()) {
                    if (m.toString().isEmpty()) {
                        m.append(p);
                    } else {
                        m.append(", ").append(p);
                    }
                }
            }
            return m.toString();
        }
    }

    private static final String TAG = "BFunctions";

    public static int minToRead(String s) {
        return (int) Math.ceil((double) s.split("\\s+").length / 175);
    }

}
