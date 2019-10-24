package org.wit.hillfortapp.activities

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_stats.*
import org.jetbrains.anko.AnkoLogger
import org.wit.hillfortapp.MainApp
import org.wit.hillfortapp.R
import org.wit.hillfortapp.models.HillfortModel

class StatsActivity : MainActivity(), AnkoLogger {

    lateinit var app: MainApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_stats, content_frame)
        app = application as MainApp

        // set stats
        stats_users_number.text = getUsers().toString()
        stats_hillfort_number.text = getHillforts().toString()
        stats_images_number.text = getImages().toString()
        stats_visited_number.text = getVisits().toString()
        stats_top_user.text = getMostActiveUser()
    }

    private fun getUsers(): Int? {
        return app.users.findAll().size
    }

    private fun getHillforts(): Int? {
        return app.users.findAllHillforts()?.size
    }

    private fun getImages(): Int {
        val hillforts: ArrayList<HillfortModel>? = app.users.findAllHillforts()
        var totalImages = 0
        if (hillforts != null) {
            for (hillfort in hillforts) {
                totalImages += hillfort.images.size
            }
        }
        return totalImages
    }

    private fun getVisits(): Int {
        val hillforts: ArrayList<HillfortModel>? = app.users.findAllHillforts()
        var totalVisits = 0
        if (hillforts != null) {
            for (hillfort in hillforts) {
                if (hillfort.visited) {
                    totalVisits++
                }
            }
        }
        return totalVisits
    }

    private fun getMostActiveUser(): String {
        val users = app.users.findAll()
        var hillfortsCount = 0
        var topUser = ""

        for (user in users) {
            if (user.hillforts.size > hillfortsCount) {
                hillfortsCount = user.hillforts.size
                topUser = user.email
            }
        }
        return topUser
    }
}