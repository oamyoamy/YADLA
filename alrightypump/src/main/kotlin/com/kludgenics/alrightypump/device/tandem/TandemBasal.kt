package com.kludgenics.alrightypump.device.tandem

import com.kludgenics.alrightypump.therapy.BasalRecord
import com.kludgenics.alrightypump.therapy.BasalSchedule
import com.kludgenics.alrightypump.therapy.ScheduledBasalRecord
import com.kludgenics.alrightypump.therapy.TemporaryBasalRecord
import org.joda.time.DateTimeZone
import org.joda.time.Duration

/**
 * Created by matthias on 11/25/15.
 */
interface TandemBasalRecord : BasalRecord, TandemTherapyRecord
class TandemBasalSchedule: BasalSchedule

data class TandemScheduledBasalRecord(val rateChange: BasalRateChange,
                                      override val schedule: BasalSchedule?):
        TandemBasalRecord,
        ScheduledBasalRecord,
        LogEvent by rateChange{
    override val rate: Double?
        get() = rateChange.rate
}

data class TandemTemporaryBasalRecord(val tempRateStart: TempRateStart?,
                                      val tempRateEnd: TempRateCompleted?,
                                      val basalRateChange: BasalRateChange?,
                                      val suspended: PumpingSuspended?,
                                      val resumed: PumpingResumed?,
                                      val logEvent: LogEvent): TandemBasalRecord,
        TemporaryBasalRecord,
        LogEvent by logEvent {
    constructor(tempRateStart: TempRateStart,
                tempRateEnd: TempRateCompleted?,
                basalRateChange: BasalRateChange?) : this(tempRateStart, tempRateEnd, basalRateChange, null,
            null, tempRateStart)
    constructor(tempRateEnd: TempRateCompleted,
                basalRateChange: BasalRateChange) : this(null, tempRateEnd, basalRateChange, null, null,
            tempRateEnd)
    constructor(pumpingSuspended: PumpingSuspended,
                pumpingResumed: PumpingResumed,
                basalRateChange: BasalRateChange?) : this(null, null, basalRateChange, pumpingSuspended,
            pumpingResumed, pumpingSuspended)

    override val rate: Double?
        get() = if (suspended != null) 0.0 else basalRateChange?.rate
    override val percent: Double?
        get() = if (suspended != null) 0.0 else tempRateStart?.percent
    override val duration: Duration
        get() =
            if (suspended != null && resumed != null)
                Duration(suspended.time.toDateTime(DateTimeZone.UTC), resumed.time.toDateTime(DateTimeZone.UTC))
            else if (tempRateStart != null && tempRateEnd != null)
                Duration(tempRateStart.time.toDateTime(DateTimeZone.UTC), tempRateEnd.time.toDateTime())
            else if (tempRateStart != null && tempRateEnd == null)
                tempRateStart.duration
            else if (tempRateStart != null && tempRateEnd != null)
                Duration(tempRateStart.time.toDateTime(DateTimeZone.UTC), tempRateEnd.time.toDateTime(DateTimeZone.UTC))
            else Duration.ZERO
}