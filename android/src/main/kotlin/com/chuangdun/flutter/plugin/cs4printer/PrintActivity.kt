package com.chuangdun.flutter.plugin.cs4printer

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "PrintActivity"

private const val SUCCESS = 0
private const val CREATE_JOB_REQUEST = "com.lexmark.print.action.CREATE_JOB_REQUEST"
private const val PRINT_JOB_REQUEST = "com.lexmark.print.action.PRINT_JOB_REQUEST"
private const val CREATE_JOB_RESPONSE = "com.lexmark.print.action.CREATE_JOB_RESPONSE"
private const val PRINT_JOB_RESPONSE = "com.lexmark.print.action.PRINT_JOB_RESPONSE"
private const val MESSAGE_ID = "MESSAGE_ID"
private const val RESPONSE_TO = "RESPONSE_TO"
private const val STATUS = "STATUS"
private const val JOB_PATH = "JOB_PATH"
private const val JOB_ID = "JOB_ID"
private const val PRINTER_ADDRESS = "PRINTER_ADDRESS"
private const val PRINT_SERVICE_PACKAGE = "com.lexmark.mobile.printservice"
private const val PRINT_BROADCAST_RECEIVER = "com.lexmark.mobile.printservice.print.PrintBroadcastReceiver"

class PrintActivity : AppCompatActivity() {
    private var _broadcastReceiver: BroadcastReceiver? = null
    private var _uri: Uri? = null
    private lateinit var _title:String
    private lateinit var _printerAddress: String
    private lateinit var _currentMsgId: String

    /** If usb is used, true; else, false */
    private var isUsbUsed = false
    private lateinit var mLogTextView: TextView
    private lateinit var mTitleTextView: TextView
    private val log = StringBuilder()
    private val simpleDateFormat = SimpleDateFormat("HH:mm:ss")

