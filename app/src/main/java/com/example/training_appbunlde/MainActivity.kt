package com.example.training_appbunlde

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION

class MainActivity : AppCompatActivity() {

    private val splitInstallManager by lazy { SplitInstallManagerFactory.create(this) }

    private lateinit var request: SplitInstallRequest

    private var sessionId = 0

    lateinit var mDownLoadF1Btn: Button

    lateinit var mDownLoadF2Btn: Button

    lateinit var mMoveBtn: Button
    lateinit var mMove1Btn: Button

    private val CLASS_NAME_MODULE_1 = "com.example.dynamic_feature.DynamiFeatureActivity"
    private val CLASS_NAME_MODULE_2 = "com.example.dynamic_feature2.ChatActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mDownLoadF1Btn = findViewById(R.id.main_down_f1_btn)
        mDownLoadF2Btn = findViewById(R.id.main_down_f2_btn)
        mMoveBtn = findViewById(R.id.button)
        mMove1Btn = findViewById(R.id.button1)

        mDownLoadF1Btn.setOnClickListener { v ->
            downLoadDynamicModule("dynamic_feature")
        }

        mDownLoadF2Btn.setOnClickListener { v ->
            downLoadDynamicModule("dynamic_feature2")
        }

        mMoveBtn.setOnClickListener { v ->
            onMove(CLASS_NAME_MODULE_1)
        }

        mMove1Btn.setOnClickListener { v ->
            onMove(CLASS_NAME_MODULE_2)
        }
    }

    private fun onMove(name:String) {
        val intent = Intent(this, Class.forName(name))
        startActivity(intent)
    }

    private fun downLoadDynamicModule(moduleName: String) {

        request = SplitInstallRequest.newBuilder()
            .addModule(moduleName)
            .build()

        splitInstallManager.startInstall(request)
            .addOnSuccessListener { sessionId ->
                this.sessionId = sessionId
                Log.d("MainActivity", "success with sessionId = ${sessionId}")
            }
            .addOnFailureListener { exception ->
                Log.d("MainActivity", "exception with exception = ${exception}")

                when ((exception as SplitInstallException).errorCode) {
                    SplitInstallErrorCode.NETWORK_ERROR -> showMessage(resources.getString(R.string.network_error))
                    SplitInstallErrorCode.MODULE_UNAVAILABLE -> showMessage(resources.getString(R.string.module_unavailable))
                    SplitInstallErrorCode.INVALID_REQUEST -> showMessage(resources.getString(R.string.invalid_request))
//                    SplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED -> checkForActiveDownloads()
                }
            }

        listenRequest()
    }

    /*
   * Listen request and handle state updates
   */
    private fun listenRequest() {
        /*Listen request status updates*/

        this.splitInstallManager.registerListener { state ->
            if (state.errorCode() == SplitInstallErrorCode.SERVICE_DIED) {
                // Retry the request
                return@registerListener
            }

            if (state.sessionId() == this.sessionId) {
                when (state.status()) {
                    SplitInstallSessionStatus.PENDING -> {
                        // The request has been accepted and the download should start soon.
                    }
                    SplitInstallSessionStatus.DOWNLOADING -> {
                        showMessage(resources.getString(R.string.downloading))
                        // update progressBar
                        val totalBytes = state.totalBytesToDownload()
                        val progress = state.bytesDownloaded()
                    }
                    SplitInstallSessionStatus.DOWNLOADED -> showMessage(resources.getString(R.string.downloaded))
                    SplitInstallSessionStatus.INSTALLED -> {
                        // After installed, you can start accessing it. Fire an Intent
                        showMessage(resources.getString(R.string.installed))
                    }
                    SplitInstallSessionStatus.INSTALLING -> showMessage(resources.getString(R.string.installing))
                    SplitInstallSessionStatus.CANCELING -> showMessage(resources.getString(R.string.canceling))
                    SplitInstallSessionStatus.CANCELED -> showMessage(resources.getString(R.string.installed))
                    SplitInstallSessionStatus.FAILED -> {
                        // Retry the request
                        showMessage(resources.getString(R.string.failed))

                    }
                    SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                        /*
                       * Displays a dialog for user "Download" or "Cancel" >10MB
                       * Params:
                       *   + Download -> request status changes to:  PENDING
                       *   + Cancel -> CANCELED
                       *   + Do not choose -> requestCode default
                       */
                        splitInstallManager.startConfirmationDialogForResult(state, this, REQUIRES_USER_CONFIRMATION)
                    }
                }
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


}
