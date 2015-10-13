package com.kludgenics.cgmlogger.app

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import com.kludgenics.cgmlogger.app.adapter.AgpAdapter
import com.kludgenics.cgmlogger.app.service.LocationIntentService
import com.kludgenics.cgmlogger.app.service.TaskService
import org.jetbrains.anko.*
import org.joda.time.Period

public class MainActivity : BaseActivity(), AnkoLogger {
    override protected val navigationId = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupNavigationBar()
        setupActionBar()

        val fab = find<FloatingActionButton>(R.id.fab)
        fab.onClick {
        }
        //startService(intentFor<LocationIntentService>().setAction(LocationIntentService.ACTION_START_LOCATION_UPDATES))
        /// / Set up the drawer.

        val recycler = find<RecyclerView>(R.id.recycler)
        recycler.adapter = AgpAdapter(listOf(1, 3, 7, 14, 30, 60, 90).map { Period.days(it) })
        //recycler.setAdapter(AgpAdapter((1 .. 90).map{Period.days(it)}))
        recycler.layoutManager = LinearLayoutManager(ctx)
    }



    override fun onStart() {
        super.onStart()
        TaskService.syncNow(this)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        /*if (!mNavigationDrawerFragment!!.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu)
            restoreActionBar()
            return true
        }*/
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item!!.itemId

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

}
