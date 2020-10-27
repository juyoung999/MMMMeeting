package com.example.mmmmeeting.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mmmmeeting.Info.ChatItem;
import com.example.mmmmeeting.Info.MemberInfo;
import com.example.mmmmeeting.R;
import com.example.mmmmeeting.adapter.ChatAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class FragChat extends Fragment {
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference chatRef;
    private MemberInfo memberInfo;
    private String userName;

    private EditText et;
    private ListView listView;

    private ArrayList<ChatItem> messageItems=new ArrayList<>();
    private ChatAdapter adapter;

    public FragChat(){}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_chat, container, false);
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userName = user.getDisplayName();

        et=view.findViewById(R.id.et);
        listView=view.findViewById(R.id.listview);
        adapter=new ChatAdapter(messageItems,getLayoutInflater());
        listView.setAdapter(adapter);

        firebaseDatabase= FirebaseDatabase.getInstance();
        chatRef= firebaseDatabase.getReference("chat");

        //RealtimeDB에서 채팅 메세지들 실시간 읽어오기..
        //'chat'노드에 저장되어 있는 데이터들을 읽어오기
        //chatRef에 데이터가 변경되는 것을 듣는 리스너 추가
        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                //새로 추가된 데이터(값 : MessageItem객체) 가져오기
                ChatItem messageItem= dataSnapshot.getValue(ChatItem.class);

                //새로운 메세지를 리스뷰에 추가하기 위해 ArrayList에 추가
                messageItems.add(messageItem);

                //리스트뷰를 갱신
                adapter.notifyDataSetChanged();
                listView.setSelection(messageItems.size()-1); //리스트뷰의 마지막 위치로 스크롤 위치 이동
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Button button = (Button) view.findViewById(R.id.msgBtn);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                clickSend(v);
            }
        });

        return view;
    }


    public void clickSend(View view) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        String nickName= userName;
        String message= et.getText().toString();

        //메세지 작성 시간 문자열로
        Calendar calendar= Calendar.getInstance();
        String time = timeFormat.format(calendar.getTime());

        //DB에 저장할 값들(닉네임, 메세지, 시간)
        ChatItem messageItem= new ChatItem(nickName,message,time);
        chatRef.push().setValue(messageItem);

        //EditText에 있는 글씨 지우기
        et.setText("");

        //소프트키패드 안보이도록
        InputMethodManager imm=(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),0);

    }

}
