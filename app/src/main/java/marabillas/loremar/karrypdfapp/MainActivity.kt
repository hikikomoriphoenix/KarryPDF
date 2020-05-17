package marabillas.loremar.karrypdfapp

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import marabillas.loremar.karrypdf.KarryPDF
import java.io.RandomAccessFile

class MainActivity : AppCompatActivity() {
    var pageNavigation: PageNavigation? = null
        private set

    var pageIndicator: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        val file = RandomAccessFile(
            "${filesDir.path}/PDF32000_2008.pdf",
            "r"
        )
        TimeCounter.reset()
        val parser = KarryPDF(file)
        println("KARRPDF => ${TimeCounter.getTimeElapsed()}")

        // Get views
        val pageView = findViewById<LinearLayout>(R.id.pageview)
        val back = findViewById<TextView>(R.id.back_button)
        val next = findViewById<TextView>(R.id.next_button)
        pageIndicator = findViewById(R.id.page_button)

        // Initialize PageNavigation
        pageNavigation = PageNavigation(this, pageView)
        pageNavigation?.document = parser
        pageNavigation?.goToPage(0, this::indicatePageNumber)

        // Setup UI events handling
        back.setOnClickListener {
            pageNavigation?.back(this::indicatePageNumber)
        }
        next.setOnClickListener {
            pageNavigation?.next(this::indicatePageNumber)
        }
        val numPages = pageNavigation?.numPages ?: 0
        pageIndicator?.setOnClickListener {
            UserInputDialog(
                this,
                "Go to Page: (1 - $numPages)",
                { input ->
                    pageNavigation?.goToPage(
                        Integer.parseInt(input) - 1,
                        this::indicatePageNumber
                    )
                },
                this::validateGoToPageInput
            )
        }
    }

    private fun indicatePageNumber(num: Int, total: Int) {
        val pStr = "${num + 1} / $total"
        pageIndicator?.text = pStr
    }

    private fun validateGoToPageInput(input: String): Boolean {
        return try {
            Integer.parseInt(input) in 1..(pageNavigation?.numPages ?: 1)
        } catch (e: NumberFormatException) {
            false
        }
    }
}
