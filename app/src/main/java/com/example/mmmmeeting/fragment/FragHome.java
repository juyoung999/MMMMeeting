// 약속 목록 메인 화면
package com.example.mmmmeeting.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mmmmeeting.Info.ScheduleInfo;
import com.example.mmmmeeting.OnScheduleListener;
import com.example.mmmmeeting.R;
import com.example.mmmmeeting.activity.MainActivity;
import com.example.mmmmeeting.activity.MakeScheduleActivity;
import com.example.mmmmeeting.adapter.ScheduleAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;

public class FragHome extends Fragment {
    private static final String TAG = "HomeFragment";
    private FirebaseFirestore firebaseFirestore;
    private ScheduleAdapter scheduleAdapter;
    private ArrayList<ScheduleInfo> postList;
    private boolean updating;
    private boolean topScrolled;
    private TextView name;
    private String meetingName;

    public FragHome() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.frag_home, container, false);
        name = (TextView)view.findViewById(R.id.schedule_name);
        name.setText("약속 목록");

        Bundle bundle = this.getArguments();
        if(bundle != null) {
            bundle = getArguments();
            meetingName = bundle.getString("Name");
        }
        Log.d("get Name Test: ", meetingName);

        firebaseFirestore = FirebaseFirestore.getInstance();
        postList = new ArrayList<>();
        // scheduleAdapter가 게시글을 카드뷰로 보여줌
        scheduleAdapter = new ScheduleAdapter(getActivity(), postList);
        scheduleAdapter.setOnPostListener(onPostListener);

        // 스크롤 되는 recyclerView에 실시간으로 게시글이 올라옴
        final RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        view.findViewById(R.id.write_schedule).setOnClickListener(onClickListener);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(scheduleAdapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                int firstVisibleItemPosition = ((LinearLayoutManager)layoutManager).findFirstVisibleItemPosition();

                if(newState == 1 && firstVisibleItemPosition == 0){
                    topScrolled = true;
                }
                if(newState == 0 && topScrolled){
                    postsUpdate(true);
                    topScrolled = false;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy){
                super.onScrolled(recyclerView, dx, dy);

                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = ((LinearLayoutManager)layoutManager).findFirstVisibleItemPosition();
                int lastVisibleItemPosition = ((LinearLayoutManager)layoutManager).findLastVisibleItemPosition();

                if(totalItemCount - 3 <= lastVisibleItemPosition && !updating){
                    postsUpdate(false);
                }

                if(0 < firstVisibleItemPosition){
                    topScrolled = false;
                }
            }
        });

        postsUpdate(false);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPause(){
        super.onPause();
        scheduleAdapter.playerStop();
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                // +버튼 누르면 약속 생성
                case R.id.write_schedule:
                    myStartActivity(MakeScheduleActivity.class);
                    break;
            }
        }
    };

    OnScheduleListener onPostListener = new OnScheduleListener() {
        @Override
        public void onDelete(ScheduleInfo postInfo) {
            postList.remove(postInfo);
            scheduleAdapter.notifyDataSetChanged();
            Log.e("로그: ","삭제 성공");
        }

        @Override
        public void onModify() {

            Log.e("로그: ","수정 성공");
        }
    };

    // 스케쥴 DB랑 연동해서 업데이트 하는 함수
    private void postsUpdate(final boolean clear) {
        updating = true;
        Date date = postList.size() == 0 || clear ? new Date() : postList.get(postList.size() - 1).getCreatedAt();
        CollectionReference collectionReference = firebaseFirestore.collection("schedule");
        collectionReference.orderBy("createdAt", Query.Direction.DESCENDING).whereLessThan("createdAt", date).limit(10).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if(clear){
                                postList.clear();
                            }
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                if(document.getData().get("meetingID").toString().equals(meetingName)){
                                    Log.d("update Test", meetingName);
                                    ScheduleInfo temp = new ScheduleInfo(
                                            document.getData().get("title").toString(),
                                            document.getData().get("meetingID").toString(),
                                            (ArrayList<String>) document.getData().get("contents"),
                                            new Date(document.getDate("createdAt").getTime()),
                                            document.getId());

                                    if(document.get("meetingPlace")!=null){
                                        temp.setMeetingPlace(document.get("meetingPlace").toString());
                                    }

                                    if(document.get("meetingDate")!=null){
                                        temp.setMeetingDate(document.getDate("meetingDate"));
                                    }

                                    postList.add(temp);
                                }
                            }
                            scheduleAdapter.notifyDataSetChanged();
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                        updating = false;
                    }
                });
    }


    private void myStartActivity(Class c) {
        Intent intent = new Intent(getActivity(), c);
        intent.putExtra("Name",meetingName);
        startActivityForResult(intent, 0);
    }
}
