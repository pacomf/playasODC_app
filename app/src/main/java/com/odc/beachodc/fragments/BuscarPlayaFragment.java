package com.odc.beachodc.fragments;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facebook.android.Util;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.odc.beachodc.R;
import com.odc.beachodc.activities.BuscarPlaya;
import com.odc.beachodc.activities.ResultadoBusquedaPlaya;
import com.odc.beachodc.interfaces.IStandardTaskListener;
import com.odc.beachodc.utilities.Utilities;
import com.odc.beachodc.utilities.ValidacionPlaya;
import com.odc.beachodc.utilities.placeAutocomplete.DetailsPlaceOne;
import com.odc.beachodc.utilities.placeAutocomplete.FillPlace;
import com.odc.beachodc.webservices.Request;

import java.util.List;
import java.util.Locale;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;


/**
 * Created by Paco on 7/01/14.
 * Fragment que se encarga de Loguear al usuario, es el splash screen inicial de login
 */
public class BuscarPlayaFragment extends Fragment {

        View rootView;
        private DetailsPlaceOne geoLugar;
        private FillPlace buscarLugar;
        private Thread thread;
        private ProgressDialog pd;
        GoogleMap mapa;
        Location myLocation;
        Button buscarBTN;
        RelativeLayout groupAddress;

