package com.akhilasdeveloper.marsroverphotos.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.akhilasdeveloper.marsroverphotos.utilities.Constants
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.ERROR_NO_INTERNET
import com.akhilasdeveloper.marsroverphotos.utilities.Utilities
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.*
import com.akhilasdeveloper.marsroverphotos.db.dao.MarsPhotoDao
import com.akhilasdeveloper.marsroverphotos.db.dao.MarsRoverDao
import com.akhilasdeveloper.marsroverphotos.db.dao.RemoteKeyDao
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoLikedTable
import com.akhilasdeveloper.marsroverphotos.db.table.rover.MarsRoverManifestTable
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.db.table.rover.MarsRoverSrcTable
import com.akhilasdeveloper.marsroverphotos.paging.MarsPagingSource
import com.akhilasdeveloper.marsroverphotos.repositories.responses.MarsRoverSrcResponse
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.ERROR_NETWORK_TIMEOUT
import com.akhilasdeveloper.marsroverphotos.utilities.formatDateToMillis
import com.akhilasdeveloper.marsroverphotos.utilities.showShortToast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

class MarsRoverPhotosRepository @Inject constructor(
    private val marsRoverPhotosService: MarsRoverPhotosService,
    private val marsRoverDao: MarsRoverDao,
    private val marsPhotoDao: MarsPhotoDao,
    private val remoteKeyDao: RemoteKeyDao,
    private val database: FirebaseDatabase,
    private val firebaseInstallations: FirebaseInstallations,
    private val utilities: Utilities
) {

    /**
     * Rover Manifest START
     */

    private suspend fun getRoverManifest(
        data: List<MarsRoverSrcTable>,
        isCache: Boolean
    ): List<RoverMaster> {
        val response = mutableListOf<RoverMaster>()
        data.forEach { src ->
            withContext(Dispatchers.IO) {
                getRoverManifestData(src.roverName, isCache)?.let { manifest ->
                    response.add(
                        RoverMaster(
                            launch_date = manifest.launch_date,
                            launch_date_in_millis = manifest.launch_date.formatDateToMillis()!!,
                            name = src.roverName,
                            total_photos = manifest.total_photos,
                            status = manifest.status,
                            max_sol = manifest.max_sol,
                            max_date = manifest.max_date,
                            max_date_in_millis = manifest.max_date.formatDateToMillis()!!,
                            landing_date = manifest.landing_date,
                            landing_date_in_millis = manifest.landing_date.formatDateToMillis()!!,
                            description = src.roverDescription,
                            image = src.roverImage,
                            id = src.id
                        )
                    )
                }
            }
        }
        return response
    }

    private suspend fun getRoverManifestData(
        roverName: String,
        isCache: Boolean
    ): MarsRoverManifestTable? {

        if (utilities.isConnectedToTheInternet() && !isCache) {
            withContext(Dispatchers.IO) {
                refreshRoverManifest(roverName)
            }
        }

        return marsRoverDao.getMarsRoverManifest(roverName)
    }

    private suspend fun insertRoverManifest(marsRoverManifestTable: MarsRoverManifestTable) {
        marsRoverDao.insertMarsRoverManifestDb(marsRoverManifestTable)
    }

    private suspend fun refreshRoverManifest(roverName: String) {

        val response =
            marsRoverPhotosService.getRoverManifest(Constants.URL_MANIFEST + roverName + "/")

        response?.photo_manifest?.let { lis ->
            insertRoverManifest(
                MarsRoverManifestTable(
                    landing_date = lis.landing_date,
                    launch_date = lis.launch_date,
                    max_date = lis.max_date,
                    max_sol = lis.max_sol,
                    status = lis.status,
                    total_photos = lis.total_photos,
                    name = lis.name
                )
            )
        }
    }

    /**
     * Rover Manifest END
     */

    /**
     * Rover Src START
     */

    suspend fun getRoverData(isRefresh: Boolean): Flow<MarsRoverSrcResponse> {

        return flow {
            emit(MarsRoverSrcResponse(isLoading = true))

            var dataSrc = marsRoverDao.getMarsRoverSrc()
            val isEmpty = dataSrc.isEmpty()

            if (!isEmpty)
                emit(MarsRoverSrcResponse(data = getRoverManifest(dataSrc, true)))

            val insertedDate = marsRoverDao.getInsertDate()
            val isExpired =
                if (insertedDate == null) true else (System.currentTimeMillis() - insertedDate) > Constants.MILLIS_IN_A_DAY

            if (isExpired || isRefresh)
                emit(MarsRoverSrcResponse(message = "Syncing Database"))

            if (isExpired || isRefresh || isEmpty) {
                if (utilities.isConnectedToTheInternet()) {
                    emit(MarsRoverSrcResponse(isLoading = true))
                    val networkJob = withTimeoutOrNull(Constants.NETWORK_TIME_OUT) {
                        try {
                            refreshRoverSrcDb()
                        } catch (exception: Exception) {
                            emit(MarsRoverSrcResponse(error = ERROR_NETWORK_TIMEOUT))
                            Timber.e("refreshRoverSrcDb() : $exception")
                            return@withTimeoutOrNull
                        }
                    }
                    if (networkJob == null) {
                        emit(MarsRoverSrcResponse(error = ERROR_NETWORK_TIMEOUT))
                        return@flow
                    }
                } else {
                    emit(MarsRoverSrcResponse(error = ERROR_NO_INTERNET))
                    return@flow
                }

                dataSrc = marsRoverDao.getMarsRoverSrc()
                val networkJob = withTimeoutOrNull(Constants.NETWORK_TIME_OUT) {
                    try {
                        emit(MarsRoverSrcResponse(data = getRoverManifest(dataSrc, false)))
                    } catch (exception: Exception) {
                        emit(MarsRoverSrcResponse(error = ERROR_NETWORK_TIMEOUT))
                        Timber.e("getRoverManifest : $exception")
                        return@withTimeoutOrNull
                    }
                }
                if (networkJob == null) {
                    emit(MarsRoverSrcResponse(error = ERROR_NETWORK_TIMEOUT))
                }
            }

        }.flowOn(Dispatchers.IO)
    }

    private suspend fun insertMarsRoverSrc(marsRoverSrcTable: MarsRoverSrcTable) {
        marsRoverDao.insertMarsRoverSrc(marsRoverSrcTable)
    }

    suspend fun updateLike(marsRoverPhotoTable: MarsRoverPhotoTable) {
        withContext(Dispatchers.IO) {
            marsRoverPhotoTable.let {
                if (checkLike(it.photo_id))
                    removeLike(marsRoverPhotoTable)
                else
                    addLike(marsRoverPhotoTable)
            }
        }
    }

    private suspend fun addLike(marsRoverPhotoTable: MarsRoverPhotoTable) {
        withContext(Dispatchers.IO) {
            marsPhotoDao.addLike(
                marsRoverPhotoLikedTable = MarsRoverPhotoLikedTable(
                    id = marsRoverPhotoTable.photo_id,
                    rover_id = marsRoverPhotoTable.rover_id
                )
            )
            submitLike(marsRoverPhotoTable)
        }
    }

    private suspend fun removeLike(marsRoverPhotoTable: MarsRoverPhotoTable) {
        withContext(Dispatchers.IO) {
            marsPhotoDao.removeLike(
                marsRoverPhotoLikedTable = MarsRoverPhotoLikedTable(
                    id = marsRoverPhotoTable.photo_id,
                    rover_id = marsRoverPhotoTable.rover_id
                )
            )
        }
    }

    suspend fun isLiked(id: Long) = flow<Boolean> { emit(checkLike(id)) }

    private suspend fun checkLike(id: Long) =
        withContext(Dispatchers.IO) { marsPhotoDao.isLiked(id) > 0 }

    private suspend fun refreshRoverSrcDb() {

        val response = marsRoverPhotosService.getRoverData()

        response?.roverSrc?.let { lis ->
            lis.forEach {
                insertMarsRoverSrc(
                    MarsRoverSrcTable(
                        roverDescription = it.description,
                        roverImage = it.image,
                        roverName = it.name,
                        addedDate = System.currentTimeMillis(),
                        id = it.id
                    )
                )
            }
        }
    }

    /**
     * Rover Src END
     */

    /**
     * Rover Photo START
     */

    fun getPhotos(
        rover: RoverMaster,
        date: Long
    ): Flow<PagingData<MarsRoverPhotoTable>> {
        return Pager(
            config = PagingConfig(
                pageSize = Constants.MARS_ROVER_PHOTOS_PAGE_SIZE
            ),
            pagingSourceFactory = {

                MarsPagingSource(
                    remoteKeyDao = remoteKeyDao,
                    roverMaster = rover,
                    marsPhotoDao = marsPhotoDao,
                    marsRoverPhotosService = marsRoverPhotosService,
                    date = date
                )
            }
        ).flow
    }

    fun getLikedPhotos(
        rover: RoverMaster
    ): Flow<PagingData<MarsRoverPhotoTable>> {
        return Pager(
            config = PagingConfig(
                pageSize = Constants.MARS_ROVER_PHOTOS_PAGE_SIZE
            ),
            pagingSourceFactory = {
                marsPhotoDao.getSavedPhotos(roverID = rover.id)
            }
        ).flow
    }

    /**
     * Rover Photo END
     */

    /**
     * Firebase
     */

    private fun submitLike(marsRoverPhotoTable: MarsRoverPhotoTable) {
        val myRef = database.reference
        firebaseInstallations.id.addOnCompleteListener { id ->
            myRef.child(Constants.FIREBASE_NODE_USER_IDS)
                .child(id.result.toString())
                .child(id.result.toString() + "_" + marsRoverPhotoTable.photo_id.toString())
                .setValue(marsRoverPhotoTable.photo_id.toString() + "_" + marsRoverPhotoTable.rover_id)
            myRef.child(Constants.FIREBASE_NODE_PHOTO_IDS)
                .child(marsRoverPhotoTable.photo_id.toString())
                .child(marsRoverPhotoTable.photo_id.toString() + "_" + id.result.toString())
                .setValue(id.result.toString())
            myRef.child(Constants.FIREBASE_NODE_PHOTOS)
                .child(marsRoverPhotoTable.photo_id.toString() + "_" + marsRoverPhotoTable.rover_id)
                .setValue(marsRoverPhotoTable)
        }
    }

    fun getLikedPhotos(): Flow<List<MarsRoverPhotoTable>> {
        val myRef = database.reference
        val photoList = arrayListOf<MarsRoverPhotoTable>()
        val photoLiveData: MutableLiveData<List<MarsRoverPhotoTable>> = MutableLiveData()
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                myRef.removeEventListener(this)
                firebaseInstallations.id.addOnCompleteListener { id ->
                    for (data in dataSnapshot.child(Constants.FIREBASE_NODE_USER_IDS)
                        .child(id.result.toString()).children) {
                        data.getValue<String>()?.let { value ->
                            dataSnapshot.child(Constants.FIREBASE_NODE_PHOTOS).child(value).let {
                                it.getValue(MarsRoverPhotoTable::class.java)?.let { photo ->
                                    photoList.add(photo)
                                }
                            }
                        }
                    }
                    photoLiveData.value = photoList
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        return photoLiveData.asFlow()
    }

    fun syncLikedPhotos() {
        val myRef = database.reference
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                myRef.removeEventListener(this)
                firebaseInstallations.id.addOnCompleteListener { id ->
                    for (data in dataSnapshot.child(Constants.FIREBASE_NODE_USER_IDS)
                        .child(id.result.toString()).children) {
                        data.getValue<String>()?.let { value ->
                            dataSnapshot.child(Constants.FIREBASE_NODE_PHOTOS).child(value).let {
                                it.getValue(MarsRoverPhotoTable::class.java)?.let { photo ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        addLike(photo)
                                        marsPhotoDao.insertMarsRoverPhoto(photo)
                                    }
                                }
                            }
                        }
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        utilities.setLikesSync()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}