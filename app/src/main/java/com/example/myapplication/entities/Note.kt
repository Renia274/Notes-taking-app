package com.example.myapplication.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "notes")
class Note : Serializable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int? = null

    @ColumnInfo(name = "title")
    var title: String? = null

    @ColumnInfo(name = "date_time")
    var dateTime: String? = null

    @ColumnInfo(name = "subtitle")
    var subtitle: String? = null

    @ColumnInfo(name = "note_text")
    var noteText: String? = null

    @ColumnInfo(name = "image_path")
    var imagePath: String? = null

    @ColumnInfo(name = "color")
    var color: String? = null

    @ColumnInfo(name = "web_link")
    var webLink: String? = null


    override fun toString(): String {
        return "$title:$dateTime"
    }
}