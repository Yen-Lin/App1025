package com.koddev.authenticatorapp.chat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.koddev.authenticatorapp.R;
import com.koddev.authenticatorapp.chat.util.Define;
import com.koddev.authenticatorapp.users.AdapterUsers;
import com.koddev.authenticatorapp.users.ModelUser;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ChatActivity extends AppCompatActivity
{
    private String mUserUUID="";
    private String mUserName="";

    private RecyclerView mRecyclerView;
    private ImageView mButtonSendIV;
    private EditText mEditTextMessage;

    private TextView mNameTV;

    private ChatMessageAdapter mAdapter;

    private ArrayList<ChatMessage> mChatMessageArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_layout);


        if(getIntent().getExtras()!=null)
        {
            mUserUUID = getIntent().getExtras().getString(Define.UserUUID);

            mUserName = getIntent().getExtras().getString(Define.UserName);
        }

        mNameTV = findViewById(R.id.textview_name);
        mNameTV.setText(mUserName);
        mChatMessageArrayList = new ArrayList<>();

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mButtonSendIV = (ImageView) findViewById(R.id.btn_send);
        mEditTextMessage = (EditText) findViewById(R.id.et_message);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));//設定樣式

        mAdapter = new ChatMessageAdapter(this, new ArrayList<ChatMessage>());
        mRecyclerView.setAdapter(mAdapter);

        mButtonSendIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mEditTextMessage.getText().toString();
                if (TextUtils.isEmpty(message)) {
                    return;
                }
                String createTime = getDateTime();
                sendToFirebase(message,createTime);//
                mEditTextMessage.setText("");
            }
        });

        init();
    }

    private void init()
    {
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

//


        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(fUser.getUid()).child(Define.ChatContent).child(mUserUUID);
        ref.addValueEventListener(new ValueEventListener() {  
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                for (DataSnapshot  ds: dataSnapshot.getChildren()){
                    HashMap<Object,String> contentHashMap = (HashMap<Object,String>)ds.getValue();
                    String time = ds.getKey();

                    boolean isMine = false;
                    boolean isCanAdd = true;

                    if(contentHashMap.get(Define.IsMine).equals("true"))
                        isMine = true;

                    for(int i=0;i<mAdapter.getMessage().size();i++)
                    {
                        if(mAdapter.getMessage().get(i).dateTime().equals(time))
                        {
                            isCanAdd = false;
                            break;
                        }
                    }

                    if(isCanAdd)
                    {
                        ChatMessage chatMessage = new ChatMessage(contentHashMap.get(Define.Sentence),time,isMine,false);
                        mAdapter.add(chatMessage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private String getDateTime()
    {
        String createTimeTemp = Define.convertUnixTimeToString(Define.getCurrentUnixTime(), "yyyyMMddhhmmss");
        String createTime = "";

        try
        {
            //Local
            createTime = Define.changeDateFormat(createTimeTemp, "yyyyMMddhhmmss", TimeZone.getTimeZone("UTC"), "yyyy-MM-dd:HH:mm:ss", TimeZone.getDefault());
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        return createTime;
    }

    private void sendToFirebase(String message,String createTime)
    {
        FirebaseDatabase database=FirebaseDatabase.getInstance();
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

        //使用者自己
        DatabaseReference mine =  database.getReference("Users").child(fUser.getUid()).child(Define.ChatContent).child(mUserUUID).child(createTime);
        //聊天的對象
        DatabaseReference friend =  database.getReference("Users").child(mUserUUID).child(Define.ChatContent).child(fUser.getUid()).child(createTime);

        //New Firebase db
        DatabaseReference chatsDB = database.getReference(Define.Chats);
        HashMap<Object,String>hashMapChats=new HashMap<>();

        hashMapChats.put(Define.isSeen, "true");
        hashMapChats.put(Define.message,message);
        hashMapChats.put(Define.receiver,mUserUUID);
        hashMapChats.put(Define.sender,mUserUUID);
        hashMapChats.put(Define.timestamp,String.valueOf(Define.getCurrentUnixTime()));
        chatsDB.child(mUserUUID).setValue(hashMapChats);

        HashMap<Object,String> mineHashMap=new HashMap<>();
        HashMap<Object,String> friendHashMap=new HashMap<>();

        mineHashMap.put(Define.Sentence, message);
        mineHashMap.put(Define.IsMine, "true");

        friendHashMap.put(Define.Sentence, message);
        friendHashMap.put(Define.IsMine, "false");

        mine.setValue(mineHashMap);
        friend.setValue(friendHashMap);
    }

    private void sendMessage(String message,String aDateTime) {
        ChatMessage chatMessage = new ChatMessage(message,aDateTime, true, false);
        mAdapter.add(chatMessage);
    }

    private void mimicOtherMessage(String message,String aDateTime) {
        ChatMessage chatMessage = new ChatMessage(message,aDateTime, false, false);
        mAdapter.add(chatMessage);

        mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    private void sendMessage() {
        ChatMessage chatMessage = new ChatMessage(null,null, true, true);
        mAdapter.add(chatMessage);

        mimicOtherMessage();
    }

    private void mimicOtherMessage() {
        ChatMessage chatMessage = new ChatMessage(null,null, false, true);
        mAdapter.add(chatMessage);

        mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
    }
}