        public BuscarPlayaFragment() {
            // Se ejecuta antes que el onCreateView

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            // Para evitar problemas al inflar Layout con Map Fragments... y reutilizar mejor los fragments
            if (rootView != null) {
                ViewGroup parent = (ViewGroup) rootView.getParent();
                if (parent != null)
                    parent.removeView(rootView);
            }
            try {
                rootView = inflater.inflate(R.layout.fragment_buscar_playas, container, false);
            } catch (InflateException e) {}

            // Empezar aqui a trabajar con la UI

            ValidacionPlaya.busqueda = (RadioGroup) rootView.findViewById(R.id.searchRG);
            ValidacionPlaya.nombrePlaya = (EditText) rootView.findViewById(R.id.nombreET);

            groupAddress = (RelativeLayout) rootView.findViewById(R.id.groupSearchByAddress);

            try {
                MapsInitializer.initialize(getActivity());
                initMapa();
            } catch (GooglePlayServicesNotAvailableException e) {}

            ValidacionPlaya.direccion = (AutoCompleteTextView) rootView.findViewById(R.id.addressET);

            ValidacionPlaya.porCercania = null;

            initAutocompletadoDireccion();

            buscarBTN = (Button) rootView.findViewById(R.id.buscarBTN);

            buscarBTN.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utilities.buscarPlaya (ValidacionPlaya.busqueda, ValidacionPlaya.nombrePlaya, getActivity(), ValidacionPlaya.porCercania, ValidacionPlaya.direccion, ValidacionPlaya.playa);
                }
            });

            ValidacionPlaya.busqueda.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (checkedId == R.id.searchByName) {
                        ocultarBusquedaNombre(false);
                    } else if (checkedId == R.id.searchByAddress){
                        ocultarBusquedaNombre(true);
                    } else {
                        ocultarBusquedaNombre(false);
                    }
                }
            });

            return rootView;

            }

    private void initMapa(){

        mapa = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        if (mapa==null)
            return;
        mapa.setMyLocationEnabled(true);
        mapa.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {

            @Override
            public void onMyLocationChange(Location arg0) {
                myLocation=arg0;
            }
        });

        mapa.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if (myLocation != null){
                    try {
                        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        try {
                            imm.hideSoftInputFromWindow(getActivity().getWindow().getCurrentFocus().getWindowToken(), 0);
                        } catch (Exception e){}
                        Geocoder geoCoder = new Geocoder(getActivity(), Locale.getDefault());
                        Double lat = myLocation.getLatitude();
                        Double lng = myLocation.getLongitude();
                        List<Address> addresses = geoCoder.getFromLocation(lat, lng, 1);
                        String add = "";
                        if (addresses.size() > 0){
                            for (int i=0; i<addresses.get(0).getMaxAddressLineIndex();i++)
                                add += addresses.get(0).getAddressLine(i) + " ";
                        }

                        mapa.clear();

                        ValidacionPlaya.direccion.setText(add);

                        ValidacionPlaya.porCercania = new LatLng(lat, lng);

                        mapa.addMarker(new MarkerOptions()
                                .position(new LatLng(lat, lng))
                                .title(add)
                                .draggable(true)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.aqui)));

                    } catch (Exception e) {}
                }
                return false;
            }
        });

        mapa.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            public void onMapLongClick(LatLng point) {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                try {
                    imm.hideSoftInputFromWindow(getActivity().getWindow().getCurrentFocus().getWindowToken(), 0);
                } catch (Exception e){}
                Geocoder geoCoder = new Geocoder(getActivity(), Locale.getDefault());
                try {
                    List<Address> addresses = geoCoder.getFromLocation(point.latitude, point.longitude, 1);
                    String add = "";
                    if (addresses.size() > 0){
                        for (int i=0; i<addresses.get(0).getMaxAddressLineIndex();i++)
                            add += addresses.get(0).getAddressLine(i) + " ";
                    }
                    ValidacionPlaya.direccion.setText(add);

                    ValidacionPlaya.porCercania = new LatLng(point.latitude, point.longitude);

                    mapa.clear();

                    // Insertamos el Marcador
                    mapa.addMarker(new MarkerOptions()
                            .position(new LatLng(point.latitude, point.longitude))
                            .title(add)
                            .draggable(true)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.aqui)));
                } catch (Exception e) {
                }

            }
        });

        mapa.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {

            @Override
            public void onMarkerDragStart(Marker marker) {
                marker.hideInfoWindow();
                marker.setTitle("");
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                LatLng newPosition = marker.getPosition();
                Geocoder geoCoder = new Geocoder(getActivity(), Locale.getDefault());
                try {
                    List<Address> addresses = geoCoder.getFromLocation(newPosition.latitude, newPosition.longitude, 1);
                    String add = "";
                    if (addresses.size() > 0){
                        for (int i=0; i<addresses.get(0).getMaxAddressLineIndex();i++)
                            add += addresses.get(0).getAddressLine(i) + " ";
                    }

                    mapa.clear();

                    mapa.addMarker(new MarkerOptions()
                            .position(new LatLng(newPosition.latitude, newPosition.longitude))
                            .title(add)
                            .draggable(true)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.aqui)));

                    ValidacionPlaya.direccion.setText(add);

                    ValidacionPlaya.porCercania = new LatLng(newPosition.latitude, newPosition.longitude);

                } catch (Exception e) {
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {}
        });
    }

    public void initAutocompletadoDireccion(){
        final ArrayAdapter<String> adapterFrom = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
        adapterFrom.setNotifyOnChange(true);
        ValidacionPlaya.direccion.setAdapter(adapterFrom);

        ValidacionPlaya.direccion.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                geoLugar = new DetailsPlaceOne();
                geoLugar.setContext(arg1.getContext());
                thread=  new Thread(){
                    @Override
                    public void run(){
                        try {
                            synchronized(this){
                                wait(700);
                            }
                        }
                        catch(InterruptedException ex){
                        }
                        pd.dismiss();

                        if ((buscarLugar.referencesPlace.isEmpty()) || (buscarLugar.referencesPlace.get(ValidacionPlaya.direccion.getText().toString()) == null)){
                            showToastError();
                        } else {
                            geoLugar.setListener(new PlaceToPointMap_TaskListener(ValidacionPlaya.direccion.getText().toString()));
                            geoLugar.execute(buscarLugar.referencesPlace.get(ValidacionPlaya.direccion.getText().toString()));
                        }
                    }
                };
                pd = ProgressDialog.show(getActivity(), getResources().getText(R.string.procesando), getResources().getText(R.string.esperar));
                pd.setIndeterminate(false);
                pd.setCancelable(true);
                thread.start();
            }
        });

        // Monitorizamos el evento de cambio en el campo a autocompletar para buscar las propuestas de autocompletado
        ValidacionPlaya.direccion.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Calculamos el autocompletado
                if (count%3 == 1){
                    adapterFrom.clear();
                    // Ejecutamos en segundo plano la busqueda de propuestas de autocompletado
                    buscarLugar = new FillPlace(adapterFrom, ValidacionPlaya.direccion, getActivity());
                    buscarLugar.execute(ValidacionPlaya.direccion.getText().toString());
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
        });
    }

    private class PlaceToPointMap_TaskListener implements IStandardTaskListener {

        private String markerStr;

        public PlaceToPointMap_TaskListener(String markerStr) {
            this.markerStr =  markerStr;
        }

        @Override
        public void taskComplete(Object result) {
            if ((Boolean)result){
                Double lat, lng;
                // Esconder teclado para que se vea la animación del mapa
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                try {
                    imm.hideSoftInputFromWindow(getActivity().getWindow().getCurrentFocus().getWindowToken(), 0);
                } catch (Exception e){}
                // Recogemos la posicion que hemos pedido al Web Service de Google Place
                lat = geoLugar.coordinates.get("lat");
                lng = geoLugar.coordinates.get("lng");
                if ((lat == null) || (lng == null))
                    return;
                else{
                    ValidacionPlaya.porCercania = new LatLng(lat, lng);
                }
                LatLng go = new LatLng(lat, lng);

                ValidacionPlaya.direccion.setText(markerStr);

                // Creamos la animación de movimiento hacia el lugar que hemos introducido
                CameraPosition camPos = new CameraPosition.Builder()
                        .target(go)
                        .zoom(18)         //Establecemos el zoom en 17
                        .tilt(45)         //Bajamos el punto de vista de la cámara 70 grados
                        .build();

                // Lanzamos el movimiento
                CameraUpdate camUpd = CameraUpdateFactory.newCameraPosition(camPos);
                mapa.animateCamera(camUpd);

                mapa.clear();

                mapa.addMarker(new MarkerOptions()
                        .position(new LatLng(lat, lng))
                        .title(markerStr)
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.aqui)));
            }
        }
    }

    public void showToastError(){
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getActivity(), R.string.error_problemautocompletado, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void ocultarBusquedaNombre (boolean ocultar){
        try {
            if (ocultar) {
                ValidacionPlaya.nombrePlaya.setVisibility(View.GONE);
                groupAddress.setVisibility(View.VISIBLE);
            } else {
                groupAddress.setVisibility(View.GONE);
                ValidacionPlaya.nombrePlaya.setVisibility(View.VISIBLE);
            }
        } catch (Exception e){}
    }

}
