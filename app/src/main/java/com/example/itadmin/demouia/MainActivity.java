package com.example.itadmin.demouia;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.itadmin.demouia.helper.ImageHelper;
import com.example.itadmin.demouia.helper.SelectImageActivity;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.microsoft.applicationinsights.library.TelemetryClient;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.FaceRectangle;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.microsoft.applicationinsights.library.ApplicationInsights;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MobileServiceClient mClient;
    EditText edtNombre;
    TelemetryClient telemetryClient;

    // Flag to indicate which task is to be performed.
    private static final int REQUEST_SELECT_IMAGE = 0;


    // The URI of the image selected to detect.
    private Uri mImageUri;

    // The image selected to detect.
    private Bitmap mBitmap;

    // The edit to show status and result.
    private TextView txtvLogs;

    private EmotionServiceClient emotionServiceClient;

    // Flag to indicate the request of the next task to be performed
    private static final int REQUEST_TAKE_PHOTO = 0;
    private static final int REQUEST_SELECT_IMAGE_IN_ALBUM = 1;

    // The URI of photo taken with camera
    private Uri mUriPhotoTaken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emotionServiceClient = new EmotionServiceRestClient(getString(R.string.subscription_key));

        // referencias
        edtNombre = (EditText) findViewById(R.id.edtUserName);
        txtvLogs = (TextView) findViewById(R.id.txtvLogs);

        // OnClick Listeners
        findViewById(R.id.btnAgregar).setOnClickListener(this);
        findViewById(R.id.btnTomarfoto).setOnClickListener(this);

        try {
            mClient = new MobileServiceClient(
                    "https://pre-kia.azurewebsites.net",
                    this
            );
        }
        catch (Exception e)
        {

        }


        ApplicationInsights.setup(this.getApplicationContext(), this.getApplication());
        ApplicationInsights.start();

        // obtenemos la instancia del cliente para telemetría
        telemetryClient = TelemetryClient.getInstance();


        /*
        List<Pair<String, String>> parameters = new ArrayList<Pair<String, String>>();

        ListenableFuture<JsonElement> result =  mClient.invokeApi("alumnos","GET",parameters);

        Futures.addCallback(result, new FutureCallback<JsonElement>() {
            @Override
            public void onFailure(Throwable exc) {

                Toast.makeText(MainActivity.this,"Error al agregar",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(JsonElement result) {

                Toast.makeText(MainActivity.this,"Estudiante agregado",Toast.LENGTH_SHORT).show();
            }


        });
        */

    }

    @Override
    public void onClick(View v) {

        switch (v.getId())
        {
            case R.id.btnAgregar:

                //track an event
                telemetryClient.trackPageView("agregar usuario");

                agregarUsuario();
                break;

            case R.id.btnTomarfoto:
                Intent intent;
                intent = new Intent(MainActivity.this, SelectImageActivity.class);
                startActivityForResult(intent, REQUEST_SELECT_IMAGE);
                break;


        }

    }

    private void agregarUsuario()
    {
        // se obtiene el texto ingresado
        String nombreUsuario = edtNombre.getText().toString();

        // se verifica si está vacío
        if(nombreUsuario.isEmpty())
        {
            // si es así se indica que se debe ingresar datos
            Toast.makeText(getApplicationContext(), "Debe ingresar datos", Toast.LENGTH_SHORT).show();
        }
        else
        {
            // en caso contrario se procede a insertar el nuevo elemento
            Estudiante item = new Estudiante();
            item.name = nombreUsuario; // se establece el nombre del usuario con el valor que el usuario ingresó

            // se obtiene referencia a la tabla Alumno
            MobileServiceTable<Estudiante> table = mClient.getTable("Alumno",Estudiante.class);

            // se inserta en la tabla Alumno
            table.insert(item, new TableOperationCallback<Estudiante>() {
                public void onCompleted(Estudiante entity, Exception exception, ServiceFilterResponse response) {
                    if (exception == null) {
                        Toast.makeText(MainActivity.this, "Estudiante agregado", Toast.LENGTH_SHORT).show();
                        edtNombre.setText("");
                    } else {
                        Toast.makeText(MainActivity.this, "Error al agregar", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            /*
            ListenableFuture<Estudiante> result =  table.insert(item);

            Futures.addCallback(result, new FutureCallback<Estudiante>() {
                @Override
                public void onFailure(Throwable exc) {

                    Toast.makeText(MainActivity.this,"Error al agregar",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(Estudiante result) {

                    Toast.makeText(MainActivity.this,"Estudiante agregado",Toast.LENGTH_SHORT).show();
                }


            });
            */

        }

    }


    public void doRecognize() {

        // Do emotion detection using auto-detected faces.
        try {
            new doRequest(false).execute();
        } catch (Exception e) {
            txtvLogs.append("Error encountered. Exception is: " + e.toString());
        }

        String faceSubscriptionKey = getString(R.string.faceSubscription_key);
        if (faceSubscriptionKey.equalsIgnoreCase("Please_add_the_face_subscription_key_here")) {
            txtvLogs.append("\n\nThere is no face subscription key in res/values/strings.xml. Skip the sample for detecting emotions using face rectangles\n");
        } else {
            // Do emotion detection using face rectangles provided by Face API.
            try {
                new doRequest(true).execute();
            } catch (Exception e) {
                txtvLogs.append("Error encountered. Exception is: " + e.toString());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("RecognizeActivity", "onActivityResult");
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    // If image is selected successfully, set the image URI and bitmap.
                    mImageUri = data.getData();

                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            mImageUri, getContentResolver());
                    if (mBitmap != null) {
                        // Show the image on screen.
                        ImageView imageView = (ImageView) findViewById(R.id.imgvPreview);
                        imageView.setImageBitmap(mBitmap);

                        // Add detection log.
                        Log.d("RecognizeActivity", "Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                                + "x" + mBitmap.getHeight());

                        doRecognize();
                    }
                }
                break;
            default:
                break;
        }
    }



    private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.d("emotion", "Start emotion detection with auto-face detection");

        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long startTime = System.currentTimeMillis();
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE STARTS HERE
        // -----------------------------------------------------------------------

        List<RecognizeResult> result = null;
        //
        // Detect emotion by auto-detecting faces in the image.
        //
        result = this.emotionServiceClient.recognizeImage(inputStream);

        String json = gson.toJson(result);
        Log.d("result", json);

        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", (System.currentTimeMillis() - startTime)));
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE ENDS HERE
        // -----------------------------------------------------------------------
        return result;
    }

    private List<RecognizeResult> processWithFaceRectangles() throws EmotionServiceException, com.microsoft.projectoxford.face.rest.ClientException, IOException {
        Log.d("emotion", "Do emotion detection with known face rectangles");
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long timeMark = System.currentTimeMillis();
        Log.d("emotion", "Start face detection using Face API");
        FaceRectangle[] faceRectangles = null;
        String faceSubscriptionKey = getString(R.string.faceSubscription_key);
        FaceServiceRestClient faceClient = new FaceServiceRestClient(faceSubscriptionKey);
        Face faces[] = faceClient.detect(inputStream, false, false, null);
        Log.d("emotion", String.format("Face detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));

        if (faces != null) {
            faceRectangles = new FaceRectangle[faces.length];

            for (int i = 0; i < faceRectangles.length; i++) {
                // Face API and Emotion API have different FaceRectangle definition. Do the conversion.
                com.microsoft.projectoxford.face.contract.FaceRectangle rect = faces[i].faceRectangle;
                faceRectangles[i] = new com.microsoft.projectoxford.emotion.contract.FaceRectangle(rect.left, rect.top, rect.width, rect.height);
            }
        }

        List<RecognizeResult> result = null;
        if (faceRectangles != null) {
            inputStream.reset();

            timeMark = System.currentTimeMillis();
            Log.d("emotion", "Start emotion detection using Emotion API");
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE STARTS HERE
            // -----------------------------------------------------------------------
            result = this.emotionServiceClient.recognizeImage(inputStream, faceRectangles);

            String json = gson.toJson(result);
            Log.d("result", json);
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE ENDS HERE
            // -----------------------------------------------------------------------
            Log.d("emotion", String.format("Emotion detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));
        }
        return result;
    }

    private class doRequest extends AsyncTask<String, String, List<RecognizeResult>> {
        // Store error message
        private Exception e = null;
        private boolean useFaceRectangles = false;

        public doRequest(boolean useFaceRectangles) {
            this.useFaceRectangles = useFaceRectangles;
        }

        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
            if (this.useFaceRectangles == false) {
                try {
                    return processWithAutoFaceDetection();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }
            } else {
                try {
                    return processWithFaceRectangles();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            // Display based on error existence

            if (this.useFaceRectangles == false) {
                txtvLogs.append("\n\nRecognizing emotions with auto-detected face rectangles...\n");
            } else {
                txtvLogs.append("\n\nRecognizing emotions with existing face rectangles from Face API...\n");
            }
            if (e != null) {
                txtvLogs.setText("Error: " + e.getMessage());
                this.e = null;
            } else {
                if (result.size() == 0) {
                    txtvLogs.append("No emotion detected :(");
                } else {
                    Integer count = 0;

                    /*
                    Canvas faceCanvas = new Canvas(mBitmap);
                    faceCanvas.drawBitmap(mBitmap, 0, 0, null);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);
                    paint.setColor(Color.RED);


*/


                    for (RecognizeResult r : result) {
                        txtvLogs.append(String.format("\nFace #%1$d \n", count));
                        txtvLogs.append(String.format("\t anger: %1$.5f\n", r.scores.anger));
                        txtvLogs.append(String.format("\t contempt: %1$.5f\n", r.scores.contempt));
                        txtvLogs.append(String.format("\t disgust: %1$.5f\n", r.scores.disgust));
                        txtvLogs.append(String.format("\t fear: %1$.5f\n", r.scores.fear));
                        txtvLogs.append(String.format("\t happiness: %1$.5f\n", r.scores.happiness));
                        txtvLogs.append(String.format("\t neutral: %1$.5f\n", r.scores.neutral));
                        txtvLogs.append(String.format("\t sadness: %1$.5f\n", r.scores.sadness));
                        txtvLogs.append(String.format("\t surprise: %1$.5f\n", r.scores.surprise));
                        txtvLogs.append(String.format("\t face rectangle: %d, %d, %d, %d", r.faceRectangle.left, r.faceRectangle.top, r.faceRectangle.width, r.faceRectangle.height));
                        /*
                        faceCanvas.drawRect(r.faceRectangle.left,
                                r.faceRectangle.top,
                                r.faceRectangle.left + r.faceRectangle.width,
                                r.faceRectangle.top + r.faceRectangle.height,
                                paint);
                        */
                        count++;



                        Map<Double, String> a = new TreeMap<>();
                        a.put(r.scores.anger,"ANGER");
                        a.put(r.scores.contempt,"CONTEMPT");
                        a.put(r.scores.disgust,"DISGUST");
                        a.put(r.scores.fear,"FEAR");
                        a.put(r.scores.happiness,"HAPPINESS");
                        a.put(r.scores.neutral,"NEUTRAL");
                        a.put(r.scores.sadness,"SADNESS");
                        a.put(r.scores.surprise,"SURPRISE");

                        double maxValue = Collections.max(a.keySet());

                        String expressionName = a.get(maxValue);

                        txtvLogs.append("\n" + expressionName);

                    }
                    ImageView imageView = (ImageView) findViewById(R.id.imgvPreview);
                    imageView.setImageDrawable(new BitmapDrawable(getResources(), mBitmap));
                }

            }

        }

    }
}
