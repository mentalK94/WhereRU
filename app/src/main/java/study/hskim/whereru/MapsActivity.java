package study.hskim.whereru;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import study.hskim.whereru.model.LocationAuth;
import study.hskim.whereru.model.User;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mGoogleMap;
    private SupportMapFragment mapFrag;
    private LocationRequest mLocationRequest;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private boolean initFlag;
    private Location mLastLocation;
    private Marker mCurrLocationMarker;
    private FusedLocationProviderClient mFusedLocationClient;
    private User user;
    private Animation fab_open, fab_close;
    private Boolean isFabOpen = false;
    private FloatingActionButton menu, people, chat, account;
    private LinearLayoutManager mLayoutManager;
    private View markerView;
    private ImageView userMarker;
    private User me = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mAuth = FirebaseAuth.getInstance();
        user = (User) getIntent().getSerializableExtra("UserInfo");
        mDatabase = FirebaseDatabase.getInstance().getReference();
        initFlag = false;

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
        // user = (User) getIntent().getSerializableExtra("UserInfo");

        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);

        menu = findViewById(R.id.floatingButton_Menu);
        people = findViewById(R.id.floatingButton_People);
        chat = findViewById(R.id.floatingButton_Chat);
        account = findViewById(R.id.floatingButton_Account);

        menu.setOnClickListener(this);
        people.setOnClickListener(this);
        chat.setOnClickListener(this);
        account.setOnClickListener(this);

        RecyclerView recyclerView = findViewById(R.id.mapsActivity_RecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new PeopleFragmentRecyclerViewAdapter());

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        recyclerView.setLayoutManager(mLayoutManager);
    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        setCustomMarkerView();

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000); // two minute interval
        mLocationRequest.setFastestInterval(10000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mGoogleMap.setMyLocationEnabled(true);
        }
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        List<User> userList;

        @Override
        public void onLocationResult(LocationResult locationResult) {
            userList = new ArrayList<>();
            List<Location> locationList = locationResult.getLocations();

            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);

                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                final FirebaseUser mUser = mAuth.getCurrentUser();
                Log.i("MapsActivity", mUser.getDisplayName());
                // 로그인한 유저의 위치정보 유저정보에 setting
                user.setCurrentLatitude(location.getLatitude());
                user.setCurrentLongitude(location.getLongitude());
                /*LatLng myReceivedLocation = new LatLng(user.getCurrentLatitude(), user.getCurrentLongitude());*/

                Log.d("MapsActivity", "setValue" + location.getLatitude() + " " + location.getLongitude());

                mDatabase.child("users").child(mUser.getUid()).setValue(user);
                mDatabase.child("users").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        mGoogleMap.clear();
                        for (final DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            final User user = snapshot.getValue(User.class);
                            final LatLng myReceivedLocation = new LatLng(user.getCurrentLatitude(), user.getCurrentLongitude());
                            /*Log.d("MapsActivity", user.getUsername());
                            Log.d("MapsActivity","getValue" + user.getCurrentLatitude() + " " + user.getCurrentLongitude());*/

      /*                      Glide.with(MapsActivity.this).load(user.getImageUri())
                                    .apply(new RequestOptions().circleCrop())
                                    .into(userMarker);*/

                            if(user.getUserId().equals(mAuth.getCurrentUser().getUid())) { // 내 계정아이디인 경우
                                me = user;
                                if(initFlag == false) {
                                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(myReceivedLocation));
                                    mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
                                    initFlag = true;
                                }
                                mGoogleMap.addMarker(new MarkerOptions().position(myReceivedLocation).title(user.getUsername())
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                            } else {
                                FirebaseDatabase.getInstance().getReference().child("locationAuth").addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        ArrayList<User> users = new ArrayList<>();

                                        for (DataSnapshot item : dataSnapshot.getChildren()) {
                                            LocationAuth locationAuth = item.getValue(LocationAuth.class);

                                            if(locationAuth.getId().equals(user.getUserId())) {
                                                if(locationAuth.denyLocationList.containsKey(mAuth.getUid())) {  // 내 아이디가 존재하는 경우
                                                    if(locationAuth.denyLocationList.get(mAuth.getUid())) { // 거부 상태
                                                        Log.d("MapsActivity", user.getUserId()+"로 부터 거부됨");
                                                    } else {  // 거부 상태가 아닌 경우
                                                        users.add(user);
                                                        //mGoogleMap.addMarker(new MarkerOptions().position(myReceivedLocation).title(user.getUsername()));
                                                    }
                                                }
                                            }
                                        }
                                        Log.d("MapsActivity", users.toString());
                                        for(User user : users) {
                                            mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(user.getCurrentLatitude(), user.getCurrentLongitude()))
                                                    .title(user.getUsername()).snippet("나와의 거리:" + String.format("%.2f", getDistance(me.getCurrentLatitude(), me.getCurrentLongitude(), user.getCurrentLatitude(), user.getCurrentLongitude())) + "m"));
                                        }


                                    }


                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });



                            }

                        }

                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        }
    };

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mGoogleMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.floatingButton_Menu:
                anim();
                break;
            case R.id.floatingButton_People:
                anim();
                Toast.makeText(this, "People", Toast.LENGTH_SHORT).show();
                //finish();
                Intent intent = new Intent(MapsActivity.this, HomeActivity.class);
                intent.putExtra("fragmentCode", 1000);
                startActivity(intent);
                finish();
                break;
            case R.id.floatingButton_Chat:
                anim();
                Toast.makeText(this, "Chat", Toast.LENGTH_SHORT).show();
                intent = new Intent(MapsActivity.this, HomeActivity.class);
                intent.putExtra("fragmentCode", 2000);
                startActivity(intent);
                finish();
                break;
            case R.id.floatingButton_Account:
                anim();
                Toast.makeText(this, "Account", Toast.LENGTH_SHORT).show();
                intent = new Intent(MapsActivity.this, HomeActivity.class);
                intent.putExtra("fragmentCode", 3000);
                startActivity(intent);
                finish();
                break;
        }


    }

    public void anim() {

        if (isFabOpen) {
            people.startAnimation(fab_close);
            chat.startAnimation(fab_close);
            account.startAnimation(fab_close);
            people.setClickable(false);
            chat.setClickable(false);
            account.setClickable(false);
            isFabOpen = false;
        } else {
            people.startAnimation(fab_open);
            chat.startAnimation(fab_open);
            account.startAnimation(fab_open);
            people.setClickable(true);
            chat.setClickable(true);
            account.setClickable(true);
            isFabOpen = true;
        }
    }


    private void setCustomMarkerView() {

        markerView = LayoutInflater.from(this).inflate(R.layout.googlemap_icon, null);
        userMarker = markerView.findViewById(R.id.googleMaps_Icon);
    }

    private Bitmap createDrawableFromView(Context context, View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    public double getDistance(double myLatitude,double myLongitude,double otherLatitude,double otherLongitude) {
        Location myLoaction = new Location("myLocation");
        Location otherLocation = new Location("otherLocation");

        myLoaction.setLatitude(myLatitude);
        myLoaction.setLongitude(myLongitude);
        otherLocation.setLatitude(otherLatitude);
        otherLocation.setLongitude(otherLongitude);

        double distance = myLoaction.distanceTo(otherLocation);

        return distance;
    }

    class PeopleFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        List<User> userList;

        public PeopleFragmentRecyclerViewAdapter() {
            userList = new ArrayList<>();
            final String myUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseDatabase.getInstance().getReference().child("users").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    userList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        User user = snapshot.getValue(User.class);
                        if(user.getUserId().equals(myUserId)) { // 검색된 아이디중 내 아이디가 있으면 리스트 추가 x
                            continue;
                        }
                        userList.add(user);
                    }
                    notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_people, parent, false);

            return new CustomViewHolder(view);
        }

        @Override // 이미지 넣어주는 메소드
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

            Glide.with(holder.itemView.getContext()).load(userList.get(position).getImageUri())
                    .apply(new RequestOptions().circleCrop())
                    .into(((CustomViewHolder)holder).imageView);
            ((CustomViewHolder)holder).textView.setText(userList.get(position).getUsername());

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    FirebaseDatabase.getInstance().getReference().child("locationAuth").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot item : dataSnapshot.getChildren()) {
                                LocationAuth locationAuth = item.getValue(LocationAuth.class);

                                if(locationAuth.getId().equals(userList.get(position).getUserId())) {
                                    if(locationAuth.denyLocationList.containsKey(mAuth.getUid())) {  // 내 아이디가 존재하는 경우
                                        if(locationAuth.denyLocationList.get(mAuth.getUid())) { // 거부 상태
                                            Toast.makeText(MapsActivity.this, userList.get(position).getUsername()+"님으로부터 위치가 거부되었습니다. ", Toast.LENGTH_SHORT).show();
                                        } else {  // 거부 상태가 아닌 경우
                                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(userList.get(position).getCurrentLatitude(),userList.get(position).getCurrentLongitude())));
                                            mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
                                        }
                                    }
                                }
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });


                }
            });
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {

            public ImageView imageView;
            public TextView textView;
            public CustomViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.peopleItem_imageView_profile);
                textView = view.findViewById(R.id.peopleItem_textView_name);
            }
        }
    }

}


