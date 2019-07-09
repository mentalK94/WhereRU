package study.hskim.whereru.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import study.hskim.whereru.R;

import study.hskim.whereru.model.LocationAuth;
import study.hskim.whereru.model.User;

public class AccountFragment extends Fragment {
    private User myUser;
    private LocationAuth locationAuth;
    private FirebaseUser myAuth;
    private FirebaseDatabase mDatabase;
    private String denyLocationListId;
    private String targetId;
    private static final String TAG = "AccountFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.accountFragment_RecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        recyclerView.setAdapter(new AccountFragmentRecyclerViewAdapter());
        myAuth = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        locationAuth = new LocationAuth();

        return view;
    }

    class AccountFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        List<User> userList;

        public AccountFragmentRecyclerViewAdapter() {

            userList = new ArrayList<>();
            FirebaseDatabase.getInstance().getReference().child("users").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    userList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        User user = snapshot.getValue(User.class);

                        if (user.getUserId().equals(myAuth.getUid())) {
                            locationAuth.setId(myAuth.getUid());
                            myUser = user;
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friendconfig, parent, false);

            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {

            Glide.with(holder.itemView.getContext()).load(userList.get(position).getImageUri())
                    .apply(new RequestOptions().circleCrop())
                    .into(((CustomViewHolder) holder).imageView);
            ((CustomViewHolder) holder).textView.setText(userList.get(position).getUsername());

            ((CustomViewHolder) holder).denySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()

            {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    targetId = userList.get(position).getUserId();

                    if (isChecked) {  // check가 되어있으면
                        locationAuth.denyLocationList.put(targetId, true);
                        Toast.makeText(getActivity(), userList.get(position).getUsername() + "님에게 위치를 거부합니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        locationAuth.denyLocationList.put(targetId, false);
                        Toast.makeText(getActivity(), userList.get(position).getUsername() + "님에게 위치를 허용합니다.", Toast.LENGTH_SHORT).show();
                    }
                    Log.d(TAG, denyLocationListId + "$!$!$");
                    if (denyLocationListId == null) {
                        FirebaseDatabase.getInstance().getReference().child("locationAuth").push()
                                .setValue(locationAuth).addOnSuccessListener(
                                new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        checkDenyLocationList();
                                    }
                                });
                    } else {
                        FirebaseDatabase.getInstance().getReference().child("locationAuth").child(denyLocationListId).child("denyLocationList")
                                .child(userList.get(position).getUserId()).setValue(locationAuth.denyLocationList.get(userList.get(position).getUserId())).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                            }
                        });
                    }

                }
            });

            checkDenyLocationList();
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView;
            public Switch denySwitch;

            public CustomViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.friendConfigItemImageView);
                textView = view.findViewById(R.id.friendConfigItemTextView);
                denySwitch = view.findViewById(R.id.friendConfigItemSwitch);
            }
        }

    }

    private void checkDenyLocationList() {

        FirebaseDatabase.getInstance().getReference().child("locationAuth")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot item : dataSnapshot.getChildren()) {
                            LocationAuth locationAuth = item.getValue(LocationAuth.class);
                            if(locationAuth.getId().equals(myAuth.getUid())) {
                                denyLocationListId = item.getKey();
                            }
                            if(locationAuth.getId().equals(myUser.getUserId())) {
                                Set set = locationAuth.denyLocationList.keySet();
                                Iterator iterator = set.iterator();

                                while(iterator.hasNext()) {
                                    String key = (String) iterator.next();
                                    myUser.denyLocationList.put(key, locationAuth.denyLocationList.get(key));
                                }
                            }


                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }
}
