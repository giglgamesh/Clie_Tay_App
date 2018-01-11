package pe.oranch.taypappcliente.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.daimajia.slider.library.Animations.DescriptionAnimation;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;
import com.daimajia.slider.library.Tricks.ViewPagerEx;
import com.google.android.gms.maps.MapView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.CacheRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import pe.oranch.taypappcliente.Config;
import pe.oranch.taypappcliente.GlobalData;
import pe.oranch.taypappcliente.R;
import pe.oranch.taypappcliente.entidades.Tay_slider;
import pe.oranch.taypappcliente.models.PSliderData;
import pe.oranch.taypappcliente.request.ListarSliderRequest;
import pe.oranch.taypappcliente.utilities.Utils;
import pe.oranch.taypappcliente.utilities.VolleySingleton;

import static pe.oranch.taypappcliente.Config.APP_API_URL;

public class EncuentraTuMenuActivity extends AppCompatActivity implements BaseSliderView.OnSliderClickListener,ViewPagerEx.OnPageChangeListener, LocationListener{
    EncuentraTuMenuActivity encuentratumenuActivity;
    ArrayList<Tay_slider> listaSlider;
    //PARA EL REFRESH LAYOUT
    SwipeRefreshLayout swipeRefreshLayout;
    //FIN REFRESH LAYOUT
    //PARA EL SLIDER
    private SliderLayout mDemoSlider;
    HashMap<String, String> HashMapForURL ;
    HashMap<String, Integer> HashMapForLocalRes ;
    String textoslider;
    String urlimagen;
    String nombreslider;
    //FIN PARA EL SLIDER

