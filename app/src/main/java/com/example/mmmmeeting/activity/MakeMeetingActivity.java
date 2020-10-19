package com.example.mmmmeeting.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.mmmmeeting.Info.MeetingInfo;
import com.example.mmmmeeting.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MakeMeetingActivity extends BasicActivity implements View.OnClickListener {
    Button makeMeeting;
    Button back1;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_meeting);
        makeMeeting = findViewById(R.id.makeMeetingBtn);
        back1 = findViewById(R.id.backBtn1);

        makeMeeting.setOnClickListener(this);
        back1.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.makeMeetingBtn:
                // 모임 이름이 이미 존재하는지 확인 메서드.. 나중에..추가

                // 모임 정보 저장
                meetingUpdate();
                break;

            case R.id.backBtn1:
                myStartActivity(MainActivity.class);
                finish();
                break;
        }

    }

    private void meetingUpdate(){
        String name = ((EditText)findViewById(R.id.makeMeetingText)).getText().toString();
        String description = ((EditText)findViewById(R.id.meetingDesc)).getText().toString();

        // 모임 이름의 길이가 0보다 큰경우 = 모임의 이름이 입력된 경우
        if(name.length()>0) {
            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // db에 저장할 모임 정보 객체 생성
            MeetingInfo info = new MeetingInfo(name,description);
            info.setUserID(user.getUid());

            if (user != null) {
                // meeting table에 미팅 정보 저장
                db.collection("meetings").document().set(info)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // 미팅 정보 저장 = 메시지 출력 후 메인으로 복귀
                                startToast("미팅 생성에 성공하였습니다.");
                                myStartActivity(MainActivity.class);
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                startToast("미팅 생성에 실패하였습니다.");
                            }
                        });
            } else {
                startToast("회원정보를 입력해주세요.");
            }
        }
    }

    private void startToast(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }

    private void myStartActivity(Class c){
        Intent intent = new Intent(this,c);
        intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}