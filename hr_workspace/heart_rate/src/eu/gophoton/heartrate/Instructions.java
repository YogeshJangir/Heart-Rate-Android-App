package eu.gophoton.heartrate;
import eu.gophoton.heartrate.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
public class Instructions extends Fragment {
  @Override
      public View onCreateView(LayoutInflater inflater, ViewGroup container,
              Bundle savedInstanceState) {
          View android = inflater.inflate(R.layout.instructions_frag, container, false);
          ((TextView)android.findViewById(R.id.textView1)).setText(R.string.Instructions);
          return android;
}}