    //Nuevas Variables
    private String jsonStatusSuccessString;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encuentra_tu_menu);
        ObtenerTiposComida();
        IniciarObjetos();
        //initData();
        IniciarSlider();
        //mDemoSlider.stopAutoCycle();
    }

    public void IniciarObjetos(){
        //INICIALIZAR REFRESH LAYOUT
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.simpleSwipeRefreshLayout);
        //FIN REFRESH LAYOUT
        //REFRESH
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // cancel the Visual indication of a refresh
                swipeRefreshLayout.setRefreshing(false);
                actualizarComidas();
            }
        });
    }

    private void initData(){
        try {
            //final String URL = Config.APP_API_URL + Config.GET_ALL;
            //Utils.psLog(URL);
            //HashMap<String, String> params = new HashMap<>();
            //params.put("estado", 1+"");
            //getSlider(URL, params);
        }catch (Exception e){
            Utils.psErrorLogE("Error in bind Rating", e);
        }
    }
    private void getSlider(String postURL, HashMap<String, String> params) {
        JsonObjectRequest req = new JsonObjectRequest(postURL, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String success_status = response.getString("status");
                            String data_status = response.getString("data");

                            if (success_status.equals(jsonStatusSuccessString)) {
                                JSONObject jresponse = new JSONObject(data_status);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
            }
        });

        req.setShouldCache(false);
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    public final boolean isInternetOn() {

        ConnectivityManager cm = (ConnectivityManager) EncuentraTuMenuActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
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

    private void showOffline() {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(EncuentraTuMenuActivity.this, R.style.AppCompatAlertDialogStyle);
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

    private void showFailPopup() {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(EncuentraTuMenuActivity.this, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(R.string.login);
        builder.setMessage(R.string.login_fail);
        builder.setPositiveButton(R.string.OK, null);
        builder.show();
    }
    public void IniciarSlider(){
        mDemoSlider = (SliderLayout)findViewById(R.id.slider);
        HashMapForURL = new HashMap<String, String>();


        //EXPERIMENTO SLIDER A BASE DE DATOS
        final int empresa = 1;
        Response.Listener<String> responseListenerLista = new Response.Listener<String>(){
            @Override
            public void onResponse(String response) {
                try {
                    listaSlider = new ArrayList<>();
                    JSONObject jsonReponse = new JSONObject(response);
                    Tay_slider tay_slider=null;
                    JSONArray json=jsonReponse.optJSONArray("usuario");
                    for (int i=0;i<json.length();i++){
                        tay_slider=new Tay_slider();
                        JSONObject jsonObject=null;
                        jsonObject=json.getJSONObject(i);
                        tay_slider.setTay_slider_id(jsonObject.optInt("tay_slider_id"));
                        tay_slider.setTay_slider_nombre(jsonObject.optString("tay_slider_nombre"));
                        tay_slider.setTay_slider_url(jsonObject.optString("tay_slider_url"));
                        listaSlider.add(tay_slider);

                        urlimagen = APP_API_URL + (listaSlider.get(i).getTay_slider_url().toString());
                        textoslider = listaSlider.get(i).getTay_slider_nombre().toString();
                        nombreslider = listaSlider.get(i).getTay_slider_id().toString();
                        HashMapForURL.put(textoslider,urlimagen);


                        TextSliderView textSliderView = new TextSliderView(EncuentraTuMenuActivity.this);
                        textSliderView
                                .description(textoslider)
                                .image(HashMapForURL.get(textoslider))
                                .setScaleType(BaseSliderView.ScaleType.Fit)
                                .setOnSliderClickListener(EncuentraTuMenuActivity.this);
                        textSliderView.bundle(new Bundle());
                        textSliderView.getBundle()
                                .putString("extra",textoslider);
                        mDemoSlider.addSlider(textSliderView);

                    }

                    mDemoSlider.setPresetTransformer(SliderLayout.Transformer.Accordion);

                    mDemoSlider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom);

                    mDemoSlider.setCustomAnimation(new DescriptionAnimation());

                    mDemoSlider.setDuration(3000);

                    mDemoSlider.addOnPageChangeListener(EncuentraTuMenuActivity.this);
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        };
        ListarSliderRequest listarsliderRequest = new ListarSliderRequest(empresa,responseListenerLista);
        RequestQueue queue = Volley.newRequestQueue(EncuentraTuMenuActivity.this);
        queue.add(listarsliderRequest);
        //FIN DEL EXPERIMENTO


        //HashMapForLocalRes.put(textoslider,R.drawable.criolla_foto);
        //HashMapForLocalRes.put("Prueba2",R.drawable.criolla_foto);
        //HashMapForLocalRes.put("Prueba3",R.drawable.criolla_foto);
        //HashMapForLocalRes.put("Prueba4",R.drawable.criolla_foto);
        //HashMapForLocalRes.put("Prueba5",R.drawable.criolla_foto);
    }
    @Override
    protected void onStop() {
        mDemoSlider.stopAutoCycle();
        super.onStop();
    }

    @Override
    public void onSliderClick(BaseSliderView slider) {

        Toast.makeText(this,slider.getBundle().get("extra") + "", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {

        Log.d("Slider Demo", "Page Changed: " + position);

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public void VolverPerfil(View view) {
        Intent intent = new Intent(EncuentraTuMenuActivity.this,PrincipalActivity.class);
        EncuentraTuMenuActivity.this.startActivity(intent);
    }

    public void actualizarComidas(){
        mDemoSlider.removeAllSliders();
        IniciarSlider();
    }

    public void ObtenerTiposComida(){

    }

    public void Descubrir(View view) {
        Intent intent = new Intent(EncuentraTuMenuActivity.this,DescubrirActivity.class);
        EncuentraTuMenuActivity.this.startActivity(intent);
    }

    public void VerMapa(View view) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("_selected_city_id", 1);
        editor.putInt("_selected_sub_cat_id", 53);
        editor.putString("_city_region_lat", String.valueOf(-18.022889713301634));
        editor.putString("_city_region_lng", String.valueOf(-70.24754695000001));
        editor.putInt("_id", 1);
        editor.commit();
        Intent intent = new Intent(EncuentraTuMenuActivity.this,MapaActivity.class);
        EncuentraTuMenuActivity.this.startActivity(intent);
    }

    @Override
    public void onLocationChanged(Location loc) {
        // Este metodo se ejecuta cada vez que el GPS recibe nuevas coordenadas
        // debido a la detecci—n de un cambio de ubicacion
        loc.getLatitude();
        loc.getLongitude();
        this.encuentratumenuActivity.setLocation(loc);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        // Este metodo se ejecuta cuando el GPS es activado
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Este metodo se ejecuta cuando el GPS es desactivado

    }

    public void setLocation(Location loc) {
        //Obtener la direcci—n de la calle a partir de la latitud y la longitud
        if (loc.getLatitude() != 0.0 && loc.getLongitude() != 0.0) {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> list = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
