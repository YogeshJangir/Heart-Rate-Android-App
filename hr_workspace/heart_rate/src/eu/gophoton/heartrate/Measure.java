package eu.gophoton.heartrate;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

//import com.androidplot.ui.SizeMetrics;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
//import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
//import com.androidplot.xy.XYStepMode;

import eu.gophoton.heartrate.R;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;


public class Measure extends Fragment {

private static final AtomicBoolean processing = new AtomicBoolean(false);
private static SurfaceView preview = null;
private static SurfaceHolder previewHolder = null;
private static Camera camera = null;
private boolean trigger = false; //This value of this determines start/stop camera
private static TextView hr_text;
private static HeartRateCalculator heart_calc;
private static HRMFileWriter fileWriter;
private static int valuesInArray = 256;
private static long startTime = 0;

private static final int HISTORY_SIZE = 300;
private static XYPlot ppgPlot = null;
private static XYPlot hrHistoryPlot = null;
private static XYPlot fourierPlot = null;
private static SimpleXYSeries ppgSeries = null;
private static SimpleXYSeries hrHistorySeries = null;
private static boolean hasProcessed = false;

// New Debugging Options!!
static boolean useRed = true;	// Set to true to use Red, set to false to use Green 
static boolean dataExcelExport = false;	// Set to true to enable email of data

@SuppressWarnings("deprecation")
@Override
public View onCreateView(LayoutInflater inflater, ViewGroup container,
              Bundle savedInstanceState) {
	
	View ios = inflater.inflate(R.layout.measure_frag, container, false);
  
	((TextView)ios.findViewById(R.id.textView3)).setText(R.string.Measure);
	hr_text=((TextView)ios.findViewById(R.id.hr_text));
	
	heart_calc = new HeartRateCalculator(valuesInArray);
  
	final Button start_stop_button=(Button) ios.findViewById(R.id.start_stop_button);  
	start_stop_button.setText(R.string.btn_start_text); //Initialize text on button
  
	start_stop_button.setOnClickListener(new OnClickListener() {
		@Override
		public void onClick(View v){

			if (trigger==false)
			{
				trigger=true;
				startProcessing();
				start_stop_button.setText(R.string.btn_stop_text); //Rename the text on button
			}
			else
			{
				trigger=false;  
				stopProcessing();
				start_stop_button.setText(R.string.btn_start_text); //Rename the text on button
			}
		} //End onClick(View v)	          					  
	});

	preview = (SurfaceView) ios.findViewById(R.id.cam_preview);
	previewHolder = preview.getHolder();
	previewHolder.addCallback(surfaceCallback);
  
	if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
	{
	  previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}
	preview.setVisibility(View.VISIBLE);
	
	// Setup the Intensity plot:
    ppgPlot = (XYPlot) ios.findViewById(R.id.ppgPlot);
    ppgSeries = new SimpleXYSeries("Red Signal");
    ppgSeries.useImplicitXVals();
    ppgPlot.setRangeBoundaries(0, 255, BoundaryMode.AUTO);
    ppgPlot.setDomainBoundaries(0, 300, BoundaryMode.FIXED);
    ppgPlot.addSeries(ppgSeries, new LineAndPointFormatter(Color.rgb(255, 0, 0), null, null, null));
    ppgPlot.setDomainStepValue(5);
    ppgPlot.setTicksPerRangeLabel(3);
    //ppgPlot.setDomainLabel("Sample Index");
    ppgPlot.getGraphWidget().setDomainLabelPaint(null); //This gets rid of the labels on x
    ppgPlot.getGraphWidget().setRangeLabelPaint(null);
    ppgPlot.getDomainLabelWidget().pack();
    //ppgPlot.setRangeLabel("Intensity");
    ppgPlot.getRangeLabelWidget().pack();
    ppgPlot.getLayoutManager()
    .remove(ppgPlot.getLegendWidget());
    ppgPlot.setTitle(getString(R.string.intensity_text));
    ppgPlot.getGraphWidget().setPaddingLeft((float) 5.0);
    
    
    // Setup the Fourier plot:
    fourierPlot = (XYPlot) ios.findViewById(R.id.fourierXYPlot);
    fourierPlot.setDomainBoundaries(4, 30, BoundaryMode.FIXED);
    // reduce the number of range labels
    fourierPlot.setTicksPerRangeLabel(3);
    fourierPlot.getGraphWidget().setDomainLabelOrientation(-45);
    fourierPlot.getGraphWidget().setDomainLabelPaint(null);//This gets rid of the xlabels
    fourierPlot.getGraphWidget().setRangeLabelPaint(null);
    fourierPlot.setTitle(getString(R.string.fourier_text));
    fourierPlot.getGraphWidget().setPaddingLeft((float) 5.0);
    
    
    // Setup the HR plot:
    hrHistoryPlot = (XYPlot) ios.findViewById(R.id.hrHistoryPlot);
    hrHistorySeries = new SimpleXYSeries("Heart Rate Value");
    hrHistorySeries.useImplicitXVals();
    hrHistoryPlot.addSeries(hrHistorySeries, new LineAndPointFormatter(Color.rgb(0, 0, 255), null, null, null));
    hrHistoryPlot.getGraphWidget().setDomainLabelPaint(null);//This gets rid of the xlabels
    hrHistoryPlot.getGraphWidget().setRangeLabelPaint(null);//This gets rid of the xlabels
    hrHistoryPlot.getLayoutManager()
    .remove(hrHistoryPlot.getLegendWidget());
    hrHistoryPlot.setTitle(getString(R.string.heart_rate_text));
    hrHistoryPlot.setRangeBoundaries(30, 200, BoundaryMode.AUTO);
    hrHistoryPlot.getTitleWidget().setPaddingBottom((float) 0.0);
    hrHistoryPlot.getGraphWidget().setPaddingTop((float) 50.0);
   
	return ios;
}

//Loops through sizes and returns smallest. 
private static Camera.Size getSmallestPreviewSize(Camera.Parameters parameters) {
	
	Camera.Size result = null;
	int cWidth = Integer.MAX_VALUE;
	int cHeight = Integer.MAX_VALUE;

	for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
		if (size.width <= cWidth && size.height <= cHeight) {
			
			if (result == null) {
				result = size;
			}  
			else {
				int resultArea = result.width * result.height;
				int newArea = size.width * size.height;

				if (newArea < resultArea) 
				{
					result = size;
				}
			}
		}
	}

