package com.example.android.camera2basic;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;


public class PredictionTF {
    private static final String TAG = "PredictionTF";
    private static final String INPUT_NAME  = "input:0";
    private static final String OUTPUT_NAME = "embeddings:0";
    private static final String PHASE_NAME  = "phase_train:0";
    private static final String[] outputNames = new String[] {OUTPUT_NAME};
    //输入图片大小.(图片非此大小，会rescale)
    private static final int INPUT_SIZE=160;
    private float[] floatValues;  //保存input的值
    private int[] intValues;      //像素值

    private TensorFlowInferenceInterface inferenceInterface;
    static {
        //加载libtensorflow_inference.so库文件
        System.loadLibrary("tensorflow_inference");
        Log.e(TAG,"libtensorflow_inference.so库加载成功");
    }

    PredictionTF(AssetManager assetManager, String modePath) {
        //初始化TensorFlowInferenceInterface对象
        inferenceInterface = new TensorFlowInferenceInterface(assetManager,modePath);
        floatValues=new float[INPUT_SIZE*INPUT_SIZE*3];
        intValues = new int[INPUT_SIZE * INPUT_SIZE];
        Log.e(TAG,"TensoFlow模型文件加载成功");
    }

    /**
     *  利用训练好的TensoFlow模型预测结果
     * @param bitmap 输入被测试的bitmap图
     * @return 返回预测结果，int数组
     */
    public FaceFeature recognizeImage(final Bitmap bitmap){
        //Log.d("Facenet","[*]recognizeImage");
        //(0)图片预处理，normailize
        normalizeImage(bitmap);
        //(1)Feed
        try {
            inferenceInterface.feed(INPUT_NAME, floatValues, 1, INPUT_SIZE, INPUT_SIZE, 3);
            boolean [] phase=new boolean[1];
//            phase[0]=false;
            inferenceInterface.feed(PHASE_NAME,phase);

        }catch (Exception e){
            Log.e("Facenet","[*] feed Error\n"+e);
            return null;
        }
        //(2)run
        // Log.d("Facenet","[*]Feed:"+INPUT_NAME);
        try {
            inferenceInterface.run(outputNames, false);
        }catch (Exception e){
            Log.e("Facenet","[*] run error\n"+e);
            return null;
        }
        //(3)fetch
        FaceFeature faceFeature=new FaceFeature();
        float[] outputs=faceFeature.getFeature();
        try {
            inferenceInterface.fetch(OUTPUT_NAME, outputs);
            faceFeature.setFea(outputs);
        }catch (Exception e){
            Log.e("Facenet","[*] fetch error\n"+e);
            return null;
        }
        return faceFeature;
    }


    private void normalizeImage(final Bitmap _bitmap){
        // (0) bitmap缩放到INPUT_SIZE*INPUT_SIZE
        float scale_width=((float)INPUT_SIZE)/_bitmap.getWidth();
        float scale_height=((float)INPUT_SIZE)/_bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scale_width,scale_height);
        Bitmap bitmap = Bitmap.createBitmap(_bitmap,0,0,_bitmap.getWidth(),_bitmap.getHeight(),matrix,true);
        //Log.d("Facenet","[*]bitmap size:"+bitmap.getHeight()+"x"+bitmap.getWidth());
        // (1) 将像素映射到[-1,1]区间内
        float imageMean=127.5f;
        float imageStd=128;
        bitmap.getPixels(intValues,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        for (int i=0;i<intValues.length;i++){
            final int val=intValues[i];
            floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - imageMean) / imageStd;
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;
            floatValues[i * 3 + 2] = ((val & 0xFF) - imageMean) / imageStd;
        }

    }
}