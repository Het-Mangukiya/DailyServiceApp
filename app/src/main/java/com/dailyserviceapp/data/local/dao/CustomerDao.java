package com.dailyserviceapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.dailyserviceapp.data.local.entity.CustomerEntity;

import java.util.List;

/**
 * Data Access Object for local Customer queries.
 */
@Dao
public interface CustomerDao {
    
    @Query("SELECT * FROM customers WHERE providerId = :providerId ORDER BY name ASC")
    List<CustomerEntity> getCustomersByProvider(String providerId);

    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    CustomerEntity getCustomerById(String customerId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CustomerEntity> customers);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CustomerEntity customer);

    @Query("SELECT * FROM customers")
    List<CustomerEntity> getAllCustomers();

    @Query("DELETE FROM customers")
    void clearAll();
}