	return result;
}


public void startProcessing()
{
	getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

	if(hasProcessed)
	{
		resetMeasureScreen();
	}
	camera = Camera.open();
    Camera.Parameters parameters = camera.getParameters();
    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
    
	Camera.Size size = getSmallestPreviewSize(parameters);
	if (size != null) {
		parameters.setPreviewSize(size.width, size.height);
		Log.d(getClass().getSimpleName(), "Using width=" + size.width + " height=" + size.height);
	}
    
    camera.setParameters(parameters);
    
    try {
    	camera.setPreviewDisplay(previewHolder);
    }	catch (IOException e) {
    	e.printStackTrace();	
    }
    camera.setPreviewCallback(previewCallback);;
    camera.startPreview(); 
    
	if(dataExcelExport)
	{
		startTime = System.currentTimeMillis();
		
		fileWriter = new HRMFileWriter(getActivity(), valuesInArray);
		if(!checkFileSetup())
		{
			dataExcelExport = false;
		}
	}
	
	hr_text.setText("    --  ");
	hasProcessed = true;
    
}

//Wrap up all the camera functions
public void stopProcessing()
{
	getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	
	if(camera!=null)
	{
		camera.setPreviewCallback(null);
		camera.stopPreview();
     	camera.release();
     	camera = null;
	} //End if
	
	if(dataExcelExport)
	{
		emailData();
	}
}

/**{@inheritDoc} */
@Override
public void onResume() {
    super.onResume();
    
    if(trigger==true)
    {
    	getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }//End if
}

/**{@inheritDoc} */
@Override
public void onPause() {
    super.onPause();
    
    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    
    if(camera!=null)
    {
    	camera.setPreviewCallback(null);
    	camera.stopPreview();
    	camera.release();
    	camera = null;
    }
}

private static PreviewCallback previewCallback = new PreviewCallback() {

    /**{@inheritDoc}*/
    @SuppressLint("DefaultLocale")
	@Override
    public void onPreviewFrame(byte[] data, Camera cam) {
    	
        if (data == null) throw new NullPointerException();
        
        Camera.Size size = cam.getParameters().getPreviewSize();
        
        if (size == null) throw new NullPointerException();

        if (!processing.compareAndSet(false, true)) return;

        int width = size.width;
        int height = size.height; 
        
        new FrameProcessor(data, width, height).execute();
        
        processing.set(false);

    }
};


