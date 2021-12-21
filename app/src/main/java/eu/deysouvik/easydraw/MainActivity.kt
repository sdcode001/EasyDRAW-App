package eu.deysouvik.easydraw

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.brush_size_selection_dialog.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {


    var isErase=0
    var prev_color="#FF000000"
    var prev_brushSize=10.toFloat()
    var storage_read_write=0   //if it is 1 then read.if it is 2 then write


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        draw_view.setBrushSize(10.toFloat())



    }


    fun brushbtn(view: View){                    //for brush size selection dialog
          val brushSizeDialog=Dialog(this)
        brushSizeDialog.setContentView(R.layout.brush_size_selection_dialog)
        brushSizeDialog.setTitle("BrushSize : ")
        brushSizeDialog.show()

        draw_view.setBrushColor(prev_color)
        isErase=0

        val verysmallbtn=brushSizeDialog.very_small_brush_btn
        verysmallbtn.setOnClickListener {
            draw_view.setBrushSize(5.toFloat())
            prev_brushSize=5.toFloat()
            brushSizeDialog.dismiss()

        }

        val smallbtn=brushSizeDialog.small_brush_btn
        smallbtn.setOnClickListener {
            draw_view.setBrushSize(10.toFloat())
            prev_brushSize=10.toFloat()
            brushSizeDialog.dismiss()
        }

        val mediumbtn=brushSizeDialog.medium_brush_btn
        mediumbtn.setOnClickListener {
            draw_view.setBrushSize(20.toFloat())
            prev_brushSize=20.toFloat()
            brushSizeDialog.dismiss()
        }

        val largebtn=brushSizeDialog.large_brush_btn
        largebtn.setOnClickListener {
            draw_view.setBrushSize(30.toFloat())
            prev_brushSize=30.toFloat()
            brushSizeDialog.dismiss()
        }

    }



    fun colorbtn(view: View){
        val selected_color=view as ImageButton
        val s_color=selected_color.tag.toString()
        draw_view.setBrushColor(s_color)
        prev_color=s_color
    }

    fun eraserbtn(view: View){
        if(isErase==0){
            var white="#FFFFFFFF"
            draw_view.setBrushColor(white)
            draw_view.setBrushSize(25.toFloat())
            isErase=1
        }
        else{
            isErase=0
            draw_view.setBrushColor(prev_color)
            draw_view.setBrushSize(prev_brushSize)
        }
    }


    fun undo_btn(view: View){
        draw_view.undo_paths()
    }


    fun gallerybtn(view: View){
        storage_read_write=1
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
              Snackbar.make(view,"You have permission",Snackbar.LENGTH_LONG).show()

            Image_Select()
        }
        else{
            request_Permission()
        }
    }

    fun Image_Select(){
        //this method is depricated by android but it still works perfectly

        val intent_to_gallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(intent_to_gallery, GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== Activity.RESULT_OK){
            if(requestCode==GALLERY){
                try{
                    if(data!!.data!=null){
                        iv_background.visibility=View.VISIBLE
                        iv_background.setImageURI(data.data)
                    }
                    else{
                        Toast.makeText(this, "Data has been corrupted", Toast.LENGTH_SHORT).show()
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
    }



    fun save_btn(view: View){
        storage_read_write=2
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
            sendImage_to_storage_AsyncTask(makeImage(draw_frame)).execute()
        }
        else{
            request_Permission()
        }

    }


    fun request_Permission(){

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,),STORAGE_READ_WRITE_PERMISSION_CODE)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode== STORAGE_READ_WRITE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){     //this will happen when we grant permission by pressing allow btn on permission dialog box
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()

                if(storage_read_write==1){
                    Image_Select()
                }
                else{
                    sendImage_to_storage_AsyncTask(makeImage(draw_frame)).execute()
                }
            }
            else{              //this will happen when we deny permission by pressing deny of cancel btn on permission dialog box
                Toast.makeText(this, "You denied to give permission", Toast.LENGTH_LONG).show()


            }
        }

    }




    fun makeImage(view: View):Bitmap{
        val Ibitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas_Ibitmap= Canvas(Ibitmap)
        val background=view.background
        if(background!=null){
            background.draw(canvas_Ibitmap)
        }
        else{
            canvas_Ibitmap.drawColor(Color.WHITE)
        }
        view.draw(canvas_Ibitmap)

        return Ibitmap
    }

    private inner class sendImage_to_storage_AsyncTask(val mBitmap:Bitmap): AsyncTask<Any,Void,String>(){

        private lateinit var progress_bar:Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            progress_bar= Dialog(this@MainActivity)
            progress_bar.setContentView(R.layout.custom_progress_bar)
            progress_bar.show()

        }


        override fun doInBackground(vararg p0: Any?): String {
           var result=""   //because we have to return a string
            try{
               val bytes=ByteArrayOutputStream()
                mBitmap.compress(Bitmap.CompressFormat.PNG,95,bytes)  //converting bitmap to bytes_stream
                //this file will contain the byte_stream or we can say file is the name of our image.
                val file= File(externalCacheDir!!.absoluteFile.toString()+
                                        File.separator+"EasyDRAW_app"+System.currentTimeMillis()/1000+
                                         ".png")
                //now insert the bytes_flow into the file.
                //here fle is tool which insert the bytes_flow into the file.
                val fle=FileOutputStream(file)  //here fle takes the container where to insert/write.
                fle.write(bytes.toByteArray())  //here fle insert/write the bytes_stream as byteArray
                fle.close()  //here we stop the process.

                result=file.absolutePath  //storing the result of this process into result

            }catch (e:Exception){
                result=""
                e.printStackTrace()
            }

            return result
        }


        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            progress_bar.dismiss()

            if(result!!.isNotEmpty()){
                Toast.makeText(this@MainActivity, "Image saved successfully: $result", Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(this@MainActivity, "Failed to save Image", Toast.LENGTH_SHORT).show()
            }

        }
    }


    fun share_btn(view: View){
        shareImage_to_social_media_AsyncTask(makeImage(draw_frame)).execute()
    }

   private inner class shareImage_to_social_media_AsyncTask(val mBitmap:Bitmap):AsyncTask<Any,Void,String>(){

       override fun doInBackground(vararg p0: Any?): String {
           var result=""   //because we have to return a string
           try{
               val bytes=ByteArrayOutputStream()
               mBitmap.compress(Bitmap.CompressFormat.PNG,95,bytes)  //converting bitmap to bytes_stream
               //this file will contain the byte_stream or we can say file is the name of our image.
               val file= File(externalCacheDir!!.absoluteFile.toString()+
                       File.separator+"EasyDRAW_app"+System.currentTimeMillis()/1000+
                       ".png")
               //now insert the bytes_flow into the file.
               //here fle is tool which insert the bytes_flow into the file.
               val fle=FileOutputStream(file)  //here fle takes the container where to insert/write.
               fle.write(bytes.toByteArray())  //here fle insert/write the bytes_stream as byteArray
               fle.close()  //here we stop the process.

               result=file.absolutePath  //storing the result of this process into result

           }catch (e:Exception){
               result=""
               e.printStackTrace()
           }

           return result
       }

       override fun onPostExecute(result: String?) {
           super.onPostExecute(result)

           if(result!!.isNotEmpty()){
               //MediaScannerConnection class allow us to share file on various shareable application present on our device
               //and this scanFile() method takes the context where it will show a dialog of sharing options and takes the array of files which has to be shared
              MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result),null){
                  path,uri-> val share_Intent=Intent()     //we need intent to transfer file from context to other sharing options as uri
                  share_Intent.action=Intent.ACTION_SEND
                  share_Intent.putExtra(Intent.EXTRA_STREAM,uri) //putting the uri
                  share_Intent.type="image/png"

                  startActivity(                     //starting the activity of the intent and sending process starts
                      Intent.createChooser(
                          share_Intent,"Share"
                      )
                  )

              }
           }
           else{
               Toast.makeText(this@MainActivity, "Failed to Share Image", Toast.LENGTH_SHORT).show()
           }
       }

   }
   companion object{
       private const val STORAGE_READ_WRITE_PERMISSION_CODE=1
       private const val GALLERY=3
   }































}