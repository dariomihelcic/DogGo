package hr.ferit.dariomihelcic.doggo.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.List;

import hr.ferit.dariomihelcic.doggo.R;
import hr.ferit.dariomihelcic.doggo.data.DogBreed;
import hr.ferit.dariomihelcic.doggo.interfaces.NameClickListener;

public class RecyclerAdapter extends RecyclerView.Adapter<DogViewHolder> implements Filterable {

    private final NameClickListener clickListener;
    private List<DogBreed> filterDataList = new ArrayList<>();
    private final List<DogBreed> fullDataList = new ArrayList<>();

    public RecyclerAdapter(NameClickListener listener){this.clickListener = listener;}

    @NonNull
    @Override
    public DogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, final int i) {
        View cellView = LayoutInflater.from(parent.getContext()).inflate(R.layout.dog_cell, parent, false);
        return new DogViewHolder(cellView, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull DogViewHolder dogViewHolder, int i) {
        dogViewHolder.setDogBreed(filterDataList.get(i));
           }

    @Override
    public int getItemCount() {
        return filterDataList.size();
    }

    public void addData(List<DogBreed> data){
        this.filterDataList.clear();
        this.filterDataList.addAll(data);
        this.fullDataList.addAll(data);
        notifyDataSetChanged();
    }

    public void addNewCell(DogBreed breed, int position){
        if(filterDataList.size() >= position && position >= 0){
            filterDataList.add(position, breed);
            notifyItemInserted(position);
        }
    }

    public void removeCell(int position){
        if(filterDataList.size() > position && position >= 0){
            filterDataList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public DogBreed getDogBreed(int position){
        return filterDataList.get(position);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                List<DogBreed> dataListFiltered = null;
                if (charString.isEmpty()) {
                    dataListFiltered = fullDataList;
                }
                else {
                    List<DogBreed> filteredList = new ArrayList<>();
                    for (DogBreed row : fullDataList) {
                        // name match condition
                        if (row.getName().toLowerCase().contains(charString.toLowerCase()) || row.getType().contains(charSequence)) {
                            filteredList.add(row);
                        }
                    }
                    dataListFiltered = filteredList;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = dataListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filterDataList = (ArrayList<DogBreed>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }



}
