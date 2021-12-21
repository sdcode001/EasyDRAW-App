package eu.deysouvik.easydraw

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View


class DrawView(context: Context, attrs:AttributeSet): View(context,attrs) {

   private var brushsize:Float=0.toFloat()
   private var drawpath:CustomPath?=null
   private var mbitmap:Bitmap?=null
   private var drawpaint:Paint?=null
   private var canvasPaint:Paint?=null
   private var brushColor= Color.BLACK
   private var mCanvas:Canvas?=null
   private var savedPaths=ArrayList<CustomPath>()


    internal inner class CustomPath(var pathColor:Int,var pathWidth:Float): Path(){

    }

   init{
       setupDrawing()
   }

   private fun setupDrawing(){
       drawpath=CustomPath(brushColor,brushsize)
       drawpaint=Paint()
       drawpaint!!.color=brushColor
       drawpaint!!.style=Paint.Style.STROKE
       drawpaint!!.strokeCap=Paint.Cap.BUTT
       drawpaint!!.strokeJoin=Paint.Join.MITER
       canvasPaint=Paint(Paint.DITHER_FLAG)
       brushsize=15.toFloat()
   }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mbitmap= Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        mCanvas=Canvas(mbitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mbitmap!!,0f,0f,canvasPaint)

        //to draw the previous saved paths on the canvas
        for(mpath in savedPaths){
            drawpaint!!.color=mpath.pathColor
            drawpaint!!.strokeWidth=mpath.pathWidth
            canvas.drawPath(mpath, drawpaint!!)
        }

        if(!drawpath!!.isEmpty) {
            drawpaint!!.color=drawpath!!.pathColor
            drawpaint!!.strokeWidth=drawpath!!.pathWidth
            canvas.drawPath(drawpath!!, drawpaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
       var touchX=event?.x
       var touchY=event?.y
        when(event?.action){
            MotionEvent.ACTION_DOWN->{
                drawpath!!.pathColor=brushColor
                drawpath!!.pathWidth=brushsize
                drawpath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        drawpath!!.moveTo(touchX,touchY)
                    }
                }
            }

            MotionEvent.ACTION_MOVE->{
                if (touchX != null) {
                    if (touchY != null) {
                        drawpath!!.lineTo(touchX,touchY)
                    }
                }
            }

            MotionEvent.ACTION_UP->{
                savedPaths.add(drawpath!!)        //storing the paths in savedpaths array
                drawpath=CustomPath(brushColor,brushsize)
            }

            else->return false
        }

        invalidate()

        return true
    }

   fun setBrushSize(newsize:Float){
       brushsize=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newsize,resources.displayMetrics)

       drawpaint!!.strokeWidth=brushsize
   }


    fun setBrushColor(newcolor:String){
        brushColor=Color.parseColor(newcolor)
        drawpaint!!.color=brushColor
    }


   fun undo_paths(){
       if(savedPaths.size>0){
           savedPaths.removeAt(savedPaths.size-1)  //removing path from last
           invalidate()  //invalidate will call onDraw method to draw the paths in savedpaths
       }

   }







}




