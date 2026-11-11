package com.scanly.app.tools

import android.graphics.Bitmap
import android.graphics.Color

/** Local ink extraction for signature photos, not general-purpose subject segmentation. */
object SignatureCleaner {
    fun clean(source: Bitmap,strength: Int=10): Bitmap {
        val w=source.width;val h=source.height
        require(w>0 && h>0 && w.toLong()*h<=6_000_000)
        val pixels=IntArray(w*h);source.getPixels(pixels,0,w,0,0,w,h)
        val transparent=pixels.count { Color.alpha(it)<240 }>pixels.size/100
        if(!transparent) {
            val gray=IntArray(pixels.size) { i -> val c=pixels[i];(Color.red(c)*77+Color.green(c)*150+Color.blue(c)*29) ushr 8 }
            val integral=LongArray((w+1)*(h+1))
            for(y in 0 until h) { var sum=0L;for(x in 0 until w) { sum+=gray[y*w+x];integral[(y+1)*(w+1)+x+1]=integral[y*(w+1)+x+1]+sum } }
            val edges=ArrayList<Int>()
            for(x in 0 until w) { edges.add(gray[x]);edges.add(gray[(h-1)*w+x]) }
            for(y in 0 until h) { edges.add(gray[y*w]);edges.add(gray[y*w+w-1]) }
            edges.sort();val lightPaper=edges[edges.size/2]>=128
            val radius=(minOf(w,h)/10).coerceAtLeast(12)
            for(y in 0 until h) for(x in 0 until w) {
                val l=maxOf(0,x-radius);val r=minOf(w,x+radius+1);val t=maxOf(0,y-radius);val b=minOf(h,y+radius+1)
                val mean=(integral[b*(w+1)+r]-integral[t*(w+1)+r]-integral[b*(w+1)+l]+integral[t*(w+1)+l]).toFloat()/((r-l)*(b-t))
                val contrast=if(lightPaper) mean-gray[y*w+x] else gray[y*w+x]-mean
                val alpha=((contrast-strength.coerceIn(0,60))*255f/16).toInt().coerceIn(0,255)
                pixels[y*w+x]=(pixels[y*w+x] and 0x00FFFFFF) or (alpha shl 24)
            }
        }
        var left=w;var right=-1;var top=h;var bottom=-1
        for(y in 0 until h) for(x in 0 until w) if(Color.alpha(pixels[y*w+x])>24) {
            left=minOf(left,x);right=maxOf(right,x);top=minOf(top,y);bottom=maxOf(bottom,y)
        }
        require(right>=left && bottom>=top) { "Tanda tangan belum terbaca. Coba kurangi pembersih latar atau pilih foto yang lebih jelas." }
        left=(left-6).coerceAtLeast(0);right=(right+6).coerceAtMost(w-1);top=(top-6).coerceAtLeast(0);bottom=(bottom+6).coerceAtMost(h-1)
        val result=Bitmap.createBitmap(right-left+1,bottom-top+1,Bitmap.Config.ARGB_8888)
        result.setPixels(pixels,top*w+left,w,0,0,result.width,result.height)
        return result
    }
}
