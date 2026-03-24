package com.dailyserviceapp.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Customer(
    @DocumentId var id: String? = null,
    var name: String? = null,
    var phone: String? = null,
    var address: String? = null,
    var area: String? = null,
    var serviceType: String? = null,
    var ratePerUnit: Double = 0.0,
    var defaultQuantity: Double = 1.0,
    var lentAmount: Double = 0.0,
    var providerId: String? = null,
    var status: String? = "ACTIVE",
    var notes: String? = null,
    @get:JvmName("isOnVacation") @set:JvmName("setOnVacation") var onVacation: Boolean = false,
    var startDate: Timestamp? = null,
    var createdAt: Timestamp? = null
) {
    constructor(
        name: String?,
        phone: String?,
        address: String?,
        serviceType: String?,
        ratePerUnit: Double,
        createdAt: Timestamp?
    ) : this() {
        this.name = name
        this.phone = phone
        this.address = address
        this.serviceType = serviceType
        this.ratePerUnit = ratePerUnit
        this.createdAt = createdAt
        this.status = "ACTIVE"
        this.defaultQuantity = 1.0
        this.lentAmount = 0.0
    }
}
