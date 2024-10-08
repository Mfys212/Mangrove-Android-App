package com.rajeshportfolio.databits
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import com.rajeshportfolio.databits.ml.Classification
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class MainActivity : AppCompatActivity() {

    //    Declaring variables
    lateinit var TakePhoto: LinearLayout
    lateinit var ChoosePhoto: LinearLayout
    lateinit var Result: TextView
    lateinit var Image: ImageView
    lateinit var Prob: TextView
    lateinit var Predict: Button
    lateinit var bitmap: Bitmap

    //    override on create method
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        TakePhoto = findViewById(R.id.button4)
        ChoosePhoto = findViewById(R.id.button5)
        Result = findViewById(R.id.textView2)
        Image = findViewById(R.id.imageView7)
        Predict = findViewById(R.id.button)
        Prob = findViewById(R.id.tvScoreRate)

        var labels = application.assets.open("labels.txt").bufferedReader().readLines()

        // image processor
        var imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 1f))
            .build()

        TakePhoto.setOnClickListener {
            var intent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, 2)
        }

        ChoosePhoto.setOnClickListener {
            var intent: Intent = Intent()
            intent.setAction(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")

//            handle null point
            startActivityForResult(Intent.createChooser(intent, "Select an Image"), 1)
        }

        Predict.setOnClickListener {
//          check whether image is selected or not
            if(!::bitmap.isInitialized){
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Result.text = getString(R.string.loading)

            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)

            tensorImage = imageProcessor.process(tensorImage)

            val model = Classification.newInstance(this)

            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            inputFeature0.loadBuffer(tensorImage.buffer)

            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

            var maxIdx = 0
            outputFeature0.forEachIndexed { index, fl ->
                if (outputFeature0[maxIdx] < fl) {
                    maxIdx = index
                }
            }
            // Check if the probability is below 0.5
            if (outputFeature0[maxIdx] < 0.7) {
                Result.text = getString(R.string.mangrove_not_detected)
            } else {
                Result.text = labels[maxIdx]
                Prob.text = String.format(getString(R.string.score_rate), outputFeature0[maxIdx])
            }

            // Releases model resources if no longer used.
            model.close()
        }

        val btnExitApp = findViewById<Button>(R.id.btnExitApp)

        btnExitApp.setOnClickListener {
            val intent = Intent(this, ExitActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Result.text = getString(R.string.result_notif)
        Prob.text = getString(R.string.scoreRate)
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                val uri = data?.data
                bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                Image.setImageBitmap(bitmap)
            } else if (requestCode == 2) {
                bitmap = data?.extras?.get("data") as Bitmap
                Image.setImageBitmap(bitmap)
            }

            // Menghapus background setelah gambar baru di-set
            Image.background = null

            // Menyembunyikan TextView di bawah ImageView
            val textViewBelowImage = findViewById<TextView>(R.id.textViewBelowImage)
            textViewBelowImage.visibility = View.GONE
        }
    }


}