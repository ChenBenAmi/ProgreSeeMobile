package com.example.progresee.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.progresee.beans.Classroom
import com.example.progresee.data.AppRepository
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Exception

class CreateClassroomViewModel(
    private val appRepository: AppRepository, classroomId: String?
) : BaseViewModel() {


    private var token: LiveData<String?>
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val classroom = MutableLiveData<Classroom>()
    fun getClassroom() = classroom

    private val _navigateBackToClassroomFragment = MutableLiveData<Long?>()
    val navigateBackToClassroomFragment: LiveData<Long?>
        get() = _navigateBackToClassroomFragment

    private val _stringLength = MutableLiveData<Int?>()
    val stringLength: LiveData<Int?>
        get() = _stringLength

    private val _descriptionStringLength = MutableLiveData<Int?>()
    val descriptionStringLength: LiveData<Int?>
        get() = _descriptionStringLength

    private val _showProgressBar = MutableLiveData<Boolean?>()
    val showProgressBar
        get() = _showProgressBar

    init {
        if (classroomId != "none" && classroomId != null) {
            setListeners(classroomId)
        }
        token = appRepository.currentToken
    }

    private fun setListeners(uid: String) {
        val db = appRepository.getFirestoreDB()
        val docRef = db.collection("classrooms")
            .document(uid)

        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Timber.wtf("Listen failed $e")
            }
            if (snapshot != null && snapshot.exists()) {
                Timber.wtf("Current data: ${snapshot.data}")

                val classroomFirestore =
                    snapshot.toObject(Classroom::class.java)
                Timber.wtf("classroom -> $classroomFirestore")
                classroomFirestore?.let {
                    Timber.wtf("formatted classroom is -> $it")
                    classroom.value = it

                }
            } else {
                Timber.wtf("Current data: null")
            }
        }
    }

    fun onSavePressed(name: String, description: String) {
        when {
            name.length > 32 -> _stringLength.value = 1
            name.isEmpty() -> _stringLength.value = 2
            description.length > 100 -> _descriptionStringLength.value = 1
            description.isEmpty() -> _descriptionStringLength.value = 2
            else -> uiScope.launch {
                showProgressBar()
                withContext(Dispatchers.IO) {
                    if (classroom.value == null) {
                        if (token.value != null) {
                            try {
                                val request =
                                    appRepository.createClassroomAsync(
                                        token.value!!,
                                        name,
                                        description
                                    ).await()
                                if (request.isSuccessful) {
                                    val data = request.body()
                                    Timber.wtf(data.toString())
                                    data?.forEach {
//                                        appRepository.insertClassroom(it.value)
                                    }

                                    withContext(Dispatchers.Main) {
                                        hideProgressBar()
                                        _navigateBackToClassroomFragment.value = 0
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.wtf("${e.message}${e.printStackTrace()}")
                            }
                        }
                    } else {
                        val classroom = getClassroom().value
                        Timber.wtf(classroom.toString())
                        if (classroom != null) {
                            try {
                                classroom.name = name
                                classroom.description = description
                                Timber.wtf("name is $name desc $description")
                                val request =
                                    appRepository.updateClassroomAsync(
                                        token.value!!,
                                        classroom.uid,
                                        classroom.name,
                                        classroom.description
                                    )
                                        .await()
                                if (request.isSuccessful) {
                                    val data = request.body()
                                    data?.forEach {
//                                        appRepository.updateClassroom(it.value)
                                    }


                                } else {
                                    Timber.wtf("${request.code()}${request.raw()}")
                                }
                            } catch (e: Exception) {
                                Timber.wtf("oh no something went wrong!${e.printStackTrace()}${e.message}")
                            } finally {
                                withContext(Dispatchers.Main) {
                                    hideProgressBar()
                                    _navigateBackToClassroomFragment.value = 0
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    override fun showProgressBar() {
        _showProgressBar.value = true
    }

    override fun hideProgressBar() {
        _showProgressBar.value = null
    }

    override fun snackBarShown() {
        _stringLength.value = null
        _descriptionStringLength.value = null
    }

    override fun onDoneNavigating() {
        _navigateBackToClassroomFragment.value = null
    }


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


}