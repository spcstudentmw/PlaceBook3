package com.raywenderlich.placebook.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.raywenderlich.placebook.util.ImageUtils
import com.raywenderlich.placebook.model.Bookmark
import com.raywenderlich.placebook.repository.BookmarkRepo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BookmarkDetailsViewModel(application: Application) :
    AndroidViewModel(application) {
    private var bookmarkRepo: BookmarkRepo =
        BookmarkRepo(getApplication())

    private var bookmarkDetailsView: LiveData<BookmarkDetailsView>? = null

    private fun bookmarkToBookmarkView(bookmark: Bookmark):
            BookmarkDetailsView {
        return BookmarkDetailsView(
                bookmark.id,
                bookmark.name,
                bookmark.phone,
                bookmark.address,
                bookmark.notes,
                bookmark.category,
                bookmark.longitude,
                bookmark.latitude,
                bookmark.placeId
        )
    }

    fun mapBookmarkToBookmarkView(bookmarkId: Long) {
        val bookmark = bookmarkRepo.getLiveBookmark(bookmarkId)
        bookmarkDetailsView = Transformations.map(bookmark)
        { repoBookmark ->
            repoBookmark?.let { repoBookmark ->
                bookmarkToBookmarkView(repoBookmark)
            }
        }
    }



    fun getBookmark(bookmarkId: Long):
            LiveData<BookmarkDetailsView>? {
        if (bookmarkDetailsView == null) {
            mapBookmarkToBookmarkView(bookmarkId)
        }
        return bookmarkDetailsView
    }


    data class BookmarkDetailsView(var id: Long? = null,
                                   var name: String = "",
                                   var phone: String = "",
                                   var address: String = "",
                                   var notes: String = "",
                                   var category: String = "",
                                   var longitude: Double = 0.0,
                                   var latitude: Double = 0.0,
                                   var placeId: String? = null) {
        fun getImage(context: Context): Bitmap? {
            id?.let {
                return ImageUtils.loadBitmapFromFile(
                    context,
                    Bookmark.generateImageFilename(it)
                )
            }
            return null
        }

        fun setImage(context: Context, image: Bitmap) {
            id?.let {
                ImageUtils.saveBitmapToFile(context, image,
                        Bookmark.generateImageFilename(it))
            }
        }
    }

    private fun bookmarkViewToBookmark(bookmarkView:
                                       BookmarkDetailsView):
            Bookmark? {
        val bookmark = bookmarkView.id?.let {
            bookmarkRepo.getBookmark(it)
        }
        if (bookmark != null) {
            bookmark.id = bookmarkView.id
            bookmark.name = bookmarkView.name
            bookmark.phone = bookmarkView.phone
            bookmark.address = bookmarkView.address
            bookmark.notes = bookmarkView.notes
            bookmark.category = bookmarkView.category
        }
        return bookmark
    }

    fun updateBookmark(bookmarkView: BookmarkDetailsView) {
        // 1
        GlobalScope.launch {
            // 2
            val bookmark = bookmarkViewToBookmark(bookmarkView)
            // 3
            bookmark?.let { bookmarkRepo.updateBookmark(it) }
        }
    }

    // Return category source id from name
    fun getCategoryResourceId(category: String): Int? {
        return bookmarkRepo.getCategoryResourceId(category)
    }

    // To get categories from BookmarkRepo
    fun getCategories(): List<String> {
        return bookmarkRepo.categories
    }


    fun deleteBookmark(bookmarkDetailsView: BookmarkDetailsView) {
        GlobalScope.launch {
            val bookmark = bookmarkDetailsView.id?.let {
                bookmarkRepo.getBookmark(it)
            }
            bookmark?.let {
                bookmarkRepo.deleteBookmark(it)
            }
        }
    }


}
