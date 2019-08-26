package hr.ferit.dariomihelcic.doggo.data;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DogListHolder {
    private final List<DogBreed> fullDataList = new ArrayList<>();
    private final List<Integer> id_list = new ArrayList<Integer>();

    public void loadData(Context context) throws IOException {
        this.fullDataList.clear();
        this.fullDataList.addAll(readListFile(context));
        this.id_list.clear();
        this.id_list.addAll(readMapFile(context));
    }

    private List<DogBreed> readListFile(Context context)
    {
        String json = null;
        try {
            InputStream is = context.getAssets().open("DogBreedsList.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Type collectionType = new TypeToken<List<DogBreed>>(){}.getType();
        return  new Gson().fromJson( json , collectionType);
    }

    private List<Integer> readMapFile(Context context) throws IOException {
        List<Integer> mapped_id = new ArrayList<Integer>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(context.getAssets().open("dog_id_map_2.1.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            mapped_id.add(Integer.parseInt(line));
        }
        reader.close();
        return mapped_id;
    }



    public List<DogBreed> getFullList(){
        return this.fullDataList;
    }

    public DogBreed getDogBreed(int position){
        return fullDataList.get(position);
    }

    public int getMappedId(int position){

        return id_list.get(position);

    }

}
