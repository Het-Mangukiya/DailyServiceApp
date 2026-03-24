package com.dailyserviceapp.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ServiceEntry(
    @DocumentId var id: String? = null,
    var providerId: String? = null,
    var customerId: String? = null,
    var date: Timestamp? = null,
    var quantity: Double = 0.0,
    var rate: Double = 0.0,
    @get:JvmName("isDelivered") @set:JvmName("setDelivered") var delivered: Boolean = false,
    var notes: String? = null,
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
) {
    // Empty constructor is implicitly handled by the default arguments, but we need the secondary one
    
    constructor(
        providerId: String?,
        customerId: String?,
        date: Timestamp?,
        quantity: Double,
        delivered: Boolean
    ) : this() {
        this.providerId = providerId
        this.customerId = customerId
        this.date = date
        this.quantity = quantity
        this.delivered = delivered
        this.createdAt = Timestamp.now()
        this.updatedAt = Timestamp.now()
    }
}
