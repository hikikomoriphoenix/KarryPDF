package marabillas.loremar.pdfparserapp

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import marabillas.loremar.pdfparser.PDFParser
import marabillas.loremar.pdfparser.contents.image.ImageContent
import marabillas.loremar.pdfparser.contents.text.TableContent
import marabillas.loremar.pdfparser.contents.text.TextContent
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        val file = RandomAccessFile("${filesDir.path}/PdfWithTable.pdf", "r")
        val parser = PDFParser().loadDocument(file)

        Handler(mainLooper).postDelayed(
            {
                TimeCounter.reset()
                val contents = parser.getPageContents(0)
                println("App getting contents -> ${TimeCounter.getTimeElapsed()} ms")
                val pageView = findViewById<LinearLayout>(R.id.pageview)
                contents.forEach { content ->
                    when (content) {
                        is TextContent -> {
                            val textView = TextView(this)
                            val params = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                            textView.layoutParams = params
                            textView.text = content.content
                            pageView.addView(textView)
                            println("text->${content.content}")
                        }
                        is ImageContent -> {
                            val imageView = ImageView(this)
                            val params = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                            imageView.layoutParams = params
                            imageView.adjustViewBounds = true
                            val bitmap = BitmapFactory.decodeByteArray(content.content, 0, content.content.size)
                            val drawable = BitmapDrawable(resources, bitmap)
                            imageView.setImageDrawable(drawable)
                            pageView.addView(imageView)
                        }
                        is TableContent -> {
                            val sb = SpannableStringBuilder()
                            content.rows.forEach { row ->
                                val isFirstRow = content.rows.first() == row
                                row.cells.forEach { cell ->
                                    if (row.cells.first() == cell && !isFirstRow) {
                                        sb.append('\n')
                                    }
                                    sb.append(cell.content)
                                    if (cell != row.cells.last()) {
                                        sb.append(" || ")
                                    }
                                }
                            }

                            val textView = TextView(this)
                            val params = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                            textView.layoutParams = params
                            textView.text = sb
                            pageView.addView(textView)


                        }
                    }
                }
            },
            TimeUnit.SECONDS.toMillis(5)
        )
    }
}
