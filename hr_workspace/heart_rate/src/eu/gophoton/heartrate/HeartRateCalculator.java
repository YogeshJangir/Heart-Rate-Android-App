package eu.gophoton.heartrate;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import org.jtransforms.fft.DoubleFFT_1D;

import android.util.Log;

public class HeartRateCalculator {

	int valuesInArray = 0;
	long timingStart = 0;
	double[] intensityArray;
	Number[] intensityArrayNumber;
	int indexQ = 0;
	DoubleFFT_1D fftDo;
	
	int heartRateLower = 30;
	int heartRateUpper = 200;
	
	Queue<Double> intensityQueue = new LinkedList<Double>();
	
	// Constructor
	public HeartRateCalculator(int values)
	{
		valuesInArray = values;
		
		//Initialization of arrays
		intensityArray = new double[valuesInArray*2];
		intensityArrayNumber = new Number[valuesInArray];
		for(int i = 0; i < intensityArray.length; ++i)
		{
			intensityArray[i] = 0;
		}
		fftDo = new DoubleFFT_1D(valuesInArray);
	}
	
	public Number[] getFFTIntensities()
	{
		return intensityArrayNumber;
	}
	
	public double updateData(Double data)
	{
		double bps = 0;
		
		if(intensityQueue.size() < valuesInArray)
		{
			// We are still early in the running. The queue has not filled to
			// valuesInArray number of values yet.
			// Just add this value, check if this addition brings it up to 
			// valuesInArray number of values, and if so, do the FFT
			
			bps = addDataQueueNotFull(data);
		}
		else
		{
			// Queue is full. Remove oldest data point, add this newest data
			// point. If 128 frames have elapsed since last FFT, then do the processing.
			
			bps = addDataQueueFull(data);
		}
		
		return bps;
	}
	
	private double addDataQueueFull(Double data)
	{
		double bps = 0;
		
		intensityQueue.remove();
		intensityQueue.add(data);
		
		indexQ++;
		
		if(indexQ == (valuesInArray/2))
		{
			String display = String.format(Locale.UK, "128 frames have passed since last FFT. Will run again.");
	        Log.e(getClass().getSimpleName(), display);
	        
			bps = doProcessing(true);
			indexQ = 0;
			
		}
		
		return bps;
	}
	
	private double addDataQueueNotFull(Double data)
	{
		if(intensityQueue.size() == 0)
		{
			timingStart = System.currentTimeMillis();
		}
		
		intensityQueue.add(data);
		
		double bps = 0;
		
		if(intensityQueue.size() == valuesInArray)
		{
			String display = String.format(Locale.UK, "Queue just filled. Will FFT for 1st time.");
	        Log.e(getClass().getSimpleName(), display);
	        
			// Do FFT
			bps = doProcessing(false);
		}
		else
		{
			// Queue not yet full, just do nothing
		}
		
		return bps;
	}
	
	private double doProcessing(boolean queueAlreadyFull)
	{
		long timingEnd = System.currentTimeMillis();
		double totalTimeInSecs = (timingEnd - timingStart) / 1000d;  //Calculate time to acquire 256 data points
		double sampleRate = 0;
		
		if(queueAlreadyFull)
		{
			sampleRate = (valuesInArray/2)/totalTimeInSecs;
		}
		else
		{
			sampleRate = valuesInArray/totalTimeInSecs;
		}				
		
		String display = String.format(Locale.UK, "Sample rate is %f fps", sampleRate);
        Log.e(getClass().getSimpleName(), display);
        

    	//access via Iterator
        int j = 0;
    	Iterator<Double> iterator = intensityQueue.iterator();
    	while(iterator.hasNext()){
    	  intensityArray[j] = (double) iterator.next();
    	  j++;
    	  if(j == valuesInArray)
    	  {
    		  break;
    	  }
    	}
		
		// Do FFT
		fftDo.realForwardFull(intensityArray);
		
		double bps = ObtainLargestFrequency(sampleRate);
		
		// Empty array
		for(int i = 0; i < intensityArray.length; ++i)
		{
			intensityArray[i] = 0;
		}

		timingStart = System.currentTimeMillis();
		
		return bps;
	}

	private double ObtainLargestFrequency(double Fs)
	{
		int size = intensityArray.length / 2;
		int index = 0;
		
		double re = 0;
		double im = 0;
		double magnitude = 0;
		
		double largestmagnitudeSoFar = 0;
		double frequency = 0;
		
		float freqResolution = (float) (valuesInArray/Fs);
		
		float heartRateLowerHz = ((float)heartRateLower/60);
		float heartRateUpperHz = ((float)heartRateUpper/60);
		
		float lowerBoundFloat = (heartRateLowerHz*freqResolution);		
		int lowerBoundIndex = Math.round(lowerBoundFloat);
		
		float upperBoundFloat = (heartRateUpperHz*freqResolution);
		int upperBoundIndex = Math.round(upperBoundFloat);
		
		String display = String.format(Locale.UK, "LBI = %d - UBI = %d fr = %f LBFHz = %f UBFHz = %f", lowerBoundIndex, upperBoundIndex, freqResolution, heartRateLowerHz, heartRateUpperHz);
        Log.e(getClass().getSimpleName(), display);
		
		for(int i = 0; i < size; ++i)
		{
			index = 2*i;
			re = intensityArray[index];
			im = intensityArray[index + 1];
			magnitude = Math.sqrt(re*re + im*im);
			
			intensityArrayNumber[i] = magnitude;
			
			if((magnitude > largestmagnitudeSoFar) && (i > lowerBoundIndex) && (i < upperBoundIndex))
			{
				largestmagnitudeSoFar = magnitude;
				frequency = Fs * i / size;
			}
		}
		
		return frequency;
	}
	
	
	public void clearData()
	{
		intensityQueue.clear();
		
		for(int i = 0; i < intensityArray.length; ++i)
		{
			intensityArray[i] = 0;
		}
		
		for(int i = 0; i < intensityArrayNumber.length; ++i)
		{
			intensityArrayNumber[i] = 0;
		}
	}
	
}
