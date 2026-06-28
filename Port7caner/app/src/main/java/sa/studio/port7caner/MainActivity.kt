package sa.studio.port7caner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : AppCompatActivity() {

    external fun checkPort(ip: String, port: Int): Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etIp = findViewById<EditText>(R.id.et_ip)
        val btnScan = findViewById<Button>(R.id.btn_scan)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val tvLog = findViewById<TextView>(R.id.tv_log)

        val commonPorts = intArrayOf(
            21, 22, 23, 25, 53, 80, 110, 139, 143, 443, 445, 3306, 3389, 8080, 8443
        )

        btnScan.setOnClickListener {
            val targetIp = etIp.text.toString().trim()

            if (targetIp.isEmpty()) {
                tvLog.text = "Error: Please enter a target IP address!"
                return@setOnClickListener
            }

            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            tvLog.text = "Starting fast parallel port scan for: $targetIp...\n"
            tvLog.append("----------------------------------\n")

            progressBar.visibility = View.VISIBLE
            progressBar.max = commonPorts.size
            progressBar.progress = 0
            btnScan.isEnabled = false

            lifecycleScope.launch {
                val openPortsCount = AtomicInteger(0)
                val progressCount = AtomicInteger(0)

                val jobs = commonPorts.map { port ->
                    async(Dispatchers.IO) {
                        val isOpen = checkPort(targetIp, port)

                        withContext(Dispatchers.Main) {
                            progressBar.progress = progressCount.incrementAndGet()
                            if (isOpen) {
                                openPortsCount.incrementAndGet()
                                tvLog.append("Port $port is OPEN\n")
                            }
                        }
                    }
                }

                jobs.awaitAll()

                progressBar.visibility = View.GONE
                btnScan.isEnabled = true
                tvLog.append("----------------------------------\n")

                val totalOpen = openPortsCount.get()
                if (totalOpen == 0) {
                    tvLog.append("Scan finished. No open ports found.")
                } else {
                    tvLog.append("Scan finished. Found $totalOpen open ports.")
                }
            }
        }
    }

    companion object {
        init {
            System.loadLibrary("nativetest")
        }
    }
}