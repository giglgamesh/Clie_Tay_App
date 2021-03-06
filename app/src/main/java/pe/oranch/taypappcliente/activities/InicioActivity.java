package pe.oranch.taypappcliente.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import pe.oranch.taypappcliente.Config;
import pe.oranch.taypappcliente.recursos.PrefManager;
import pe.oranch.taypappcliente.R;
import pe.oranch.taypappcliente.utilities.Utils;
import pe.oranch.taypappcliente.utilities.VolleySingleton;

public class InicioActivity extends AppCompatActivity {
    private ProgressDialog prgDialog;
    private SpannableString loginString;
    //VARIABLE PARA REGISTRAR
    TextView btn_registrarse;
    //FIN VARIABLE
    //VARIABLES GENERALES
    EditText tv_usuario,tv_password;
    //FIN VARIABLES GENERALES
    //FUNCION PARA RETORNAR Y NO SALIR
    //FUNCION DE FACEBOOK
    CallbackManager callbackManager;
    //FIN FUNCION
    //VARIABLES RECORDAR SESION
    private PrefManager prefManager;
    //FIN VARIABLES


    private String jsonStatusSuccessString;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.app_name))
                    .setMessage("Realmente desea salir?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                            System.exit(0);
                        }})
                    .setNegativeButton(android.R.string.no, null).show();
        }
        return true;
    }
    //FIN DE LA FUNCION

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        // Revisar si es la primera vez que inicia el prefManager()
        prefManager = new PrefManager(this);
        if (!prefManager.isFirstTimeLaunch()) {
            iniciarPrincipal();
            finish();
        }
        initData();
    }

    private void iniciarPrincipal(){
        prefManager.setFirstTimeLaunch(false);
        startActivity(new Intent(InicioActivity.this, EncuentraTuMenuActivity.class));
        finish();
    }

    public void registrarvista(View view) {
        //iniciar actividad
        Intent intent = new Intent(InicioActivity.this,RegistrarUsuarioActivity.class);
        InicioActivity.this.startActivity(intent);
        //fin inicio de la actividad
    }

    private void showFailPopup() {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(InicioActivity.this, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(R.string.login);
        builder.setMessage(R.string.login_fail);
        builder.setPositiveButton(R.string.OK, null);
        builder.show();
    }
    private void showOffline() {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(InicioActivity.this, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(R.string.sorry_title);
        builder.setMessage(R.string.device_offline);
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Utils.psLog("OK clicked.");
            }
        });
        builder.show();
    }
    public final boolean isInternetOn() {

        ConnectivityManager cm = (ConnectivityManager) InicioActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                return true;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
                return true;
            }
        } else {
            return false;
        }
        return false;
    }
    private boolean inputValidation() {

        if(tv_usuario.getText().toString().equals("")) {
            Toast.makeText(InicioActivity.this.getApplicationContext(), R.string.usuario_mensaje_validacion,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        if(tv_password.getText().toString().equals("")) {
            Toast.makeText(InicioActivity.this.getApplicationContext(), R.string.password_mensaje_validacion,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        return true;

    }
    private void initData() {
        try {
            loginString = Utils.getSpannableString(getString(R.string.login));
            jsonStatusSuccessString = getResources().getString(R.string.json_status_success);
        }catch(Exception e){
            Utils.psErrorLogE("Error in init data.", e);
        }
    }
    public void loginfacebook(View view){
        callbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("email");
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                getUserDetails(loginResult);
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    protected void getUserDetails(LoginResult loginResult) {
        GraphRequest data_request = GraphRequest.newMeRequest(
                loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject json_object,
                            GraphResponse response) {
                        prefManager.setFirstTimeLaunch(false);
                        Intent intent = new Intent(InicioActivity.this, EncuentraTuMenuActivity.class);

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(InicioActivity.this.getApplicationContext());
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("userProfile", json_object.toString());
                        editor.putString("loguinFacebook", "verdad");
                        editor.apply();
                        startActivity(intent);
                    }

                });
        Bundle permission_param = new Bundle();
        permission_param.putString("fields", "id,name,email,picture.width(120).height(120)");
        data_request.setParameters(permission_param);
        data_request.executeAsync();

    }

    protected void onResume() {
        super.onResume();
        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }

    public void doLogin(View view) {
        //SETEO DE CAMPOS PARA LOGUIN
        tv_usuario = (EditText) findViewById(R.id.Tv_usuario);
        tv_password = (EditText) findViewById(R.id.Tv_password);
        if(inputValidation()) {
            final String URL = Config.APP_API_URL + Config.POST_USER_LOGIN;
            Utils.psLog(URL);

            HashMap<String, String> params = new HashMap<>();
            params.put("tay_cliente_email", tv_usuario.getText().toString().trim());
            params.put("tay_cliente_contrasena", tv_password.getText().toString().trim());

            doSubmit(URL, params);
        }
    }
    private void doSubmit(String postURL, HashMap<String, String> params) {
        if(isInternetOn()) {
            //prgDialog.show();

            JsonObjectRequest req = new JsonObjectRequest(postURL, new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {

                                Utils.psLog(" ....Inicio de sesión devolución de llamada .... " + response);

                                String status = response.getString("status");
                                if (status.equals(jsonStatusSuccessString)) {

                                    JSONObject dat = response.getJSONObject("data");
                                    //retorna el valor de nombre de la base de datos en el jason
                                    String idcliente = dat.getString("tay_cliente_id");
                                    String nombre = dat.getString("tay_cliente_nombre");
                                    String usuario = dat.getString("tay_cliente_email");
                                    String tipo = dat.getString("tay_cliente_tipo");
                                    String fecha = dat.getString("tay_cliente_fecha");
                                    String estado = dat.getString("tay_cliente_estado");
                                    //fin retorno

                                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(InicioActivity.this.getApplicationContext());
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putInt("tay_cliente_id", Integer.parseInt(idcliente));
                                    editor.putString("tay_cliente_nombre", nombre);
                                    editor.putString("tay_cliente_email", usuario);
                                    editor.putInt("tay_cliente_tipo", Integer.parseInt(tipo));
                                    editor.putString("tay_cliente_fecha", fecha);
                                    editor.putInt("tay_cliente_estado", Integer.parseInt(estado));
                                    editor.putString("loguinFacebook", "falso");
                                    editor.apply();

                                    //iniciar actividad
                                    prefManager.setFirstTimeLaunch(false);
                                    Intent intent = new Intent(InicioActivity.this,EncuentraTuMenuActivity.class);
                                    //finalizar actividad
                                    //enviar valor
                                    intent.putExtra("tay_cliente_nombre", nombre);
                                    //fin envio de valor

                                    InicioActivity.this.startActivity(intent);

                                } else {
                                    Utils.psLog("Error al Iniciar Sesión");
                                   // prgDialog.cancel();
                                    showFailPopup();


                                }

                            } catch (JSONException e) {
                              //  prgDialog.cancel();
                                Utils.psLog("Error al Iniciar Sesión : " + e.getMessage());
                                e.printStackTrace();
                                showFailPopup();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                   // prgDialog.cancel();
                    Utils.psLog("Error: " + error.getMessage());
                }
            });
            req.setShouldCache(false);
            // add the request object to the queue to be executed
            VolleySingleton.getInstance(InicioActivity.this).addToRequestQueue(req);
        } else {

            showOffline();;

        }
    }
}
