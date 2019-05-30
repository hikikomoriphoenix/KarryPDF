package marabillas.loremar.pdfparserapp

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import marabillas.loremar.pdfparser.PDFParser
import marabillas.loremar.pdfparser.contents.image.ImageContent
import marabillas.loremar.pdfparser.contents.text.TableContent
import marabillas.loremar.pdfparser.contents.text.TextContent

class PageNavigation(private val activity: AppCompatActivity, private val pageView: LinearLayout) {
    var document: PDFParser? = null
        set(value) {
            field = value
            numPages = field?.getTotalPages() ?: 0
        }

    var pageNumber = 0
        private set

    var numPages = 0
        private set

    fun goToPage(num: Int, onPageChangeAction: (pageNumber: Int, pageTotal: Int) -> Unit) {
        pageNumber = num
        val contents = document?.getPageContents(pageNumber)
        pageView.removeAllViews()
        contents?.forEach { content ->
            when (content) {
                is TextContent -> {
                    val textView = TextView(activity)
                    val params = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    textView.layoutParams = params
                    textView.text = content.content
                    pageView.addView(textView)
                }
                is ImageContent -> {
                    val imageView = ImageView(activity)
                    val params = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    imageView.layoutParams = params
                    imageView.adjustViewBounds = true
                    val bitmap = BitmapFactory.decodeByteArray(content.content, 0, content.content.size)
                    val drawable = BitmapDrawable(activity.resources, bitmap)
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

                    val textView = TextView(activity)
                    val params = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    textView.layoutParams = params
                    textView.text = sb
                    pageView.addView(textView)
                }
            }
        }

        onPageChangeAction(pageNumber, numPages)
    }

    fun back(onPageChangeAction: (pageNumber: Int, pageTotal: Int) -> Unit) {
        if (pageNumber - 1 >= 0) {
            pageNumber--
            goToPage(pageNumber, onPageChangeAction)
        }
    }

    fun next(onPageChangeAction: (pageNumber: Int, pageTotal: Int) -> Unit) {
        if (pageNumber + 1 < numPages) {
            pageNumber++
            goToPage(pageNumber, onPageChangeAction)
        }
    }
}