package com.example.itadmin.demouia;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MobileServiceClient mClient;
    EditText edtNombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // referencia al EditText
        edtNombre = (EditText) findViewById(R.id.edtUserName);

        // se establece el onClickListener el botón
        findViewById(R.id.btnAgregar).setOnClickListener(this);

        try {
            mClient = new MobileServiceClient(
                    "https://pre-kia.azurewebsites.net",
                    this
            );
        }
        catch (Exception e)
        {

        }

        
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

        if (v.getId() == R.id.btnAgregar)
        {
            agregarUsuario();
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
}
