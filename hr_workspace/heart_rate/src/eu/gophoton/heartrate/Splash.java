package eu.gophoton.heartrate;

import eu.gophoton.heartrate.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Splash extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
		
		//The next few lines are from Bucky's Tutorial #14
		Thread timer = new Thread(){
			public void run(){
				try{
					sleep(2500); //Sleep time in ms  NORMALLY 2500
				} catch (InterruptedException e){
					e.printStackTrace();
				} finally{
					//This runs once the splash screen is finished
					Intent openMainActivity = new Intent("eu.gophoton.heartrate.MAINHRACTIVITY");
					startActivity(openMainActivity);
				} //End finally
				finish();  //Added on 12/9/14 - Required when the Exit button is selected.
			} //End of public void run...
		};//End of new thread
		timer.start();
	}

}
