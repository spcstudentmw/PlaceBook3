package com.raywenderlich.placebook.repository

import com.raywenderlich.placebook.db.PlaceBookDatabase
import com.raywenderlich.placebook.model.Bookmark
import android.content.Context
import androidx.lifecycle.LiveData
import com.raywenderlich.placebook.db.BookmarkDao


//  Pass context object to constructor for PlaceBookDatabase instance
class BookmarkRepo(context: Context) {
    // 2
    private var db = PlaceBookDatabase.getInstance(context)
    private var bookmarkDao: BookmarkDao = db.bookmarkDao()
    // Logic for adding bookmark to the repo
    fun addBookmark(bookmark: Bookmark): Long? {
        val newId = bookmarkDao.insertBookmark(bookmark)
        bookmark.id = newId
        return newId
    }
    // Create new initialized bookmark object
    fun createBookmark(): Bookmark {
        return Bookmark()
    }
    // Returns LiveData list of all bookmarks in repo
    val allBookmarks: LiveData<List<Bookmark>>
        get() {
            return bookmarkDao.loadAll()
        }
}