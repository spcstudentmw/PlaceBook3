package com.raywenderlich.placebook.viewmodel


import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.raywenderlich.placebook.util.ImageUtils
import com.raywenderlich.placebook.repository.BookmarkRepo
import com.raywenderlich.placebook.model.Bookmark

// 1
class MapsViewModel(application: Application) :
    AndroidViewModel(application) {
    private var bookmarks: LiveData<List<BookmarkView>>? = null  // Property to hold bookmarks

    private val TAG = "MapsViewModel"

    // 2
    private var bookmarkRepo: BookmarkRepo = BookmarkRepo(
        getApplication()
    )

    // 3
    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {
        // 4
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.placeId = place.id
        bookmark.name = place.name.toString()
        bookmark.longitude = place.latLng?.longitude ?: 0.0
        bookmark.latitude = place.latLng?.latitude ?: 0.0
        bookmark.phone = place.phoneNumber.toString()
        bookmark.address = place.address.toString()

        bookmark.category = getPlaceCategory(place) // Assigns category to new bookmark

        val newId = bookmarkRepo.addBookmark(bookmark)
        image?.let { bookmark.setImage(it, getApplication()) }

        Log.i(TAG, "New bookmark $newId added to the database.")
    }

    private fun mapBookmarksToMarkerView() {
        // 1
        bookmarks = Transformations.map(bookmarkRepo.allBookmarks)
        { repoBookmarks ->
            // 2
            repoBookmarks.map { bookmark ->
                bookmarkToMarkerView(bookmark)
            }
        }
    }

    fun getBookmarkMarkerViews():
            LiveData<List<BookmarkView>>? {
        if (bookmarks == null) {
            mapBookmarksToMarkerView()
        }
        return bookmarks
    }

    private fun bookmarkToMarkerView(bookmark: Bookmark):
            MapsViewModel.BookmarkView {
        return MapsViewModel.BookmarkView(
            bookmark.id,
            LatLng(bookmark.latitude, bookmark.longitude),
            bookmark.name,
            bookmark.phone)
    }

    data class BookmarkView(val id: Long? = null,
                            val location: LatLng = LatLng(0.0, 0.0),
                            val name: String = "",
                            val phone: String = "",
                            val categoryResourceId: Int? = null) {
        fun getImage(context: Context): Bitmap? {
            id?.let {
                return ImageUtils.loadBitmapFromFile(
                    context,
                    Bookmark.generateImageFilename(it)
                )
            }
            return null
        }
    }

    // Converts place type to a bookmark category
    private fun getPlaceCategory(place: Place): String {
        // 1
        var category = "Other"
        val placeTypes = place.types
        placeTypes?.let { placeTypes ->
            // 2
            if (placeTypes.size > 0) {
                // 3
                val placeType = placeTypes[0]
                category = bookmarkRepo.placeTypeToCategory(placeType)
            }
        }
        // 4
        return category
    }

    private fun bookmarkToBookmarkView(bookmark: Bookmark):
            MapsViewModel.BookmarkView {
        return MapsViewModel.BookmarkView(
                bookmark.id,
                LatLng(bookmark.latitude, bookmark.longitude),
                bookmark.name,
                bookmark.phone,
                bookmarkRepo.getCategoryResourceId(bookmark.category))
    }

    fun addBookmark(latLng: LatLng) : Long? {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.name = "Untitled"
        bookmark.longitude = latLng.longitude
        bookmark.latitude = latLng.latitude
        bookmark.category = "Other"
        return bookmarkRepo.addBookmark(bookmark)
    }

}