private static SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

    /**{@inheritDoc} */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
        	Log.e(getClass().getSimpleName(), "Creating surface");
        } catch (Throwable t) {
            Log.e("PreviewDemo-surfaceCallback", "Exception in setPreviewDisplay()", t);
        }
    }

    /**{@inheritDoc}*/
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    	// This method is required
    }

    /**{@inheritDoc} */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    	// This method is required
    }
};

//Check that external storage is available to write to and that we have permission
// to write data to this location. Also check a file and the HeartRateData directory
// has been created. 
private boolean checkFileSetup()
{
	
	if(!fileWriter.createDirectoryAndFile())
	{
		String dtlMsg = "Unable to create file to save data.";
		Log.e(getClass().getSimpleName(), dtlMsg);
		return false;
	}

	return true;
}

private void emailData() {
    
    fileWriter.emptyCacheToFile();

    if(!fileWriter.usingInternalStorage)
    {
    	// The file is saved on External Storage - this case is a little easier to deal with
    	// Use an ACTION_SEND Intent to send the email, the user can select the email application
    	// using a chooser
		Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Heart Rate Data from TOMI Heart Rate Monitor is attached.");
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Heart Rate Data");
		emailIntent.setType("text/csv");

		Uri uri = Uri.parse("file://" + fileWriter.getDataFilePath());
		emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
		startActivity(emailIntent);
    }
    else
    {
    	// The file is saved on Internal Storage - other apps (such as email apps) cannot access
    	// these files as they are private to our app. Therefore we must use the ContentProvider 
    	// to make the file available. Users can only send the email using GMail in this case!
    	final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
 
        //Explicitly only use Gmail to send
        emailIntent.setClassName("com.google.android.gm","com.google.android.gm.ComposeActivityGmail");
        
        emailIntent.setType("text/csv");	        
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Heart Rate Data");	     
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Heart Rate Data from TOMI Heart Rate Monitor is attached.");
     
        //Add the attachment by specifying a reference to our custom ContentProvider
        //and the specific file of interest
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://" + CachedFileProvider.AUTHORITY + "/" + fileWriter.getDataFileName()));
        
        startActivity(emailIntent);
    }
	
}

private static class FrameProcessor extends AsyncTask<Void, Void, Double>
{
	byte[] frame;
	int width;
	int height;

	FrameProcessor(byte[] frm, int w, int h) { 
        this.frame = frm;
        this.width = w;
        this.height = h;
    }
	
	@Override
	protected Double doInBackground(Void... params) {
		
		if(useRed)
		{
			return meanRedOnFrame();
		}
		else
		{
			return meanGreenOnFrame();
		}
		
	}
	
	@Override
    protected void onPostExecute(Double avg) {
		
		if(dataExcelExport)
		{
	        // Calculate time since app has started. This is time-stamp to be written to CSV file.
	    	double timingSeconds = (double)(System.currentTimeMillis() - startTime)/1000;
	    	
	    	// Write data to CSV file.
	    	fileWriter.writeData(timingSeconds, avg);
		}
		
		double bps = heart_calc.updateData(avg);
    	
        if(bps != 0)
        {
        	Log.d(getClass().getSimpleName(), "Updating BPM");
        	double bpm = bps*60;
        	if(bpm > 40 && bpm < 200)
        	{
        		int bpm_int = (int) Math.round(bpm);
        		String heartRateString = String.format(Locale.UK, "    %d BPM", bpm_int); 
        		hr_text.setText(String.valueOf(heartRateString));
        		
        		hrHistorySeries.addLast(null, bpm_int);
        		
        		hrHistoryPlot.redraw();
        		
        		// Draw the result of FFT
        		Number[] fftResult = heart_calc.getFFTIntensities();
        		
        		// Turn the above arrays into XYSeries':
                XYSeries fourierSeries = new SimpleXYSeries(
                        Arrays.asList(fftResult),          // SimpleXYSeries takes a List so turn our array into a List
                        SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // Y_VALS_ONLY means use the element index as the x value
                        "Fourier Analysis"); 
                
                LineAndPointFormatter fourierSeriesFormat = new LineAndPointFormatter(Color.RED, Color.GREEN, Color.BLUE, null);
         
                fourierPlot.clear();
                
                // add a new series' to the xyplot:
                fourierPlot.addSeries(fourierSeries, fourierSeriesFormat);
                
                fourierPlot.removeMarkers();
                fourierPlot.getLayoutManager()
                .remove(fourierPlot.getLegendWidget());
                
                fourierPlot.redraw();
        	}
        }
        
        // get rid the oldest sample in history:
        if (ppgSeries.size() > HISTORY_SIZE) {
            ppgSeries.removeFirst();
        }
        
        // add the latest history sample:
        ppgSeries.addLast(null, avg);
        
        ppgPlot.redraw();
        
    }
	
