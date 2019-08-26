package hr.ferit.dariomihelcic.doggo.activities;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import hr.ferit.dariomihelcic.doggo.R;
import hr.ferit.dariomihelcic.doggo.fragments.CameraFragment;

public class MainActivity extends AppCompatActivity {

    public static String PACKAGE_NAME;
    public Context context;

    private TabLayout tlLayout;
    private ViewPager vpPager;
    private PagerAdapter paAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PACKAGE_NAME = getApplicationContext().getPackageName();


        context = this;
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().
                    replace(R.id.container, CameraFragment.newInstance())
                    .commit();
        }
    }

    //TODO: Add vievs to stack.
    /***@Override
    public void onBackPressed() {
        if(vpPager.getCurrentItem()==1 || vpPager.getCurrentItem()==2) {
            vpPager.setCurrentItem(0);
        }
        else
            super.onBackPressed();
    }***/
}
