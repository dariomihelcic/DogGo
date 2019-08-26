package hr.ferit.dariomihelcic.doggo.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import hr.ferit.dariomihelcic.doggo.R;
import hr.ferit.dariomihelcic.doggo.activities.MainActivity;

public class DogDescriptionFragment extends Fragment {

    private ImageView imDog;
    private TextView tvDesc;

    public static DogDescriptionFragment newInstance(){
        return new DogDescriptionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_dog_description, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpData(view);
        Bundle bundle = this.getArguments();
        if(bundle != null) {
            tvDesc.setText(bundle.getString("description"));
            int resourceId = view.getResources().getIdentifier(bundle.getString("picture"),
                    "drawable", MainActivity.PACKAGE_NAME);
            Glide.with(view).load(resourceId).into(imDog);
        }
    }

    private void setUpData(View view) {
        imDog = view.findViewById(R.id.ivDog);
        tvDesc = view.findViewById(R.id.tvDesc);

        tvDesc.setText("Choos a dog breed to see a description");
    }


}
