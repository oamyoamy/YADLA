package com.kludgenics.alrightypump.cloud.nightscout.records

import org.joda.time.format.ISODateTimeFormat

/**
 * Created by matthiasgranberry on 5/26/15.
 */
public interface NightscoutEntry {
    val id: String
    val device: String
    val type: String
    val date: Long
    val dateString: String get() = ISODateTimeFormat.dateTime().print(date)
}