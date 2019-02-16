package de.mide.memoryleakdemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


/**
 * App erzeugt Speicherlöcher im RAM wenn in der Methode {@link MainActivity#onDestroy()}
 * der Thread nicht beendet wird (d.h. Anweisung <i>_blinkerThread.beenden()</i> ist auskommentiert)
 * und das Gerät mehrfach gedreht wird (Wechsel zwischen Hoch- und Querformat).
 * <br><br>
 * 
 * This file is licensed under the terms of the BSD 3-Clause License.
 */
public class MainActivity extends Activity {

	public static final String TAG4LOGGING = "Speicherleichen";
	
	
	/** Statische Variable zum Mitzählen der erzeugten Instanzen. */
	protected static int INSTANZEN_ZAEHLER = 0;
	
	/** Member-Variable zum Speichern der laufenden Nummer der Instanz. */
	protected int _instanzNummer = 0;
	
	/** Hintergrund-Farbe dieses TextViews wird durch Thread regelmäßig zwischen zwei
	 *  Farben gewechselt, so dass "Blink-Effekt" entsteht. */ 	 
	protected TextView _blinkendesTextview = null;
	
	/** Thread-Instanz, die für den "Blink-Effekt" verantwortlich ist. */
	protected BlinkerThread _blinkerThread = null;
	
	
	/**
	 * Default-Konstruktor zum Mitzählen der erzeugten Instanzen dieser Activity-Klasse.
	 * Außerdem wird mit {@link System#gc()} der Garbage Collector aufgerufen, was aber
	 * nicht garantiert, dass dieser wirklich (sofort) ausgeführt wird.
	 */
	public MainActivity() {

		super();
		INSTANZEN_ZAEHLER++;
		_instanzNummer = INSTANZEN_ZAEHLER;
		
		System.gc();  		
	}
	
	
	/**
	 * Lifecycle-Methode: Layout-Datei laden und aktuelle Instanz-Nummer
	 * im Titel der Activity anzeigen; außerdem wird ein Thread gestartet.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setTitle("Instanz-Nummer " + _instanzNummer);
									
		_blinkendesTextview = findViewById(R.id.textview_blinkend);
		
		
		_blinkerThread = new BlinkerThread(_instanzNummer, _blinkendesTextview);
		_blinkerThread.start();		
	}

	
	/* *************************** */
	/* *** Start innere Klasse *** */
	/* *************************** */	
	protected class BlinkerThread extends Thread {
			
		protected int      __instanzNummer = 0;
		
		protected TextView __textViewBlinkend = null;
		
		protected boolean  __beenden = false;  
		
		
		/** Konstruktor der inneren Klasse. */
		public BlinkerThread(int instanzNummer, TextView textViewBlinkend) {

			__instanzNummer    = instanzNummer;
			__textViewBlinkend = textViewBlinkend;
		}
		
		/** 
		 * Inhalt dieser Methode wird in Hintergrund-Thread ausgeführt.
		 * Die Hintergrund-Farbe des TextView-Elements wird abwechselnd geändert.
		 * Dieser Zugriff auf ein UI-Element darf nur aus dem Main-Thread (und nicht
		 * einem Hintergrund-Thread) heraus geschehen, weshalb das Setzen der
		 * Hintergrund-Farbe mit der Methode {@link android.view.View#post(Runnable)}
		 * an den Main-Thread geschickt wird; hierzu muss das Setzen der Farbe
		 * mit einem {@link java.lang.Runnable}-Objekt gekapselt werden.   
		 * */
		@Override
		public void run() {
			
			int zaehler = 0;
																									
			while(true) {
						
				if (__beenden == true) {
					Log.i(TAG4LOGGING, "Endlos-Schleife für Instanz " + __instanzNummer + " wird verlassen.");
					break;
				}
				
				zaehler++;
						
				// Farbe in Abhängigkeit davon, ob Wert in "zaehler" gerade oder ungerade ist, setzen.
				if (zaehler % 2 == 0) {
							
					__textViewBlinkend.post(new Runnable() {								
						@Override
						public void run() {
							__textViewBlinkend.setBackgroundColor( 0xFFFFFF00 ); // Gelb mit voller Deck-Kraft
						}
					});
																	
				} else {

					__textViewBlinkend.post(new Runnable() {								
						@Override
						public void run() {
							__textViewBlinkend.setBackgroundColor( 0xFFD0D0D0 ); // Grauton 							
						}
					});
				}
					
						
				try {							
					Thread.sleep(1000); // Eine Sekunde warten, ohne dabei CPU-Zeit zu verschwenden
				} 						
				catch (InterruptedException ex) {
					Log.e(TAG4LOGGING, "Exception während Warten aufgetreten: " + ex);
				}				
						
						
				Log.i(TAG4LOGGING, "Instanz " + __instanzNummer + " lebt noch.");
												
			} // Schleife							

			// Wenn wir bis zu dieser Stelle kommen (da Endlos-Schleife geht dies nur über "break"),
			// dann wird der Thread beendet (Ende run()-Methode => Ende Thread).
		}
		
		
		/**
		 * Setzt ein Thread-internes Flag, damit der Thread Bescheid weiß, dass er nach
		 * der nächsten Iteration sich beenden soll.
		 */
		public void beenden() {

			__beenden = true;
		}

		@Override
		protected void finalize() throws Throwable {

			super.finalize();
			Log.i(TAG4LOGGING,
                    "Der GC hat die Instanz " + _instanzNummer + " von BlinkerThread (innere Klasse) gelöscht.");
		}
				
	};
	/* *************************** */
	/* *** Ende innere Klasse  *** */
	/* *************************** */	
	
			
	/**
	 * Destruktor, wird vom GC aufgerufen; diese Methode ist in dieser App überschrieben,
	 * um Nachricht mit Instanz-Nummer auf Logger zu schreiben.
	 */
	@Override
	protected void finalize() throws Throwable {

		super.finalize();
		Log.i(TAG4LOGGING, "Der GC hat die Instanz " + _instanzNummer + " von MainActivity gelöscht.");
	}


	/**
	 * Dies ist die letzte Methode, die im Lifecycle einer Activity-Instanz aufgerufen werden kann.
	 * Wir überschreiben diese Methode, um den BlinkerThread zu beenden.
	 */
	@Override
	protected void onDestroy() {
        
		super.onDestroy();
		
		// Ohne die folgende Zeile entsteht eine Speicherleich, weil die Activity-Instanz wegen
		// dem noch laufenden Thread nicht vom GC beseitigt werden kann.
		_blinkerThread.beenden();
		
		Log.i(TAG4LOGGING, "Methode onDestroy() für Instanz " + _instanzNummer + " aufgerufen.");
	}
	
};
