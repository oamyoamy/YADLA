package com.kludgenics.cgmlogger.model.math.agp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.RealmClass
import org.joda.time.DateTime
import org.joda.time.Period
import java.util.*
import com.kludgenics.cgmlogger.extension.*
import org.jetbrains.anko.*
import java.util.concurrent.Executors

/**
 * Created by matthiasgranberry on 7/5/15.
 */

@RealmClass public open class CachedDatePeriodAgp(public open var outer: String = "",
                                                  public open var inner: String = "",
                                                  public open var median: String = "",
                                                  public open var date: Date = Date(),
                                                  public open var period: Int = 30): RealmObject() {
    @Ignore
    public var svgHeight: Float = DailyAgp.SPEC_HEIGHT
    @Ignore
    public var svgWidth: Float = DailyAgp.SPEC_WIDTH
}

public object AgpUtil: AnkoLogger {
    val executor = Executors.newScheduledThreadPool(1)
    fun getLatestCached(context: Context, realm: Realm, period: Period,
                        updated: ((CachedDatePeriodAgp)->Unit)? = null,
                        dateTime: DateTime = DateTime().withTimeAtStartOfDay()): CachedDatePeriodAgp {
        info("$period Querying cache")
        val result = realm.where<CachedDatePeriodAgp> {
            equalTo("period", period.getDays())
        }.findAllSorted("date", false).firstOrNull()

        return if (result == null) {
            info("$period Result not cached, returning dummy")
            context.asyncResult(executor) {
                calculateAndCacheAgp(dateTime, period, updated)
            }
            CachedDatePeriodAgp(date=dateTime.toDate(),period=period.getDays())

        } else if (result.dateTime != dateTime) {
            info("Result cached, but stale.  Calculating in background.")
            context.asyncResult(executor) {
                info("starting bg")
                calculateAndCacheAgp(dateTime, period, updated)
                info("ending bg")
            }
            result
        } else {
            info("Current result cache is valid.  Returning cache.")
            result
        }
    }

    private fun calculateAndCacheAgp(dateTime: DateTime, period: Period, updated: ((CachedDatePeriodAgp)->Unit)? = null) {
        info ("$period Calculating agp: ${dateTime}, ${period}")
        val currentAgp = DailyAgp(dateTime, period)
        val realm = Realm.getDefaultInstance()
        realm.use {
            info("$period Storing cached AGP")
            val ro = realm.create<CachedDatePeriodAgp> {
                this.dateTime = dateTime
                if (currentAgp.percentiles.size() >= 5) {
                    this.outer = currentAgp.pathStrings[0]
                    this.inner = currentAgp.pathStrings[1]
                    this.median = currentAgp.pathStrings[2]
                } else {
                    this.outer = ""
                    this.inner = ""
                    this.median = ""
                }
                this.period = period.getDays()
            }
            info("$period Calculation completed")
            updated?.invoke(ro)
        }
    }
}
public var CachedDatePeriodAgp.dateTime: DateTime
    get() = DateTime(date)
    set(value) { date = value.toDate() }


public val CachedDatePeriodAgp.svg: String
    get() =
            """
<?xml version="1.0" encoding="utf-8" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg height="${svgHeight}pt" version="1.1" viewBox="0 0 240 400" width="${svgWidth}pt" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
    <g id="agp">
        <path d="${outer}" fill="#2d95c2"/>
        <path d="${inner}" fill="#005882"/>
        <path d="${median}" stroke="#bce6ff" fill-opacity="0.0" stroke-with="3"/>
        <line x1="0" y1="220" y2="220" x2="360" stroke="yellow"/>
        <line x1="0" y1="320" y2="320" x2="360" stroke="red"/>
    </g>
</svg>
"""
