package com.kludgenics.alrightypump.device.dexcom

import com.kludgenics.alrightypump.ContinuousGlucoseMonitor
import com.kludgenics.alrightypump.DateTimeChangeRecord
import com.kludgenics.alrightypump.therapy.SmbgRecord
import okio.BufferedSink
import okio.BufferedSource
import org.joda.time.Chronology
import org.joda.time.Instant
import org.joda.time.chrono.ISOChronology
import kotlin.Iterator

/**
 * Created by matthias on 11/5/15.
 */

open class DexcomG4(private val source: BufferedSource,
                    private val sink: BufferedSink) : ContinuousGlucoseMonitor {

    companion object {
        public const val SOURCE = "alrightypump-DexCom-G4"
    }

    var rawEnabled = true

    private inner class DexcomCgmSequence(private val egvs: Sequence<EgvRecord>,
                                          private val sgvs: Sequence<SgvRecord>?,
                                          private val cals: Sequence<CalSetRecord>?) : Sequence<DexcomCgmRecord> {

        override fun iterator() = object : Iterator<DexcomCgmRecord> {
            var currentCal: CalSetRecord?
            val calIterator = cals?.iterator()
            val bgIterator: Iterator<Pair<EgvRecord, SgvRecord?>>

            init {
                println("init")
                val bgs: Sequence<Pair<EgvRecord, SgvRecord?>>
                if (sgvs != null && calIterator != null) {
                    val currentTime = Instant()
                    currentCal = calIterator.next()
                    while (currentCal!!.displayTime > currentTime && calIterator.hasNext())
                        currentCal = calIterator.next()
                    if (currentCal!!.displayTime > currentTime)
                        currentCal = null
                    bgs = egvs.filterNot { it.skipped }.zip(sgvs)
                } else {
                    currentCal = null
                    bgs = egvs.filterNot { it.skipped }.map { it to null }
                }
                bgIterator = bgs.iterator()
                println("init done")
            }

            override fun next(): DexcomCgmRecord {
                val bg = bgIterator.next()
                if (currentCal != null && bg.first.displayTime < currentCal!!.displayTime && calIterator != null) {
                    println("finding next")
                    while (bg.first.displayTime < currentCal!!.displayTime && calIterator.hasNext())
                        currentCal = calIterator.next()
                    if (bg.first.displayTime < currentCal!!.displayTime)
                        currentCal = null
                    println("found")
                }
                return DexcomCgmRecord(bg.first, bg.second, currentCal)
            }

            override fun hasNext(): Boolean = bgIterator.hasNext()
        }
    }

    override val cgmRecords: Sequence<DexcomCgmRecord>
        get() =
        if (rawEnabled) {
            val cals = calibrationRecords.asSequence()
            val egvs = egvRecords.asSequence()
            val sgvs = sgvRecords.asSequence()
            DexcomCgmSequence(egvs, sgvs, cals)
        } else
            DexcomCgmSequence(egvRecords.asSequence(), null, null)


    override val smbgRecords: Sequence<SmbgRecord>
        get() = meterRecords.asSequence()

    override val dateTimeChangeRecords: Sequence<DateTimeChangeRecord>
        get() = throw UnsupportedOperationException()

    override val chronology: Chronology
        get() = ISOChronology.getInstance() // this should actually be constructed from time change records

    override val outOfRangeHigh: Double
        get() = 401.0

    override val outOfRangeLow: Double
        get() = 39.0

    public val egvRecords: Iterator<EgvRecord> get() = DataPageIterator(RecordPage.EGV_DATA)
    public val sgvRecords: Iterator<SgvRecord> get() = DataPageIterator(RecordPage.SENSOR_DATA)
    public val eventRecords: Iterator<EventRecord> get() = DataPageIterator(RecordPage.USER_EVENT_DATA)
    public val settingsRecords: Iterator<UserSettingsRecord> get() = DataPageIterator(RecordPage.USER_SETTING_DATA)
    public val calibrationRecords: Iterator<CalSetRecord> get() = DataPageIterator(RecordPage.CAL_SET)
    public val meterRecords: Iterator<MeterRecord> get() = DataPageIterator(RecordPage.METER_DATA)
    public val insertionRecords: Iterator<InsertionRecord> get() = DataPageIterator(RecordPage.INSERTION_TIME)

    public val version: String? by lazy { requestVersion() }
    public val databasePartitionInfo: String? by lazy { readDatabasePartitionInfo() }

    private inner class DataPageIterator<T>(private val type: Int) : Iterator<T> {
        var pageIterator: Iterator<T>? = null
        var pageIndex: Int
        val start: Int
        val end: Int

        init {
            val (startIdx, endIdx) = readDataPageRange(type) ?: 0 to 0
            start = endIdx
            end = startIdx
            pageIndex = start
            updateIterator()
        }

        private fun updateIterator() {
            val records = @Suppress("UNCHECKED_CAST")(readDataPages(type, pageIndex).flatMap { it.records } as List<T>)
            pageIterator = records.reversed().iterator()
            pageIndex -= 1
        }

        override fun next(): T {
            if (pageIterator != null) {
                if (pageIterator!!.hasNext()) {
                    return pageIterator!!.next()
                } else {
                    updateIterator()
                    if (pageIterator != null)
                        return pageIterator!!.next()
                }
            }
            throw UnsupportedOperationException()
        }

        override fun hasNext(): Boolean {
            if (pageIterator?.hasNext() == true)
                return true
            else {
                if (pageIndex >= end)
                    updateIterator()
                else
                    pageIterator = null
                return pageIterator != null
            }
        }
    }

    public fun ping(): Boolean {
        val command = Ping()
        val response = commandResponse(command)
        return response is Ping
    }

    private fun readDatabasePartitionInfo(): String? {
        val command = ReadDatabasePartitionInfo()
        val response = commandResponse(command)
        return if (response is XmlDexcomResponse) {
            response.payloadString.utf8()
        } else null
    }

    private fun requestVersion(): String? {
        val command = ReadFirmwareHeader()
        val response = commandResponse(command)
        return if (response is XmlDexcomResponse)
            response.payloadString.utf8()
        else null
    }

    public fun readDataPages(recordType: Int, start: Int, count: Int = 1): List<RecordPage> {
        val response = commandResponse(ReadDataPages(recordType, start, count))
        return if (response is ReadDataPagesResponse)
            response.pages
        else
            emptyList()
    }

    public fun readDataPageRange(recordType: Int): Pair<Int, Int>? {
        val response = commandResponse(ReadDataPageRange(recordType))
        return if (response is ReadDataPageRangeResponse)
            response.start to response.end
        else null
    }

    public fun commandResponse(command: DexcomCommand): ResponsePayload {
        val packet = DexcomG4Request(command.command, command).frame
        sink.write(packet, packet.size())
        val response = DexcomG4Response(source)
        val payload = DexcomG4Response.parsePayload(command.command, response.command, response.payload)
        return payload
    }
}