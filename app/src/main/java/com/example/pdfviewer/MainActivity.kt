package com.example.pdfviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.pdfviewer.ui.theme.PDFViewerTheme
import com.example.pdfviewer.utility.Utility

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PDFViewerTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    LoadPDFFromAsset()

//                    LoadPDFinWebView()

                }
            }
        }
    }
}


@Composable
fun LoadPDFFromAsset() {
    val context = LocalContext.current
    val file = Utility.getFileFromAssets(context,"sample2.pdf")
//    Render1(file = file)
    Render2(file)
}

@Composable
fun LoadPDFinWebView() {
    val url = "https://www.africau.edu/images/default/sample.pdf"
//    val url = "https://medium.com/telepass-digital/how-to-show-a-pdf-with-jetpack-compose-74fc773adbd0"
    /**
     * Avoid using this method to display PDF as there is certain limit to `Google Drive` request that we can make.
     */
    WebViewScreen("https://docs.google.com/gview?embedded=true&url=$url")
}