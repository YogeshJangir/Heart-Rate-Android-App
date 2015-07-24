package eu.gophoton.heartrate;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
public class TabPagerAdapter extends FragmentStatePagerAdapter {
    public TabPagerAdapter(FragmentManager fm) {
    super(fm);
    // TODO Auto-generated constructor stub
  }
 @Override
  public Fragment getItem(int i) {
    switch (i) {
        case 0:
            //Fragment for Instructions Tab
            return new Instructions();
        case 1:
           //Fragment for Learn Tab
            return new Learn();
        case 2:
            //Fragment for Measure Tab
            return new Measure();
        case 3:
            //Fragment for Partners Tab
            return new Partners();
        }
    return null;
  }
  @Override
  public int getCount() {
    // TODO Auto-generated method stub
    return 4; //No of Tabs
  }
    }