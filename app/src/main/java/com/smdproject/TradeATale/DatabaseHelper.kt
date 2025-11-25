package com.smdproject.TradeATale

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "ShelfShareDB"
        const val DATABASE_VERSION = 1
        const val TABLE_BOOKS = "saved_books"
        const val COLUMN_ID = "id"
        const val COLUMN_BOOK_ID = "book_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_AUTHOR = "author"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_CATEGORIES = "categories"
        const val COLUMN_IMAGE = "image"
        const val COLUMN_OWNER_ID = "owner_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_BOOKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_BOOK_ID TEXT,
                $COLUMN_NAME TEXT,
                $COLUMN_AUTHOR TEXT,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_CATEGORIES TEXT,
                $COLUMN_IMAGE TEXT,
                $COLUMN_OWNER_ID TEXT
            )
        """
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKS")
        onCreate(db)
    }

    fun bookExists(bookId: String): Boolean {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_BOOKS WHERE $COLUMN_BOOK_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(bookId))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    fun saveBook(book: SavedBook) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_BOOK_ID, book.bookId)
            put(COLUMN_NAME, book.name)
            put(COLUMN_AUTHOR, book.author)
            put(COLUMN_DESCRIPTION, book.description)
            put(COLUMN_CATEGORIES, book.categories)
            put(COLUMN_IMAGE, book.image)
            put(COLUMN_OWNER_ID, book.ownerId)
        }
        db.insert(TABLE_BOOKS, null, values)
        db.close()
    }

    fun getAllSavedBooks(): List<SavedBook> {
        val books = mutableListOf<SavedBook>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_BOOKS"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val book = SavedBook(
                    bookId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOOK_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    author = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    categories = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORIES)),
                    image = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE)),
                    ownerId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OWNER_ID))
                )
                books.add(book)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return books
    }
}

data class SavedBook(
    val bookId: String,
    val name: String,
    val author: String,
    val description: String,
    val categories: String,
    val image: String?,
    val ownerId: String
)