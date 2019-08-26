package hr.ferit.dariomihelcic.doggo.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import hr.ferit.dariomihelcic.doggo.R;
import hr.ferit.dariomihelcic.doggo.adapters.RecyclerAdapter;
import hr.ferit.dariomihelcic.doggo.data.DogBreed;
import hr.ferit.dariomihelcic.doggo.data.DogListHolder;
import hr.ferit.dariomihelcic.doggo.interfaces.NameClickListener;

public class DogListFragment extends Fragment implements SearchView.OnQueryTextListener, NameClickListener {

    private RecyclerView recycler;
    private RecyclerAdapter adapter;

    public static DogListFragment newInstance(){
        return new DogListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_dog_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpRecycler(view);
        setUpRecyclerData();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();

        inflater.inflate(R.menu.main_menu, menu);
        MenuItem item = menu.findItem(R.id.dog_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW|MenuItem.SHOW_AS_ACTION_IF_ROOM);

        SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        adapter.getFilter().filter(s);
        return false;
    }

    private void setUpRecycler(View view) {
        recycler = view.findViewById(R.id.rvCell);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecyclerAdapter(this);
        recycler.setAdapter(adapter);

        recycler.addItemDecoration(new DividerItemDecoration(recycler.getContext(), DividerItemDecoration.VERTICAL));
        recycler.setHasFixedSize(true);
        recycler.setItemViewCacheSize(20);
        recycler.setDrawingCacheEnabled(true);
    }

    //changed
    private void setUpRecyclerData() {
        DogListHolder holder = new DogListHolder();
        List<DogBreed> list;
        try {
            holder.loadData(this.getContext());
        } catch (IOException e) {
            Toast.makeText(getContext(), "NOPE", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        list = holder.getFullList();
        adapter.addData(list);
    }

    @Override
    public void onNameClick(View v, int position) {
        DogBreed dog = adapter.getDogBreed(position);
        Bundle bundle = new Bundle();

        bundle.putString("description", dog.getDescription());
        bundle.putString("picture", dog.getPicture());

        DogDescriptionFragment newFragment = DogDescriptionFragment.newInstance();
        newFragment.setArguments(bundle);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.container, newFragment).commit();
    }


}
