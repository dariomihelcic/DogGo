package hr.ferit.dariomihelcic.doggo.tflite;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class Classifier {

    private static final int MAX_RESULTS = 3;
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    private static final int IMAGE_SIZE_X = 224;
    private static final int IMAGE_SIZE_Y = 224;
    private static final int BYTES_PER_CHANNEL = 4;

    private static final String MODEL_PATH = "model_3.1.tflite";
    private static final String LABEL_PATH = "labels_dogs.txt";

    private final int[] intValues = new int[IMAGE_SIZE_X * IMAGE_SIZE_Y];
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    private float[][] labelProbArray = null;
    private MappedByteBuffer tfliteModel;
    private List<String> labels;

    private ByteBuffer imgData = null;
    private Interpreter tflite;

    public Classifier(Activity activity, int numThreads) throws IOException {
        tfliteModel = loadModelFile(activity);
        tfliteOptions.setNumThreads(numThreads);
        tflite = new Interpreter(tfliteModel, tfliteOptions);
        labels = loadLabelList(activity);
        imgData =
                ByteBuffer.allocateDirect(
                        DIM_BATCH_SIZE
                                * IMAGE_SIZE_X
                                * IMAGE_SIZE_Y
                                * DIM_PIXEL_SIZE
                                * BYTES_PER_CHANNEL
                                );
        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new float[1][getNumLabels()];
    }

    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labels = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
        return labels;
}

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        try {
            AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
        catch (IOException e){
            Log.e("Classifier", "Model could not load!");
        }
        return  null;
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            Log.e("Classifier", "No data available!");
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < IMAGE_SIZE_X; ++i) {
            for (int j = 0; j < IMAGE_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                addPixelValue(val);
            }
        }
    }

    public List<Recognition> recognizeImage(final Bitmap bitmap) {
        convertBitmapToByteBuffer(bitmap);
        runInference();
        // Find the best classifications.
        PriorityQueue<Recognition> pq =
                new PriorityQueue<Recognition>(
                        3,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                // Intentionally reversed to put high confidence at the head of the queue.
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });
        for (int i = 0; i < labels.size(); ++i) {
            pq.add(
                    new Recognition(
                            "" + i,
                            labels.size() > i ? labels.get(i) : "unknown",
                            getNormalizedProbability(i)));
        }
        final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        tfliteModel = null;
    }

    public static int getImageSizeX(){
        return IMAGE_SIZE_X;
    }

    public static int getImageSizeY(){
        return IMAGE_SIZE_Y;
    }

    private void addPixelValue(int pixelValue){
        imgData.putFloat((pixelValue >> 16) & 0xFF);
        imgData.putFloat((pixelValue >> 8) & 0xFF);
        imgData.putFloat(pixelValue & 0xFF);
    }

    private  float getNormalizedProbability(int labelIndex){
        return labelProbArray[0][labelIndex];
    }


    private  void runInference(){tflite.run(imgData, labelProbArray);}

    private int getNumLabels() {
        return labels.size();
    }

}
