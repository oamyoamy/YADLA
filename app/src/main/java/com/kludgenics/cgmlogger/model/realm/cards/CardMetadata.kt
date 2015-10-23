package com.kludgenics.cgmlogger.model.realm.cards

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

/**
 * Created by matthias on 10/14/15.
 */
@RealmClass
public open class CardMetadata: RealmObject() {
    @PrimaryKey
    public open var id: Long = 0
    public open var lastUpdated: Date? = null
    public open var cardtType: Int = -1
}