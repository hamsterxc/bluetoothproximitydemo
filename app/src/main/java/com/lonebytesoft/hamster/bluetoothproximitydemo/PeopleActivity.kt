package com.lonebytesoft.hamster.bluetoothproximitydemo

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.facebook.*
import kotlinx.android.synthetic.main.activity_people.*
import kotlinx.android.synthetic.main.content_people.*

class PeopleActivity : AppCompatActivity() {

    private companion object {
        private const val REFRESH_INTERVAL = 60000L
        private const val BLUETOOTH_REQUEST_CODE = 1
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothBroadcastReceiver: BluetoothBroadcastReceiver

    private val handler = Handler()
    private var refreshTask: Runnable = Runnable {}

    private val ids = mutableListOf<String>()
    private val graphResponses = mutableMapOf<String, MutableMap<FacebookRequestType, GraphResponse>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_people)
        setSupportActionBar(toolbar)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (!bluetoothAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, BLUETOOTH_REQUEST_CODE)
        }

        refreshTask = Runnable {
            try {
                update()
            } finally {
                handler.removeCallbacks(refreshTask)
                handler.postDelayed(refreshTask, REFRESH_INTERVAL)
            }
        }
        refreshTask.run()

        viewManager = LinearLayoutManager(this)
        viewAdapter = PeopleAdapter(emptyArray(), this)
        recyclerView = findViewById<RecyclerView>(R.id.list_people).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    private fun update() {
        if (!bluetoothAdapter.isEnabled) {
            return
        }

        setBluetoothName()

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()

        bluetoothBroadcastReceiver = BluetoothBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothDevice.ACTION_CLASS_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(bluetoothBroadcastReceiver, intentFilter)
    }

    private fun setBluetoothName() {
        Profile.fetchProfileForCurrentAccessToken()
        val facebookId = Profile.getCurrentProfile().id
        bluetoothAdapter.name = generateNameFromId(facebookId)
    }

    private fun updateList() {
        graphResponses.clear()
        val accessToken = AccessToken.getCurrentAccessToken()
        val requestBatch = GraphRequestBatch(
            ids.toSet()
                .flatMap { listOf(
                    GraphRequest(
                        accessToken,
                        "/$it/",
                        null,
                        HttpMethod.GET,
                        { response -> graphResponses.getOrPut(it) { mutableMapOf() }.put(FacebookRequestType.PROFILE, response) }
                    ),
                    GraphRequest(
                        accessToken,
                        "/$it/picture?redirect=false&type=large",
                        null,
                        HttpMethod.GET,
                        { response -> graphResponses.getOrPut(it) { mutableMapOf() }.put(FacebookRequestType.PICTURE, response) }
                    )
                ) }
        )

        requestBatch.addCallback {
            list_people.removeAllViews()
            val adapter = PeopleAdapter(
                graphResponses
                    .map { it.value }
                    .filter { it.size == 2 }
                    .filter { it.all { it.value.jsonObject != null } }
                    .map { PersonInformation(
                        it[FacebookRequestType.PROFILE]?.jsonObject?.getString("id") ?: "",
                        it[FacebookRequestType.PROFILE]?.jsonObject?.getString("name") ?: "",
                        it[FacebookRequestType.PICTURE]?.jsonObject?.getJSONObject("data")?.getString("url") ?: ""
                    ) }
                    .filter { it.id.isNotBlank() && it.name.isNotBlank() && it.picture.isNotBlank() }
                    .toSet()
                    .toTypedArray(),
                this
            )
            recyclerView.swapAdapter(adapter, true)
        }

        requestBatch.executeAsync()
    }

    private inner class BluetoothBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Snackbar
                        .make(this@PeopleActivity.people_layout, "Discovery started", Snackbar.LENGTH_SHORT)
                        .show()
                    Log.d("bluetoothproximitydemo", "Discovery started")

                    ids.clear()
                }

                BluetoothDevice.ACTION_FOUND, BluetoothDevice.ACTION_CLASS_CHANGED, BluetoothDevice.ACTION_NAME_CHANGED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    Log.d("bluetoothproximitydemo", "Device found: ${device.name} ($device)")
                    device.name?.let { extractIdFromName(it)?.let { ids.add(it) } }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Snackbar
                        .make(this@PeopleActivity.people_layout, "Discovery finished", Snackbar.LENGTH_SHORT)
                        .show()
                    Log.d("bluetoothproximitydemo", "Discovery finished")

                    updateList()
                }
            }
            Log.v("bluetoothproximitydemo", intent?.action ?: "null intent")
        }
    }

    private enum class FacebookRequestType {
        PROFILE,
        PICTURE,
    }

}