	private double meanRedOnFrame()
	{
		if (frame == null) return 0.0;

	    final int frameSize = width * height;

	    int sum = 0;
	    for (int j = 0, yp = 0; j < height; j++) {
	        int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
	        for (int i = 0; i < width; i++, yp++) {
	            int y = (0xff & frame[yp]) - 16;
	            if (y < 0) y = 0;
	            if ((i & 1) == 0) {
	                v = (0xff & frame[uvp++]) - 128;
	                u = (0xff & frame[uvp++]) - 128;
	            }
	            int y1192 = 1192 * y;
	            int r = (y1192 + 1634 * v);
	            int g = (y1192 - 833 * v - 400 * u);
	            int b = (y1192 + 2066 * u);

	            if (r < 0) r = 0;
	            else if (r > 262143) r = 262143;
	            if (g < 0) g = 0;
	            else if (g > 262143) g = 262143;
	            if (b < 0) b = 0;
	            else if (b > 262143) b = 262143;

	            int pixel = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
	            int red = (pixel >> 16) & 0xff;
	            sum += red;
	        }
	    }
	    double av_red=sum/(width*height);
	    return av_red;
	}
	
	private double meanGreenOnFrame()
	{
		int frameSize = width * height;
		
        int sum = 0;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & frame[yp]) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & frame[uvp++]) - 128;
                    u = (0xff & frame[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;

                int pixel = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
                int green = (pixel >> 8) & 0xff;
                sum += green;
            }
        }
        double av_green=sum/(width*height);
	    return av_green;
	}
	
}

private void resetMeasureScreen()
{
	hr_text.setText("    --  ");
	
	heart_calc.clearData();
	
	ppgPlot.clear();
	ppgSeries = new SimpleXYSeries("Red Signal");
    ppgSeries.useImplicitXVals();
    ppgPlot.setRangeBoundaries(0, 255, BoundaryMode.AUTO);
    ppgPlot.setDomainBoundaries(0, 300, BoundaryMode.FIXED);
    ppgPlot.addSeries(ppgSeries, new LineAndPointFormatter(Color.rgb(255, 0, 0), null, null, null));
    ppgPlot.setDomainStepValue(5);
    ppgPlot.setTicksPerRangeLabel(3);
    ppgPlot.getGraphWidget().setDomainLabelPaint(null); //This gets rid of the labels on x
    ppgPlot.getGraphWidget().setRangeLabelPaint(null);
    ppgPlot.getDomainLabelWidget().pack();
    ppgPlot.getRangeLabelWidget().pack();
    ppgPlot.getLayoutManager().remove(ppgPlot.getLegendWidget());
    ppgPlot.setTitle("Intensity");
    ppgPlot.getGraphWidget().setPaddingLeft((float) 5.0);
    ppgPlot.redraw();
	
	fourierPlot.clear();
	fourierPlot.redraw();
	
	hrHistoryPlot.clear();
	hrHistorySeries = new SimpleXYSeries("Heart Rate Value");
    hrHistorySeries.useImplicitXVals();
    hrHistoryPlot.addSeries(hrHistorySeries, new LineAndPointFormatter(Color.rgb(0, 0, 255), null, null, null));
    hrHistoryPlot.getGraphWidget().setDomainLabelPaint(null);//This gets rid of the xlabels
    hrHistoryPlot.getGraphWidget().setRangeLabelPaint(null);//This gets rid of the xlabels
    hrHistoryPlot.getLayoutManager()
    .remove(hrHistoryPlot.getLegendWidget());
    hrHistoryPlot.setTitle("Heart Rate");
    hrHistoryPlot.setRangeBoundaries(30, 200, BoundaryMode.AUTO);
    hrHistoryPlot.getTitleWidget().setPaddingBottom((float) 0.0);
    hrHistoryPlot.getGraphWidget().setPaddingTop((float) 50.0);
    hrHistoryPlot.redraw();
}

}