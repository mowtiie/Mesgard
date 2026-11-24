package com.mowtiie.mesgard.domain.avatar

/** Turns a photo the person picked into a profile picture. Nothing leaves the phone from here. */
internal interface AvatarPhotos {
    /** An upright copy of the photo, shrunk and without metadata, to crop from. Null when it cannot be read. */
    suspend fun prepare(uri: String): PreparedPhoto?

    /** The path of a 512 pixel JPEG of [crop], small enough for the server. Null when it cannot be made. */
    suspend fun crop(photo: PreparedPhoto, crop: CropSquare): String?

    /** Removes the working files. */
    fun discard(photo: PreparedPhoto, croppedPath: String?)
}
