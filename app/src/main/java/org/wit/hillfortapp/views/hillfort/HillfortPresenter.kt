package org.wit.hillfortapp.views.hillfort

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_hillfort.*
import org.jetbrains.anko.*
import org.wit.hillfortapp.MainApp
import org.wit.hillfortapp.R
import org.wit.hillfortapp.helpers.checkLocationPermissions
import org.wit.hillfortapp.helpers.showImagePicker
import org.wit.hillfortapp.models.HillfortModel
import org.wit.hillfortapp.models.Location
import org.wit.hillfortapp.models.Note
import org.wit.hillfortapp.views.editlocation.EditLocationView
import org.wit.hillfortapp.views.hillfortlist.HillfortListView

class HillfortPresenter(val view: HillfortView) : AnkoLogger {

    var app: MainApp = view.application as MainApp
    private var hillfort = HillfortModel()
    private var edit = false

    private val IMAGE_REQUEST = 1
    private val LOCATION_REQUEST = 2

    var locationService: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(view)
    private var location = Location()

    init {
        if (view.intent.hasExtra("hillfort_edit")) {
            edit = true
            hillfort = view.intent.extras?.getParcelable("hillfort_edit")!!
            view.showHillfort(hillfort)
        } else {
            if (checkLocationPermissions(view)) {
                doSetCurrentLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun doSetCurrentLocation() {
        locationService.lastLocation.addOnSuccessListener {
            hillfort.location.lat = it.latitude
            hillfort.location.lng = it.longitude
            view.showHillfort(hillfort)
        }
    }

    fun doClickNote(note: Note) {
        view.alert("${note.title}\n\n${note.content}").show()
    }

    fun doAddOrSave(tempHillfort: HillfortModel) {
        hillfort.name = tempHillfort.name
        hillfort.description = tempHillfort.description
        hillfort.visited = tempHillfort.visited
        hillfort.dateVisited = tempHillfort.dateVisited

        if (edit) {
            hillfort.notes = app.users.findOneUserHillfortNotes(app.activeUser, hillfort)!!
            app.users.updateHillfort(
                hillfort, app.activeUser
            )
        } else {
            app.users.createHillfort(hillfort, app.activeUser)
        }
        view.finish()
    }

    fun doCancel() {
        view.finish()
        view.startActivity(Intent(view, HillfortListView::class.java))
    }

    fun doDelete() {

        if (edit) {
            val builder = AlertDialog.Builder(view)
            builder.setMessage("Are you sure you want to delete this Hillfort?")
            builder.setPositiveButton("Yes") { dialog, _ ->
                app.users.deleteHillfort(hillfort, app.activeUser)
                dialog.dismiss()
                view.startActivity(Intent(view, HillfortListView::class.java))
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    // Credit: https://tutorial.eyehunts.com/android/android-date-picker-dialog-example-kotlin/
    @TargetApi(Build.VERSION_CODES.N)
    fun doDateDialog() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(
            view,
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                view.hillfortDateVisited.setText("$dayOfMonth/${monthOfYear + 1}/$year")
            },
            year, month, day
        )
        dpd.show()
    }

    fun doNext() {
        val index = app.activeUser.hillforts.indexOf(hillfort)
        try {
            view.startActivityForResult(
                view.intentFor<HillfortView>().putExtra(
                    "hillfort_edit",
                    app.activeUser.hillforts[index + 1]
                ), 0
            )
        } catch (e: IndexOutOfBoundsException) {
            view.toast("Next Hillfort is Empty!")
        }
    }

    fun doPrevious() {
        val index = app.activeUser.hillforts.indexOf(hillfort)
        try {
            view.startActivityForResult(
                view.intentFor<HillfortView>().putExtra(
                    "hillfort_edit",
                    app.activeUser.hillforts[index - 1]
                ), 0
            )
        } catch (e: IndexOutOfBoundsException) {
            view.toast("Previous Hillfort is Empty!")
        }
    }

    fun doSelectImage() {
        showImagePicker(view, IMAGE_REQUEST)
    }

    fun doSetLocation() {
        view.startActivityForResult(
            view.intentFor<EditLocationView>().putExtra("location", hillfort.location),
            LOCATION_REQUEST
        )
    }

    fun doNoteDialog() {
        if (!edit) {
            view.toast("Please create a hillfort before adding notes to it!")
        } else {

            val mDialogView = LayoutInflater.from(view).inflate(R.layout.dialog_note, null)
            val builder = AlertDialog.Builder(view)
            builder.setMessage("Enter note details: ")
            builder.setView(mDialogView)

            val dialog: AlertDialog = builder.create()
            dialog.show()

            val addBtn = dialog.findViewById(R.id.noteDialogAddBtn) as Button
            val cancelBtn = dialog.findViewById(R.id.noteDialogCancelBtn) as Button
            val noteTitle = dialog.findViewById(R.id.noteDialogTitle) as? EditText
            val noteContent = dialog.findViewById(R.id.noteDialogContent) as? EditText

            addBtn.setOnClickListener {

                if (listOf(noteTitle!!.text.toString(), noteContent!!.text.toString())
                        .contains("")
                ) {
                    view.toast("Please fill out all fields!")
                } else {
                    val newNote = Note()
                    newNote.title = noteTitle.text.toString()
                    newNote.content = noteContent.text.toString()
                    app.users.createNote(app.activeUser, hillfort, newNote)
                    view.showNotes(app.users.findOneUserHillfortNotes(app.activeUser, hillfort))
                    dialog.dismiss()
                }
            }

            cancelBtn.setOnClickListener {
                dialog.dismiss()
            }
        }
    }

    fun doActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            IMAGE_REQUEST -> {
                view.info("RECEIVED REQ")
                val builder = AlertDialog.Builder(view)
                builder.setMessage("This will reset the existing images, continue?")
                builder.setPositiveButton("YES") { dialog, _ ->
                    if (data != null) {
                        val imageArray = ArrayList<String>()

                        // if multiple images selected
                        if (data.clipData != null) {
                            if (data.clipData!!.itemCount > 4) {
                                view.toast("Exceeded maximum of 4 images")
                            } else {
                                val mClipData = data.clipData
                                var counter = 0
                                while (counter < mClipData!!.itemCount) {
                                    imageArray.add(mClipData.getItemAt(counter).uri.toString())
                                    counter++
                                }
                            }
                        } else {
                            imageArray.add(data.data.toString())
                        }
                        hillfort.images = imageArray
                        view.showImages(imageArray)
                        dialog.dismiss()
                    }
                }
                builder.setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }

                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
            LOCATION_REQUEST -> {
                if (data != null) {
                    location = data.extras?.getParcelable("location")!!
                    hillfort.location = location
                    val latLng = LatLng(hillfort.location.lat, hillfort.location.lng)
                    view.showUpdatedMap(latLng)
                }
            }
        }
    }
}
