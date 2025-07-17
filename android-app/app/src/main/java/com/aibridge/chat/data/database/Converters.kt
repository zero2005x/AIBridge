package com.aibridge.chat.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.aibridge.chat.domain.model.PortalParameter
import com.aibridge.chat.domain.model.ParameterDefinition
import com.aibridge.chat.domain.model.PortalExample

class Converters {
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { Gson().toJson(it) }
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(it, listType)
        }
    }
    
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return value?.let { Gson().toJson(it) }
    }
    
    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        return value?.let {
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson(it, mapType)
        }
    }
    
    @TypeConverter
    fun fromPortalParameterMap(value: Map<String, PortalParameter>?): String? {
        return value?.let { Gson().toJson(it) }
    }
    
    @TypeConverter
    fun toPortalParameterMap(value: String?): Map<String, PortalParameter>? {
        return value?.let {
            val mapType = object : TypeToken<Map<String, PortalParameter>>() {}.type
            Gson().fromJson(it, mapType)
        }
    }
    
    @TypeConverter
    fun fromParameterDefinitionList(value: List<ParameterDefinition>?): String? {
        return value?.let { Gson().toJson(it) }
    }
    
    @TypeConverter
    fun toParameterDefinitionList(value: String?): List<ParameterDefinition>? {
        return value?.let {
            val listType = object : TypeToken<List<ParameterDefinition>>() {}.type
            Gson().fromJson(it, listType)
        }
    }
    
    @TypeConverter
    fun fromPortalExampleList(value: List<PortalExample>?): String? {
        return value?.let { Gson().toJson(it) }
    }
    
    @TypeConverter
    fun toPortalExampleList(value: String?): List<PortalExample>? {
        return value?.let {
            val listType = object : TypeToken<List<PortalExample>>() {}.type
            Gson().fromJson(it, listType)
        }
    }
}
