package marabillas.loremar.pdfparserapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.widget.TextView
import marabillas.loremar.pdfparser.PDFParser
import marabillas.loremar.pdfparser.contents.TextContent
import java.io.RandomAccessFile

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        val file = RandomAccessFile("${filesDir.path}/sample2.pdf", "r")
        val parser = PDFParser().loadDocument(file)

        val contents = parser.getPageContents(0)
        val textView = findViewById<TextView>(R.id.textView)
        val sb = SpannableStringBuilder()
        contents.forEach {
            if (it is TextContent)
                sb.append(it.content)
        }
        textView.text = sb

        println("SIZE -> ${sb.length}")
        println("CONTENT -> $sb")
    }
}