    private fun setUsbUsed(usbUsed: Boolean) {
        isUsbUsed = usbUsed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print)
        mTitleTextView = findViewById(R.id.tv_title)
        mLogTextView = findViewById(R.id.progress_log)
        findViewById<View>(R.id.btn_close).setOnClickListener { finish() }
        val documentUri = intent.getStringExtra("documentUri")
        val bannerTitle = intent.getStringExtra("bannerTitle")
        Log.i(TAG, "??????????????????:$documentUri, UI??????:$bannerTitle")
        _broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                handleResponse(intent)
            }
        }
        val printFilter = IntentFilter()
        printFilter.addAction(CREATE_JOB_RESPONSE)
        printFilter.addAction(PRINT_JOB_RESPONSE)
        registerReceiver(_broadcastReceiver, printFilter)
        setUsbUsed(true)
        _printerAddress = if (isUsbUsed) {
            startUsbService()
            "127.0.0.1:60000"
        } else {
            "192.168.0.6"
        }
        if (documentUri == null || bannerTitle == null){
            Toast.makeText(this, "??????????????????.", Toast.LENGTH_LONG).show()
            finish()
            return
        }else{
            _title = bannerTitle
            mTitleTextView.text = _title
            _uri = getSampleUri(documentUri)
            Log.i(TAG, "????????????URI:${_uri}")
            if (_uri == null){
                Toast.makeText(this, "???????????????.", Toast.LENGTH_LONG).show()
                return
            }
            createJob()
        }
    }

    private fun getSampleUri(uri:String): Uri? {
        val sharedDir = createSharedDir() ?: return null
        val file = copyAssetToDir(uri, sharedDir) ?: return null
        val app = applicationContext
        return FileProvider.getUriForFile(app, app.packageName + ".provider", file)
    }

    private fun createSharedDir(): String? {
        val captureDir = "$filesDir/capture/"
        val captureDirFile = File(captureDir)
        if (!captureDirFile.exists()) {
            if (!captureDirFile.mkdirs()) {
                return null
            }
        }
        return captureDir
    }

    private fun copyAssetToDir(uri: String, dir: String): File? {
        return try {
            val origin = File(URI.create(uri))
            val `in` = FileInputStream(origin)
            Log.i(TAG, "????????????????????????:${origin.name}")
            val newFile = File(dir, origin.name)
            val out = FileOutputStream(newFile)
            // 4k buffer
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            out.flush()
            out.close()
            `in`.close()
            origin.delete()
            File(dir, origin.name)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(_broadcastReceiver)
    }

    private fun createJob() {
        // Send a CreateJobRequest Message.
        log.append("${simpleDateFormat.format(Date())} ??????????????????.").append("\r\n")
        mLogTextView.text = log.toString()
        val createJobRequest = createRequestMessage(CREATE_JOB_REQUEST)
        createJobRequest.putExtra(PRINTER_ADDRESS, _printerAddress)
        sendBroadcast(createJobRequest)
    }

    private fun verifyMsgId(intent: Intent): Boolean {
        val msgId = intent.getStringExtra(RESPONSE_TO)
        if (_currentMsgId != msgId) {
            return false
        }
        _currentMsgId = intent.getStringExtra(MESSAGE_ID)!!
        return true
    }

    private fun handleCreateJobResponse(intent: Intent) {
        if (!verifyMsgId(intent)) {
            return
        } // msg id does not match, bail out now!
        val status = intent.getIntExtra(STATUS, -1)
        val decodedStatus = decodeStatus(status)
        if (status != SUCCESS) {
            log.append("${simpleDateFormat.format(Date())} ??????????????????:").append(decodedStatus).append("\r\n")
            mLogTextView.text = log.toString()
            return
        }
        val jobId = intent.getStringExtra(JOB_ID)!!
        log.append("${simpleDateFormat.format(Date())} ????????????ID:").append(jobId).append("\r\n")
        mLogTextView.text = log.toString()
        printJob(jobId)
    }

    private fun printJob(jobId: String) {
        // Send a PrintJobRequest Message
        log.append("${simpleDateFormat.format(Date())} ??????????????????:").append(jobId).append("\r\n")
        mLogTextView.text = log.toString()
        val printJobRequest = createRequestMessage(PRINT_JOB_REQUEST)
        printJobRequest.putExtra(JOB_ID, jobId)
        printJobRequest.putExtra(PRINTER_ADDRESS, _printerAddress)
        printJobRequest.putExtra(JOB_PATH, _uri)
        grantUriPermission(
            PRINT_SERVICE_PACKAGE, _uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        sendBroadcast(printJobRequest)
    }

    private fun handlePrintJobResponse(intent: Intent) {
        if (!verifyMsgId(intent)) {
            return
        } // msg id does not match, bail out now!
        val status = intent.getIntExtra(STATUS, -1)
        val decodedStatus = decodeStatus(status)
        if (status != SUCCESS) {
            log.append("${simpleDateFormat.format(Date())} ?????????????????????").append(decodedStatus).append("\r\n")
            mLogTextView.text = log.toString()
            return
        }
        log.append("${simpleDateFormat.format(Date())} ?????????????????????").append(decodedStatus).append("\r\n")
        mLogTextView.text = log.toString()
        Log.d("MainActivity", "PrintJob successfully sent.")
    }

    private fun createRequestMessage(action: String): Intent {
        val requestMessage = Intent()
        val componentName = ComponentName(
            PRINT_SERVICE_PACKAGE,
            PRINT_BROADCAST_RECEIVER
        )
        requestMessage.component = componentName
        requestMessage.action = action
        _currentMsgId = UUID.randomUUID().toString()
        requestMessage.putExtra(
            MESSAGE_ID,
            _currentMsgId
        )
        return requestMessage
    }

    private fun handleResponse(intent: Intent) {
        val action = intent.action
        if (CREATE_JOB_RESPONSE == action) {
            handleCreateJobResponse(intent)
        } else if (PRINT_JOB_RESPONSE == action) {
            handlePrintJobResponse(intent)
        }
    }

    private fun decodeStatus(status:Int):String{
        return when(status){
            0 -> "????????????(0)"
            1 -> "????????????(1)"
            2 -> "???????????????(2)"
            3 -> "?????????????????????(3)"
            4 -> "?????????????????????(4)"
            else -> "????????????($status)"
        }
    }

    /**
     * Start up ippusb-bridge service
     */
    private fun startUsbService() {
        val uuid = UUID.randomUUID().toString()
        val infoComp = ComponentName(
            "com.lexmark.mobile.ippusbbridge",
            "com.lexmark.mobile.ippusbbridge.RootBroadcastReceiver"
        )
        val intent = Intent()
        intent.component = infoComp
        intent.action = "com.lexmark.ippusbbridge.service"
        intent.putExtra("MESSAGE_ID", uuid)
        sendBroadcast(intent)
    }
}