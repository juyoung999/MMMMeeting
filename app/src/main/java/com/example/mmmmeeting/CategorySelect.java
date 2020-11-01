package com.example.mmmmeeting;



import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.example.mmmmeeting.Info.MeetingInfo;
import com.example.mmmmeeting.Info.MemberInfo;
import com.example.mmmmeeting.activity.MeetingActivity;
import com.example.mmmmeeting.activity.MeetingInfoActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.Map;

import androidx.annotation.NonNull;

public class CategorySelect {

    private String Tag = "category Test";
    String name;
    String category="";
    ArrayList<Float[]> userRatings =new ArrayList<>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CategorySelect(String name){
        this.name = name;
    }

    Handler mHandler = new Handler(){
        public void handleMessage(android.os.Message msg)
        {
            Bundle bd = msg.getData( ) ;            /// 전달 받은 메세지에서 번들을 받음
            String str = bd.getString( "arg" ) ;    /// 번들에 들어있는 값 꺼냄
            sendCategory(str) ;
            Log.d(Tag,"Send is "+ str);
        } ;
    } ;

    public void select(){
        //1. DB에서 별점 읽어오기 (meeting 에서 -> ui목록 접근 -> ui의 별점 읽기)
        meetingFind();
        // 다음 동작- 가중치
        Runnable r = new Runnable() {
            @Override
            public void run() {
                addWeight();
                getHighest(calAvg());
                Log.d(Tag, category);

                Bundle bd = new Bundle();      /// 번들 생성
                bd.putString("arg", category);    /// 번들에 값 넣기
                Message msg = mHandler.obtainMessage();   /// 핸들에 전달할 메세지 구조체 받기
                msg.setData(bd);                     /// 메세지에 번들 넣기
                mHandler.sendMessage(msg);
            }
        };

        mHandler.postDelayed(r, 600); // 0.6초후
    }

    private String sendCategory(String str) {
 // 액티비티에 추가..
//        Intent intent = new Intent(this, MeetingInfoActivity.class);
        return str;
    }


    // 1-1. 미팅 이름으로 사용자 테이블 접근
    private void meetingFind() {
        db.collection("meetings").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            //모든 document 출력 (dou id + data arr { : , ... ,  })
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // 모임 이름이 같은 경우 해당 모임의 코드를 텍스트뷰에 출력
                                if (document.get("name").toString().equals(name)) {
                                    // 찾은 모임의 사용자 테이블로
                                    List<String> users = (List<String>) document.get("userID");
                                    userRating(users);
                                    break;
                                }
                            }
                        } else {
                            Log.d(Tag, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    // 1-2 사용자 별점 가져오기
    private void userRating(List<String> userID) {
        for(int i=0; i<userID.size(); i++){
            DocumentReference docRef = db.collection("users").document(userID.get(i));

            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            MemberInfo user = document.toObject(MemberInfo.class);
                            Map<String, Float> rating = user.getRating();
//                            Log.d(Tag,document.getId() + "> get rating: "+ rating);
                            MapToArray(rating);
                        }
                    } else {
                        Log.d(Tag, "Task Fail : " + task.getException());
                    }
                }
            });
        }
    }

    // 1-3 맵 -> 배열로 변경 (계산 편리)
    private void MapToArray(Map<String, Float> rating) {
        Float[] temp = new Float[rating.size()];
        int i = 0;
        // 배열에 저장
        for (String key : rating.keySet()) {
            Float value = rating.get(key);
            temp[i++]=value; // cafe, subway, restaurant ,shopping
        }
        this.userRatings.add(temp);
    }

    // 2-1 가중치 계산
    private void addWeight() {
//        Log.d(Tag,"userRating size is :" +userRatings.size() );

        for(int i=0; i< userRatings.size(); i++){
            Log.d(Tag,Arrays.toString(userRatings.get(i)));
            // 2. 분산 구하기
            double std = calVariance(userRatings.get(i));
            if (std > 1.15) {  // 현재 설정 : 표준편차 값이 1.15 이상인 경우 카테고리의 점수 조정
                // 사용자 별점 업데이트
                this.userRatings.set(i,ratingUpdate(userRatings.get(i)));
            }
        }
    }

    // 2-1. 분산 계산 -> 표준편차 계산
    private double calVariance(Float[] rating) {
        // 분산 = 편차 제곱의 합 / 변량의 수  => (편차 = 값- 평균)
        double avg = 0;

        // 1. 평균 구하기
        for (int i = 0; i < rating.length; i++) {
            avg += rating[i];
        }
        avg /= rating.length;

        //2. 편차 제곱
        double variance = 0;
        for (int i = 0; i < rating.length; i++) {
            variance += (rating[i] - avg) * (rating[i] - avg);
        }

        // 3. 분산
        variance /= rating.length;

        // +) 표준 편차
        double std = Math.sqrt(variance);

        Log.d(Tag, "variance: " + variance + ",  standard deviation: " + std);

        return std;

    }

    // 2-2. 가중치 설정
    private Float[] ratingUpdate(Float[] rating) {
        float high = rating[0];
        float low = rating[0];
        int indexH = 0;
        int indexL = 0;

        for (int i = 0; i < rating.length; i++) {
            if (high < rating[i]) {
                high = rating[i];
                indexH = i;
            }
            if (low > rating[i]) {
                low = rating[i];
                indexL = i;
            }
        }
        // 가장 좋아하는 카테고리 점수에 가산점, 싫어하는건 감점
        rating[indexH] = rating[indexH] * 1.2f;
        rating[indexL] = rating[indexL] * 0.8f;

        Log.d(Tag, "After update rating is " + Arrays.toString(rating));

        return rating;
    }

    // 3-1 평균
    private Float[] calAvg() {
        Float[] avgRating = new Float[userRatings.get(0).length];
        for(int i=0; i<userRatings.get(0).length; i++){ // 카테고리마다
            Float sum=0f;
            for(int j=0; j<userRatings.size(); j++){ // 사용자 평가 합
                sum +=userRatings.get(j)[i];
            }
            avgRating[i]=sum/avgRating.length;
        }
        Log.d(Tag, Arrays.toString(avgRating));
        return avgRating;
    }

    // 3-2 최고값
    private void getHighest(Float[] avgRating) {
        float high = avgRating[0];
        int index=0;
        for (int i = 0; i < avgRating.length; i++) {
            if (high < avgRating[i]) {
                high = avgRating[i];
                index = i;
            }
        }

        // 0 = 카페 , 1 = 역, 2 = 음식점, 3 = 쇼핑몰
        switch (index) {
            case 0: this.category="카페"; break;
            case 1: this.category= "역"; break;
            case 2: this.category= "음식점"; break;
            case 3: this.category= "쇼핑몰"; break;
        }
    }

}
