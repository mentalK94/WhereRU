package study.hskim.whereru;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import study.hskim.whereru.model.Chat;
import study.hskim.whereru.model.User;

public class MessengerActivity extends AppCompatActivity {

    private static final String TAG = "MESSENGER_1000";
    private String targetId;
    private Button button;
    private EditText editText;

    private String userId;
    private String chatRoomUserId;

    private RecyclerView recyclerView;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messenger);

        recyclerView = findViewById(R.id.messageActivity_recyclerView);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();  // 단말기에 로그인된 id

        // 눌려진 아이디 정보 받아옴
        targetId = getIntent().getStringExtra("targetID");  // 상대방 id

        button = findViewById(R.id.messageButton);
        editText = findViewById(R.id.messageEditText);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Chat chat = new Chat();
                chat.users.put(userId, true);
                chat.users.put(targetId, true);

                if (chatRoomUserId == null) { // 생성된 채팅방이 없는경우
                    button.setEnabled(false);
                    FirebaseDatabase.getInstance().getReference().child("chatRooms").push().setValue(chat).addOnSuccessListener(
                            new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    checkChatRoom();
                                }
                            });

                } else { // 이미 채팅방이 있는경우
                    Chat.ChatMessage chattingMessage = new Chat.ChatMessage();
                    chattingMessage.userId = userId;
                    chattingMessage.chatMessage = editText.getText().toString();
                    chattingMessage.timestamp = ServerValue.TIMESTAMP;

                    // 메시지 내용 firebase에 저장
                    FirebaseDatabase.getInstance().getReference().child("chatRooms").child(chatRoomUserId).child("messages")
                            .push().setValue(chattingMessage).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // 메시지 저장에 성공한 경우 콜백메서드 처리
                            editText.setText("");
                        }
                    });
                }
            }
        });
        checkChatRoom();
    }

    public void checkChatRoom() { // 채팅방이 이미 있는 경우 생성 방지
        FirebaseDatabase.getInstance().getReference().child("chatRooms").orderByChild("users/" + userId).equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot item : dataSnapshot.getChildren()) {
                            Chat chat = item.getValue(Chat.class);
                            if (chat.users.containsKey(targetId)) {
                                chatRoomUserId = item.getKey();
                                button.setEnabled(true);
                                recyclerView.setLayoutManager(new LinearLayoutManager(MessengerActivity.this));
                                recyclerView.setAdapter(new RecyclerViewAdapter());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        List<Chat.ChatMessage> chatMessages;
        User targetUser;

        public RecyclerViewAdapter() {
            chatMessages = new ArrayList<>();

            FirebaseDatabase.getInstance().getReference().child("users").child(targetId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Log.d(TAG, dataSnapshot.toString());
                    targetUser = dataSnapshot.getValue(User.class);

                    getMessageList();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }

        public void getMessageList() {
            FirebaseDatabase.getInstance().getReference().child("chatRooms").child(chatRoomUserId)
                    .child("messages").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    chatMessages.clear();

                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        chatMessages.add(item.getValue(Chat.ChatMessage.class));
                    }
                    // data 갱신
                    notifyDataSetChanged();

                    // 스크롤 갱신
                    recyclerView.scrollToPosition(chatMessages.size() - 1);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);

            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MessageViewHolder messageViewHolder = ((MessageViewHolder) holder);

            if (chatMessages.get(position).userId.equals(userId)) {  // 자기자신의 메시지인 경우

                messageViewHolder.textViewMessage.setText(chatMessages.get(position).chatMessage);
                messageViewHolder.textViewMessage.setBackgroundResource(R.drawable.message_send_bubble);
                messageViewHolder.linearLayout_target.setVisibility(View.INVISIBLE);
                messageViewHolder.textViewMessage.setTextSize(20);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);

            } else {  // 상대방의 메시지인 경우
                if(targetUser != null) {
                    Glide.with(holder.itemView.getContext())
                            .load(targetUser.getImageUri())
                            .apply(new RequestOptions().circleCrop())
                            .into(messageViewHolder.imageViewProfile);
                    messageViewHolder.textViewName.setText(targetUser.getUsername());
                }

                messageViewHolder.linearLayout_target.setVisibility(View.VISIBLE);
                messageViewHolder.textViewMessage.setBackgroundResource(R.drawable.message_receive_bubble);
                messageViewHolder.textViewMessage.setText(chatMessages.get(position).chatMessage);
                messageViewHolder.textViewMessage.setTextSize(20);
                messageViewHolder.linearLayout_main.setGravity(Gravity.LEFT);

            }
            long unixTime = (long) chatMessages.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            messageViewHolder.timestamp.setText(time);

        }

        @Override
        public int getItemCount() {
            return chatMessages.size();
        }

        private class MessageViewHolder extends RecyclerView.ViewHolder {
            public TextView textViewMessage;
            public TextView textViewName;
            public ImageView imageViewProfile;
            public LinearLayout linearLayout_target;
            public LinearLayout linearLayout_main;
            public TextView timestamp;

            public MessageViewHolder(View view) {
                super(view);
                textViewMessage = view.findViewById(R.id.messageItem_messageTextView);
                textViewName = view.findViewById(R.id.messageItem_textView_name);
                imageViewProfile = view.findViewById(R.id.messageItem_imageView_profile);
                linearLayout_target = view.findViewById(R.id.messageItem_linearLayout_target);
                linearLayout_main = view.findViewById(R.id.messageItem_linearLayout);
                timestamp = view.findViewById(R.id.messageItem_timestamp);
            }
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
