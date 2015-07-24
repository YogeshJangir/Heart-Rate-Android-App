package eu.gophoton.heartrate;
import eu.gophoton.heartrate.R;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
public class Partners extends Fragment {
	
	ImageButton link_button;  //Declare ImageButton
	
  @Override
      public View onCreateView(LayoutInflater inflater, ViewGroup container,
              Bundle savedInstanceState) {
          View android = inflater.inflate(R.layout.partners_frag, container, false);
          ((TextView)android.findViewById(R.id.textView4)).setText(R.string.Partners);
          link_button = (ImageButton) android.findViewById(R.id.button1);
          addListenerOnButton();
          return android;
}
  
	//This is the function for creating a listener on the link button
	public void addListenerOnButton() {

		//When the button is clicked, it goes into the following function
		link_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.gophoton.eu"));
				startActivity(browserIntent);
			}
		}); //End of setOnClickListener  
		
		
	} //End of addListener

}