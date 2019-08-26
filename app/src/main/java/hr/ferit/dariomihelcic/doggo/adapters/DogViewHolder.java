package hr.ferit.dariomihelcic.doggo.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import hr.ferit.dariomihelcic.doggo.R;
import hr.ferit.dariomihelcic.doggo.activities.MainActivity;
import hr.ferit.dariomihelcic.doggo.data.DogBreed;
import hr.ferit.dariomihelcic.doggo.interfaces.NameClickListener;

public class DogViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private TextView tvDName;
    private ImageView imDImage;
    private TextView tvDTemperament;

    private NameClickListener clickListener;

    public DogViewHolder(@NonNull View itemView, NameClickListener listener) {
        super(itemView);
        this.clickListener = listener;
        tvDName = itemView.findViewById(R.id.tvDName);
        imDImage = itemView.findViewById(R.id.imDImage);
        tvDTemperament = itemView.findViewById(R.id.tvDTemperament);
        itemView.setOnClickListener(this);
    }


    public void setDogBreed(DogBreed breed){

        tvDName.setText(breed.getName());
        tvDTemperament.setText(breed.getType());
        String dogImage = breed.getPicture();

        int resourceId = itemView.getResources().getIdentifier(dogImage,
                "drawable", MainActivity.PACKAGE_NAME);
        //imDImage.setImageResource(resourceId);
        MultiTransformation multi = new MultiTransformation<>(
                new CenterCrop(),
                new RoundedCorners(35));
        Glide.with(itemView)
                .load(resourceId)
                .apply(RequestOptions.bitmapTransform(multi))
                .into(imDImage);
    }


    @Override
    public void onClick(View v) {
        clickListener.onNameClick(v, getAdapterPosition());
    }
}