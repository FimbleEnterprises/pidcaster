package com.fimbleenterprises.torquebroadcaster.utils;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Matt on 10/27/2016.
 */

public class Logger extends ArrayList<Logger.LogEvent> {

    Context context;
    // private List<LogEvent> logEvents;
    private int limit = 10;

    public Logger(Context context) {
        this.context = context;

    }

    /**
     * The amount of log entries allowed in this Logger
     * @return The current limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Sets the maximum amount of log entries to retain
     * @param setLimit The integer to apply
     */
    public void setLimit(int setLimit) {
        this.limit = setLimit;
    }

    /**
     * Outputs all log entries
     * @return All log entries as a string
     */
    public String write() {
        StringBuilder sb = new StringBuilder();
        for (LogEvent logEvent :
                this) {
            sb.append(logEvent.eventText);
        };
        return sb.toString();
    }

    /**
     * Appends a log entry to the existing log entries.  If the current amount of log entries
     * exceeds or equals the set limit then entries will be purged starting from the oldest entry
     * and working forward until the count is less than the limit.
     * @param logEvent
     */
    public void append(LogEvent logEvent) {

        // If the limit is set to zero then ignore the limit
        if (this.limit == 0) {
            add(logEvent);
            return;
        }

        // Trims the array (from the beginning) until its count is at the configured limit
        if (this.size() >= this.limit) {
            int overage = (this.size() - this.limit);
            for (int i = 0; i < overage; i++) {
                this.remove(i);
            }
        }
        add(logEvent);
    }

    public List<LogEvent> getAll() {
        List<LogEvent> allEntries = new ArrayList<>();
        for (LogEvent logEvent : this) {
            allEntries.add(logEvent);
        }
        return allEntries;
    }

    public static class LogEvent implements Parcelable {

        public String eventText;
        public Date timeStamp;

        /**
         * Logs the supplied string and automatically sets the timestamp to now
         * @param eventText The text to log
         */
        public LogEvent(String eventText) {
            this.eventText = eventText;
            this.timeStamp = new Date(System.currentTimeMillis());
        }

        /**
         * Logs the supplied string and sets a timestamp using the supplied Date
         * @param eventText The text to log
         * @param timeStamp The date and time to use as the timestamp
         */
        public LogEvent(String eventText, Date timeStamp) {
            this.eventText = eventText;
            this.timeStamp = timeStamp;
        }

        protected LogEvent(Parcel in) {
            eventText = in.readString();
            long tmpTimeStamp = in.readLong();
            timeStamp = tmpTimeStamp != -1 ? new Date(tmpTimeStamp) : null;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(eventText);
            dest.writeLong(timeStamp != null ? timeStamp.getTime() : -1L);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<LogEvent> CREATOR = new Parcelable.Creator<LogEvent>() {
            @Override
            public LogEvent createFromParcel(Parcel in) {
                return new LogEvent(in);
            }

            @Override
            public LogEvent[] newArray(int size) {
                return new LogEvent[size];
            }
        };
    }

}
