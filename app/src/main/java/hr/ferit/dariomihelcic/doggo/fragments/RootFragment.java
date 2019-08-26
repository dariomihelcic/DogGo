package hr.ferit.dariomihelcic.doggo.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import hr.ferit.dariomihelcic.doggo.R;

public class RootFragment extends Fragment {

    public static RootFragment newInstance(){
        return new RootFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_root, container, false);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.rlFrame, new DogDescriptionFragment());
        transaction.commit();

        return view;
    }
}
