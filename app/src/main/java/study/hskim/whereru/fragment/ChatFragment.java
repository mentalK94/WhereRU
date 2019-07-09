package study.hskim.whereru.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import study.hskim.whereru.MessengerActivity;
import study.hskim.whereru.R;
import study.hskim.whereru.model.Chat;
import study.hskim.whereru.model.User;

public class ChatFragment extends Fragment {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.chatFragment_RecyclerView);
        recyclerView.setAdapter(new ChatRecyclerViewAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        return view;
    }

    class ChatRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private String userId;
        private List<Chat> chatList = new ArrayList<>();
        private ArrayList<String> targetUsers = new ArrayList<>();


        public ChatRecyclerViewAdapter() {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseDatabase.getInstance().getReference().child("chatRooms").orderByChild("users/"+userId).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    chatList.clear();
                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        chatList.add(item.getValue(Chat.class));
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);

            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            final CustomViewHolder customViewHolder = (CustomViewHolder)holder;

            String targetUid = null;

            // 채팅방에 있는 유저들 체크
            for(String user : chatList.get(position).users.keySet()) {
                if(!user.equals(userId)) {
                    targetUid = user;
                    targetUsers.add(targetUid);
                }
            }
            FirebaseDatabase.getInstance().getReference().child("users").child(targetUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    Glide.with(customViewHolder.itemView.getContext())
                            .load(user.getImageUri())
                            .apply(new RequestOptions().circleCrop())
                            .into(customViewHolder.imageView);

                    customViewHolder.chatRoomTitle.setText(user.getUsername());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            // 메시지를 내림차순으로 정렬 후 마지막 메시지 값 얻어옴
            Map<String, Chat.ChatMessage> messageMap = new TreeMap<>(Collections.<String>reverseOrder());
            messageMap.putAll(chatList.get(position).messages);
            String lastMessageKey = (String)messageMap.keySet().toArray()[0];
            customViewHolder.lastMessage.setText(chatList.get(position).messages.get(lastMessageKey).chatMessage);

            customViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(view.getContext(), MessengerActivity.class);
                    intent.putExtra("targetID", targetUsers.get(position));

                    startActivity(intent);
                }
            });
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            long unixTime = (long) chatList.get(position).messages.get(lastMessageKey).timestamp;
            Date date = new Date(unixTime);
            customViewHolder.timestamp.setText(simpleDateFormat.format(date));

        }

        @Override
        public int getItemCount() {
            return chatList.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {

            public ImageView imageView;
            public TextView chatRoomTitle;
            public TextView lastMessage;
            public TextView timestamp;

            public CustomViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.chatItem_ImageView);
                chatRoomTitle = view.findViewById(R.id.chatItem_ChatRoomTitle);
                lastMessage = view.findViewById(R.id.chatItem_LastMessage);
                timestamp = view.findViewById(R.id.chatItem_timestamp);
            }
        }
    }

}
