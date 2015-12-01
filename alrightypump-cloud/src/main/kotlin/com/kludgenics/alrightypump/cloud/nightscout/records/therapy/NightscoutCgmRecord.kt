package com.kludgenics.alrightypump.cloud.nightscout.records.therapy

import com.kludgenics.alrightypump.cloud.nightscout.NightscoutApiSgvEntry
import com.kludgenics.alrightypump.therapy.*

/**
 * Created by matthias on 12/1/15.
 */
class NightscoutCgmRecord(public override val rawEntry: NightscoutApiSgvEntry): CgmRecord, NightscoutRecord {
    override val value: GlucoseValue
        get() = NightscoutGlucoseValue(rawEntry)
}

class NightscoutGlucoseValue(public val sgv: NightscoutApiSgvEntry) : RawGlucoseValue {
    override val calibration: Calibration?
        get() = null
    override val filtered: Int?
        get() = sgv.filtered
    override val unfiltered: Int?
        get() = sgv.unfiltered
    override val glucose: Double?
        get() = sgv.sgv.toDouble()
    override val unit: Int
        get() = GlucoseUnit.MGDL
}